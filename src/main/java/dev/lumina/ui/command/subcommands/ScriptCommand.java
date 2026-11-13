package dev.lumina.ui.command.subcommands;

import dev.lumina.ui.LuminaPlugin;
import dev.lumina.ui.command.MessageService;
import dev.lumina.ui.command.SubCommand;
import dev.lumina.ui.script.ScriptFormat;
import dev.lumina.ui.script.ScriptService;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;

/**
 * Inspects and reloads scripts.
 */
public final class ScriptCommand implements SubCommand {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final LuminaPlugin plugin;
    private final MessageService messages;

    public ScriptCommand(LuminaPlugin plugin, MessageService messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    @Override
    public String name() {
        return "script";
    }

    @Override
    public String usage() {
        return "/lumina script <list|reload|formats|run <file> <index>>";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            messages.send(sender, "command.usage", "usage", usage());
            return true;
        }
        ScriptService service = plugin.scriptService();
        switch (args[0].toLowerCase(java.util.Locale.ROOT)) {
            case "reload" -> {
                ScriptService.LoadReport report = service.reload();
                plugin.reportScripts(report);
                messages.sendPrefixed(sender, "script.reloaded",
                        "count", String.valueOf(report.loaded().size()),
                        "handlers", String.valueOf(report.handlers()));
                for (String error : report.errors()) {
                    sender.sendMessage(MINI.deserialize("<red>" + error));
                }
                return true;
            }
            case "formats" -> {
                sender.sendMessage(MINI.deserialize("<gray>Supported script formats:"));
                for (ScriptFormat format : service.formats().all()) {
                    sender.sendMessage(MINI.deserialize("<gray>- <white>" + format.description()));
                }
                return true;
            }
            case "list" -> {
                sender.sendMessage(MINI.deserialize("<gray>Scripts:"));
                for (String name : service.scriptNames()) {
                    String format = service.formatNameOf(name);
                    sender.sendMessage(MINI.deserialize(
                            "<gray>- <white>" + name + " <dark_gray>(" + format + ")"));
                }
                if (service.scriptNames().isEmpty()) {
                    sender.sendMessage(MINI.deserialize("<gray>No scripts loaded."));
                }
                return true;
            }
            case "run" -> {
                if (args.length < 3) {
                    messages.send(sender, "command.usage", "usage", usage());
                    return true;
                }
                int index;
                try {
                    index = Integer.parseInt(args[2]);
                } catch (NumberFormatException invalid) {
                    messages.send(sender, "command.invalid-number", "value", args[2]);
                    return true;
                }
                org.bukkit.entity.Player player =
                        sender instanceof org.bukkit.entity.Player p ? p : null;
                boolean ran = service.runHandler(args[1], index, player);
                if (!ran) {
                    sender.sendMessage(MINI.deserialize("<red>No handler " + index + " in " + args[1]));
                }
                return true;
            }
            default -> {
                messages.send(sender, "command.usage", "usage", usage());
                return true;
            }
        }
    }

    @Override
    public List<String> complete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            for (String option : List.of("list", "reload", "formats", "run")) {
                if (option.startsWith(args[0])) {
                    options.add(option);
                }
            }
            return options;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("run")) {
            List<String> names = new ArrayList<>();
            for (String name : plugin.scriptService().scriptNames()) {
                if (name.startsWith(args[1])) {
                    names.add(name);
                }
            }
            return names;
        }
        return List.of();
    }
}
