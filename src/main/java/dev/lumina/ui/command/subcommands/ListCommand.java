package dev.lumina.ui.command.subcommands;

import dev.lumina.ui.command.MessageService;
import dev.lumina.ui.command.SubCommand;
import dev.lumina.ui.core.UiRegistry;
import java.util.List;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;

/**
 * Lists available pages with their element counts.
 */
public final class ListCommand implements SubCommand {

    private final UiRegistry registry;
    private final MessageService messages;

    public ListCommand(UiRegistry registry, MessageService messages) {
        this.registry = registry;
        this.messages = messages;
    }

    @Override
    public String name() {
        return "list";
    }

    @Override
    public List<String> aliases() {
        return List.of("ls");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        messages.sendPrefixed(sender, "page.list-header");
        int count = 0;
        for (String id : registry.pageIds()) {
            var page = registry.page(id);
            if (page == null) {
                continue;
            }
            count++;
            sender.sendMessage(MiniMessage.miniMessage().deserialize(
                    "<gray>- <white>" + page.id() + " <dark_gray>(" + page.nodeCount() + " nodes)</dark_gray>"));
        }
        if (count == 0) {
            messages.send(sender, "page.list-empty");
        }
        return true;
    }

    @Override
    public List<String> complete(CommandSender sender, String[] args) {
        return List.of();
    }
}
