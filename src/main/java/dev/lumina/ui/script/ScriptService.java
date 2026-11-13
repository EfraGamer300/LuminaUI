package dev.lumina.ui.script;

import dev.lumina.ui.LuminaPlugin;
import dev.lumina.ui.api.UiEvent;
import dev.lumina.ui.api.UiEventType;
import dev.lumina.ui.compat.SchedulerBridge;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Stream;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Loads script files, keeps one interpreter per file and dispatches UI events to matching handlers.
 *
 * <p>Loading is tolerant: a file that fails to parse is reported and skipped, so one broken script
 * cannot stop the others from running. Reloading cancels timers and unregisters script commands
 * before re-reading the directory.
 */
public final class ScriptService implements AutoCloseable {

    private final LuminaPlugin plugin;
    private final ScriptFormats formats = new ScriptFormats();
    private final Map<String, LoadedScript> scripts = new LinkedHashMap<>();
    private final List<SchedulerBridge.ScheduledTaskHandle> timers = new ArrayList<>();
    private final List<ScriptCommand> registeredCommands = new ArrayList<>();
    private AutoCloseable subscription;

    public ScriptService(LuminaPlugin plugin) {
        this.plugin = plugin;
    }

    /** Starts listening to UI events. Called once when the plugin enables. */
    public void start() {
        if (subscription == null) {
            subscription = plugin.registry().onAny(this::dispatch);
        }
    }

    /** Reads every supported script file under {@code contents/scripts}. */
    public LoadReport reload() {
        clear();
        List<String> loaded = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        Path directory = plugin.scriptsDirectory();

        if (!Files.isDirectory(directory)) {
            return new LoadReport(loaded, errors, 0);
        }
        try (Stream<Path> files = Files.list(directory)) {
            files.filter(Files::isRegularFile).sorted().forEach(file -> {
                ScriptFormat format = formats.forFile(file);
                if (format == null) {
                    return;
                }
                try {
                    String source = Files.readString(file, StandardCharsets.UTF_8);
                    ScriptProgram program = format.parse(source, file.getFileName().toString());
                    LoadedScript script = new LoadedScript(file, format, program,
                            new Interpreter(program, null));
                    scripts.put(file.getFileName().toString(), script);
                    loaded.add(file.getFileName().toString());
                } catch (ScriptException failure) {
                    errors.add(failure.getMessage());
                } catch (IOException failure) {
                    errors.add(file.getFileName() + ": " + failure.getMessage());
                }
            });
        } catch (IOException failure) {
            errors.add("could not list scripts directory: " + failure.getMessage());
        }

        for (LoadedScript script : scripts.values()) {
            script.interpreter = new Interpreter(script.program, null);
            script.interpreter.bootstrap();
            scheduleTimers(script);
            registerCommands(script);
        }

        int handlers = 0;
        for (LoadedScript script : scripts.values()) {
            handlers += script.program.handlers().size();
        }
        return new LoadReport(loaded, errors, handlers);
    }

    /** Routes a UI event to every matching handler. */
    public void dispatch(UiEvent event) {
        if (scripts.isEmpty()) {
            return;
        }
        String eventName = eventNameOf(event.type());
        Player player = event.playerId() == null ? null : Bukkit.getPlayer(event.playerId());
        for (LoadedScript script : scripts.values()) {
            for (ScriptHandler handler : script.program.handlers()) {
                if (handler.event().equals("every") || handler.event().equals("command")) {
                    continue;
                }
                if (!matchesEvent(handler.event(), eventName)) {
                    continue;
                }
                if (!handler.matches(event.pageId(), event.nodeId())) {
                    continue;
                }
                run(script, handler, player, event.pageId(), event.nodeId());
            }
        }
    }

    /** Runs a handler by index, used by commands and tests. */
    public boolean runHandler(String file, int index, Player player) {
        LoadedScript script = scripts.get(file);
        if (script == null || index < 0 || index >= script.program.handlers().size()) {
            return false;
        }
        run(script, script.program.handlers().get(index), player, null, null);
        return true;
    }

    public List<String> scriptNames() {
        return List.copyOf(scripts.keySet());
    }

    public ScriptProgram program(String file) {
        LoadedScript script = scripts.get(file);
        return script == null ? null : script.program;
    }

    /** Format that parsed a given file, or {@code null} when it is not loaded. */
    public String formatNameOf(String file) {
        LoadedScript script = scripts.get(file);
        return script == null ? null : script.format.name();
    }

    public ScriptFormats formats() {
        return formats;
    }

    public int handlerCount() {
        int total = 0;
        for (LoadedScript script : scripts.values()) {
            total += script.program.handlers().size();
        }
        return total;
    }

    private void run(LoadedScript script, ScriptHandler handler, Player player,
            String pageId, String elementId) {
        BukkitScriptHost host = new BukkitScriptHost(plugin, player, pageId, elementId);
        Interpreter interpreter = new Interpreter(script.program, host);
        interpreter.bootstrap();
        try {
            interpreter.run(handler);
        } catch (ScriptException failure) {
            plugin.getLogger().warning("script " + script.file.getFileName() + ": " + failure.getMessage());
        } catch (RuntimeException failure) {
            plugin.getLogger().log(Level.WARNING,
                    "script " + script.file.getFileName() + " failed while handling " + handler.event(),
                    failure);
        }
    }

    private void scheduleTimers(LoadedScript script) {
        String fileName = script.file.getFileName().toString();
        for (ScriptHandler handler : script.program.handlers()) {
            if (!handler.event().equals("every")) {
                continue;
            }
            long period = Math.max(1L, handler.everyTicks());
            timers.add(plugin.scheduler().repeatGlobal(period, period, () -> {
                BukkitScriptHost host = new BukkitScriptHost(plugin, null, handler.pageId(), null);
                Interpreter interpreter = new Interpreter(script.program, host);
                interpreter.bootstrap();
                try {
                    interpreter.run(handler);
                } catch (RuntimeException failure) {
                    plugin.getLogger().warning("timer in " + fileName + ": " + failure.getMessage());
                }
            }));
        }
    }

    private void registerCommands(LoadedScript script) {
        String fileName = script.file.getFileName().toString();
        org.bukkit.command.CommandMap commandMap = plugin.getServer().getCommandMap();
        for (ScriptHandler handler : script.program.handlers()) {
            if (!handler.event().equals("command") || handler.commandName() == null) {
                continue;
            }
            ScriptCommand command = new ScriptCommand(
                    handler.commandName(),
                    handler.permission(),
                    "Script command from " + fileName,
                    (sender, args) -> {
                        Player player = sender instanceof Player p ? p : null;
                        BukkitScriptHost host = new BukkitScriptHost(plugin, player, handler.pageId(), null);
                        Interpreter interpreter = new Interpreter(script.program, host);
                        interpreter.bootstrap();
                        interpreter.run(handler);
                    });
            commandMap.register("luminaui", command);
            registeredCommands.add(command);
        }
    }

    private void clear() {
        for (SchedulerBridge.ScheduledTaskHandle timer : timers) {
            timer.cancel();
        }
        timers.clear();
        org.bukkit.command.CommandMap commandMap = plugin.getServer().getCommandMap();
        for (ScriptCommand command : registeredCommands) {
            command.unregister(commandMap);
        }
        registeredCommands.clear();
        scripts.clear();
    }

    @Override
    public void close() {
        clear();
        if (subscription != null) {
            try {
                subscription.close();
            } catch (Exception ignored) {
                // closing a registry subscription cannot fail in practice
            }
            subscription = null;
        }
    }

    private static boolean matchesEvent(String handlerEvent, String actual) {
        if (handlerEvent.equals(actual)) {
            return true;
        }
        return switch (handlerEvent) {
            case "click" -> actual.equals("left click") || actual.equals("right click");
            case "scroll up" -> actual.equals("scroll");
            case "scroll down" -> actual.equals("scroll");
            case "opening" -> actual.equals("open");
            default -> false;
        };
    }

    private static String eventNameOf(UiEventType type) {
        return switch (type) {
            case LEFT_CLICK -> "left click";
            case RIGHT_CLICK -> "right click";
            case SHIFT_CLICK -> "shift click";
            case HOVER_ENTER -> "hover";
            case HOVER_EXIT -> "unhover";
            case DRAG_START -> "drag start";
            case DRAG_MOVE -> "drag";
            case DRAG_END -> "drop";
            case SCROLL -> "scroll";
            case INPUT_TEXT -> "input";
            case PAGE_OPEN -> "open";
            case PAGE_CLOSE -> "close";
        };
    }

    /** Result of a reload, shown by {@code /lumina script reload}. */
    public record LoadReport(List<String> loaded, List<String> errors, int handlers) {
        public LoadReport {
            loaded = List.copyOf(loaded);
            errors = List.copyOf(errors);
        }

        public boolean hasErrors() {
            return !errors.isEmpty();
        }
    }

    private static final class LoadedScript {
        private final Path file;
        private final ScriptFormat format;
        private final ScriptProgram program;
        private Interpreter interpreter;

        private LoadedScript(Path file, ScriptFormat format, ScriptProgram program, Interpreter interpreter) {
            this.file = file;
            this.format = format;
            this.program = program;
            this.interpreter = interpreter;
        }
    }
}
