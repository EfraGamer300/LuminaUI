package dev.lumina.ui.command.subcommands;

import dev.lumina.ui.LuminaPlugin;
import dev.lumina.ui.api.UiPage;
import dev.lumina.ui.command.MessageService;
import dev.lumina.ui.command.SubCommand;
import dev.lumina.ui.core.UiRegistry;
import dev.lumina.ui.editor.EditorSession;
import dev.lumina.ui.session.PageService;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Opens an editing session and previews the page in front of the sender.
 *
 * <p>Editing happens through a menu plus chat input; the world preview is read-only, so an editor
 * can never break a page by misplacing an entity.
 */
public final class EditCommand implements SubCommand {

    private final LuminaPlugin plugin;
    private final UiRegistry registry;
    private final PageService pageService;
    private final MessageService messages;

    public EditCommand(
            LuminaPlugin plugin, UiRegistry registry, PageService pageService, MessageService messages) {
        this.plugin = plugin;
        this.registry = registry;
        this.pageService = pageService;
        this.messages = messages;
    }

    @Override
    public String name() {
        return "edit";
    }

    @Override
    public String usage() {
        return "/lumina edit <page>";
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
        UiPage page = registry.page(args[0]);
        if (page == null) {
            messages.send(sender, "page.not-found", "page", args[0]);
            return true;
        }

        EditorSession session = plugin.editorService().open(player.getUniqueId(), page);
        plugin.editorMenu().open(player, session);

        Location origin = player.getEyeLocation()
                .add(player.getLocation().getDirection().multiply(4d));
        pageService.open(player, page.id(), origin);
        messages.sendPrefixed(sender, "editor.opened", "page", page.id());
        return true;
    }

    @Override
    public List<String> complete(CommandSender sender, String[] args) {
        if (args.length != 1) {
            return List.of();
        }
        List<String> ids = new ArrayList<>();
        for (String id : registry.pageIds()) {
            if (id.startsWith(args[0])) {
                ids.add(id);
            }
        }
        return ids;
    }
}
