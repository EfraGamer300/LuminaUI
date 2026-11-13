package dev.lumina.ui.command.subcommands;

import dev.lumina.ui.command.MessageService;
import dev.lumina.ui.command.SubCommand;
import dev.lumina.ui.session.PageService;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Closes the sender's open page.
 */
public final class CloseCommand implements SubCommand {

    private final PageService pageService;
    private final MessageService messages;

    public CloseCommand(PageService pageService, MessageService messages) {
        this.pageService = pageService;
        this.messages = messages;
    }

    @Override
    public String name() {
        return "close";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "command.players-only");
            return true;
        }
        if (!pageService.isOpen(player.getUniqueId())) {
            messages.send(sender, "page.none-open");
            return true;
        }
        pageService.close(player.getUniqueId());
        messages.sendPrefixed(sender, "page.closed");
        return true;
    }
}
