package dev.lumina.ui.command.subcommands;

import dev.lumina.ui.command.MessageService;
import dev.lumina.ui.command.SubCommand;
import dev.lumina.ui.core.UiRegistry;
import dev.lumina.ui.session.PageService;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Opens a page for the sender at an offset in front of them.
 */
public final class OpenCommand implements SubCommand {

    private final UiRegistry registry;
    private final PageService pageService;
    private final MessageService messages;

    public OpenCommand(UiRegistry registry, PageService pageService, MessageService messages) {
        this.registry = registry;
        this.pageService = pageService;
        this.messages = messages;
    }

    @Override
    public String name() {
        return "open";
    }

    @Override
    public String usage() {
        return "/lumina open <page> [distance]";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "command.players-only");
            return true;
        }
        if (args.length == 0) {
            messages.send(sender, "command.usage", "usage", usage());
            return true;
        }
        String pageId = args[0];
        if (registry.page(pageId) == null) {
            messages.send(sender, "page.not-found", "page", pageId);
            return true;
        }
        float distance = 3f;
        if (args.length > 1) {
            try {
                distance = Float.parseFloat(args[1]);
            } catch (NumberFormatException invalid) {
                messages.send(sender, "command.invalid-number", "value", args[1]);
                return true;
            }
        }

        Location origin = player.getEyeLocation()
                .add(player.getLocation().getDirection().multiply(distance));
        boolean opened = pageService.open(player, pageId, origin);
        if (opened) {
            messages.sendPrefixed(sender, "page.opened", "page", pageId);
        } else {
            messages.send(sender, "page.not-found", "page", pageId);
        }
        return true;
    }

    @Override
    public List<String> complete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> ids = new ArrayList<>();
            for (String id : registry.pageIds()) {
                if (id.startsWith(args[0])) {
                    ids.add(id);
                }
            }
            return ids;
        }
        return List.of();
    }
}
