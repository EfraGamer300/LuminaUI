package dev.lumina.ui.command.subcommands;

import dev.lumina.ui.LuminaPlugin;
import dev.lumina.ui.command.MessageService;
import dev.lumina.ui.command.SubCommand;
import org.bukkit.command.CommandSender;

/**
 * Reloads configuration and page files.
 */
public final class ReloadCommand implements SubCommand {

    private final LuminaPlugin plugin;
    private final MessageService messages;

    public ReloadCommand(LuminaPlugin plugin, MessageService messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    @Override
    public String name() {
        return "reload";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        plugin.reloadEverything();
        messages.sendPrefixed(sender, "plugin.reloaded");
        return true;
    }
}
