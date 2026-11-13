package dev.lumina.ui.command;

import dev.lumina.ui.LuminaPlugin;
import dev.lumina.ui.command.subcommands.BedrockCommand;
import dev.lumina.ui.command.subcommands.CloseCommand;
import dev.lumina.ui.command.subcommands.CreateCommand;
import dev.lumina.ui.command.subcommands.DebugCommand;
import dev.lumina.ui.command.subcommands.DeleteCommand;
import dev.lumina.ui.command.subcommands.EditCommand;
import dev.lumina.ui.command.subcommands.HelpCommand;
import dev.lumina.ui.command.subcommands.ListCommand;
import dev.lumina.ui.command.subcommands.OpenCommand;
import dev.lumina.ui.command.subcommands.ReloadCommand;
import dev.lumina.ui.command.subcommands.ScriptCommand;
import dev.lumina.ui.core.UiRegistry;
import dev.lumina.ui.session.PageService;
import dev.lumina.ui.storage.YamlPageStore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

/**
 * Dispatches {@code /lumina <sub-command>}. Sub-commands are the only place that knows about
 * arguments; this class owns routing, permission checks and tab completion.
 */
public final class CommandRouter implements CommandExecutor, TabCompleter {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final String BASE_PERMISSION = "luminaui.command.use";

    private final Map<String, SubCommand> byName = new LinkedHashMap<>();
    private final List<SubCommand> ordered = new ArrayList<>();
    private final MessageService messages;

    public CommandRouter(
            LuminaPlugin plugin,
            UiRegistry registry,
            PageService pageService,
            YamlPageStore pageStore) {
        this.messages = new MessageService(plugin, plugin.getConfig().getString("lang", "en"));
        register(new HelpCommand(this, messages));
        register(new OpenCommand(registry, pageService, messages));
        register(new CloseCommand(pageService, messages));
        register(new CreateCommand(plugin, registry, pageStore, messages));
        register(new DeleteCommand(registry, pageStore, pageService, messages));
        register(new EditCommand(plugin, registry, pageService, messages));
        register(new ListCommand(registry, messages));
        register(new ReloadCommand(plugin, messages));
        register(new BedrockCommand(plugin, registry, messages));
        register(new ScriptCommand(plugin, messages));
        register(new DebugCommand(plugin, pageService, messages));
    }

    private void register(SubCommand subCommand) {
        ordered.add(subCommand);
        byName.put(subCommand.name().toLowerCase(Locale.ROOT), subCommand);
        for (String alias : subCommand.aliases()) {
            byName.putIfAbsent(alias.toLowerCase(Locale.ROOT), subCommand);
        }
    }

    public List<SubCommand> subCommands() {
        return List.copyOf(ordered);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(BASE_PERMISSION)) {
            sender.sendMessage(MINI.deserialize("<red>You do not have permission to use this command."));
            return true;
        }
        if (args.length == 0) {
            return dispatch(sender, byName.get("help"), new String[0]);
        }
        SubCommand subCommand = byName.get(args[0].toLowerCase(Locale.ROOT));
        if (subCommand == null) {
            sender.sendMessage(MINI.deserialize("<red>Unknown sub-command: " + args[0]));
            return true;
        }
        return dispatch(sender, subCommand, Arrays.copyOfRange(args, 1, args.length));
    }

    private boolean dispatch(CommandSender sender, SubCommand subCommand, String[] args) {
        String permission = subCommand.permission();
        if (!permission.isEmpty() && !sender.hasPermission(permission)) {
            messages.send(sender, "command.no-permission", "permission", permission);
            return true;
        }
        try {
            return subCommand.execute(sender, args);
        } catch (RuntimeException failure) {
            messages.send(sender, "command.failed", "error", String.valueOf(failure.getMessage()));
            return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length <= 1) {
            String prefix = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
            List<String> visible = new ArrayList<>();
            for (SubCommand subCommand : new LinkedHashSet<>(ordered)) {
                if (!subCommand.permission().isEmpty() && !sender.hasPermission(subCommand.permission())) {
                    continue;
                }
                if (subCommand.name().startsWith(prefix)) {
                    visible.add(subCommand.name());
                }
            }
            return visible;
        }
        SubCommand subCommand = byName.get(args[0].toLowerCase(Locale.ROOT));
        if (subCommand == null) {
            return List.of();
        }
        return subCommand.complete(sender, Arrays.copyOfRange(args, 1, args.length));
    }
}
