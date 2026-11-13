package dev.lumina.ui.command.subcommands;

import dev.lumina.ui.command.MessageService;
import dev.lumina.ui.command.SubCommand;
import java.util.List;
import org.bukkit.command.CommandSender;

/**
 * Prints the command overview.
 */
public final class HelpCommand implements SubCommand {

    private final dev.lumina.ui.command.CommandRouter router;
    private final MessageService messages;

    public HelpCommand(dev.lumina.ui.command.CommandRouter router, MessageService messages) {
        this.router = router;
        this.messages = messages;
    }

    @Override
    public String name() {
        return "help";
    }

    @Override
    public List<String> aliases() {
        return List.of("?");
    }

    @Override
    public String permission() {
        return "luminaui.command.use";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        messages.sendPrefixed(sender, "command.help-header");
        for (SubCommand subCommand : router.subCommands()) {
            if (!subCommand.permission().isEmpty() && !sender.hasPermission(subCommand.permission())) {
                continue;
            }
            sender.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                    .deserialize("<gray>" + subCommand.usage()));
        }
        return true;
    }
}
