package dev.lumina.ui.command.subcommands;

import dev.lumina.ui.LuminaPlugin;
import dev.lumina.ui.api.UiPage;
import dev.lumina.ui.command.MessageService;
import dev.lumina.ui.command.SubCommand;
import dev.lumina.ui.core.UiRegistry;
import dev.lumina.ui.storage.YamlPageStore;
import java.io.IOException;
import java.util.List;
import org.bukkit.command.CommandSender;

/**
 * Creates an empty page file and registers it.
 */
public final class CreateCommand implements SubCommand {

    private static final String ID_PATTERN = "[a-z0-9_-]{1,48}";

    private final LuminaPlugin plugin;
    private final UiRegistry registry;
    private final YamlPageStore pageStore;
    private final MessageService messages;

    public CreateCommand(
            LuminaPlugin plugin, UiRegistry registry, YamlPageStore pageStore, MessageService messages) {
        this.plugin = plugin;
        this.registry = registry;
        this.pageStore = pageStore;
        this.messages = messages;
    }

    @Override
    public String name() {
        return "create";
    }

    @Override
    public String usage() {
        return "/lumina create <page>";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            messages.send(sender, "command.usage", "usage", usage());
            return true;
        }
        String pageId = args[0].toLowerCase(java.util.Locale.ROOT);
        if (!pageId.matches(ID_PATTERN)) {
            messages.send(sender, "page.invalid-id", "page", pageId);
            return true;
        }
        if (registry.page(pageId) != null) {
            messages.send(sender, "page.already-exists", "page", pageId);
            return true;
        }

        UiPage page = UiPage.empty(pageId, pageId);
        try {
            pageStore.save(page);
        } catch (IOException | IllegalStateException failure) {
            plugin.reportFailure("saving new page " + pageId, failure);
            messages.send(sender, "page.save-failed", "page", pageId);
            return true;
        }
        registry.registerPage(page);
        messages.sendPrefixed(sender, "page.created", "page", pageId);
        return true;
    }

    @Override
    public List<String> complete(CommandSender sender, String[] args) {
        return List.of();
    }
}
