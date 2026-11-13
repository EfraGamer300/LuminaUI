package dev.lumina.ui.command.subcommands;

import dev.lumina.ui.command.MessageService;
import dev.lumina.ui.command.SubCommand;
import dev.lumina.ui.core.UiRegistry;
import dev.lumina.ui.session.PageService;
import dev.lumina.ui.storage.YamlPageStore;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.command.CommandSender;

/**
 * Removes a page definition, its file and any session showing it.
 */
public final class DeleteCommand implements SubCommand {

    private final UiRegistry registry;
    private final YamlPageStore pageStore;
    private final PageService pageService;
    private final MessageService messages;

    public DeleteCommand(
            UiRegistry registry,
            YamlPageStore pageStore,
            PageService pageService,
            MessageService messages) {
        this.registry = registry;
        this.pageStore = pageStore;
        this.pageService = pageService;
        this.messages = messages;
    }

    @Override
    public String name() {
        return "delete";
    }

    @Override
    public String usage() {
        return "/lumina delete <page> [confirm]";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            messages.send(sender, "command.usage", "usage", usage());
            return true;
        }
        String pageId = args[0];
        if (registry.page(pageId) == null) {
            messages.send(sender, "page.not-found", "page", pageId);
            return true;
        }
        if (args.length < 2 || !args[1].equalsIgnoreCase("confirm")) {
            messages.send(sender, "page.delete-confirm", "page", pageId);
            return true;
        }

        pageService.closePage(pageId);
        registry.unregisterPage(pageId);
        boolean removed = pageStore.delete(pageId);
        messages.sendPrefixed(sender, removed ? "page.deleted" : "page.delete-failed", "page", pageId);
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
        if (args.length == 2) {
            return List.of("confirm");
        }
        return List.of();
    }
}
