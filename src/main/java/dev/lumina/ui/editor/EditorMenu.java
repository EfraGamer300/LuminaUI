package dev.lumina.ui.editor;

import dev.lumina.ui.LuminaPlugin;
import dev.lumina.ui.api.UiNode;
import dev.lumina.ui.api.UiPage;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Inventory-based editor UI.
 *
 * <p>Deliberately simple and robust: one row of actions, one row of the page's elements. Selecting an
 * element opens its property actions; every mutation goes through {@link EditorSession}, so undo and
 * autosave apply to menu edits exactly like they do to command edits.
 */
public final class EditorMenu implements Listener {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final int SIZE = 54;
    private static final int ACTION_ROW_OFFSET = 0;
    private static final int NODE_ROW_OFFSET = 18;

    private final LuminaPlugin plugin;

    public EditorMenu(LuminaPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, EditorSession session) {
        Inventory inventory = Bukkit.createInventory(null, SIZE,
                MINI.deserialize("<dark_gray>Editing <white>" + session.pageId()));
        fillActions(inventory, session);
        fillNodes(inventory, session);
        player.openInventory(inventory);
    }

    private void fillActions(Inventory inventory, EditorSession session) {
        inventory.setItem(ACTION_ROW_OFFSET, item(Material.NAME_TAG, "<white>Title",
                "<gray>Current: <white>" + session.page().title()));
        inventory.setItem(ACTION_ROW_OFFSET + 1, item(Material.ARROW, "<green>Undo",
                "<gray>Available: <white>" + session.canUndo()));
        inventory.setItem(ACTION_ROW_OFFSET + 2, item(Material.SPECTRAL_ARROW, "<yellow>Redo",
                "<gray>Available: <white>" + session.canRedo()));
        inventory.setItem(ACTION_ROW_OFFSET + 3, item(Material.EMERALD_BLOCK, "<green>Save",
                "<gray>Writes the page to disk"));
        inventory.setItem(ACTION_ROW_OFFSET + 4, item(Material.LIME_DYE, "<green>Add text node",
                "<gray>Creates a text element at the centre"));
        inventory.setItem(ACTION_ROW_OFFSET + 5, item(Material.LIGHT_BLUE_DYE, "<aqua>Add shape node",
                "<gray>Creates a solid rectangle element"));
        inventory.setItem(ACTION_ROW_OFFSET + 8, item(Material.BARRIER, "<red>Close",
                "<gray>Closes the editor"));
    }

    private void fillNodes(Inventory inventory, EditorSession session) {
        int slot = NODE_ROW_OFFSET;
        for (UiPage.Layer layer : session.page().layers()) {
            for (UiNode node : layer.nodes()) {
                if (slot >= SIZE) {
                    return;
                }
                Material material = switch (node) {
                    case UiNode.TextNode ignored -> Material.PAPER;
                    case UiNode.ImageNode ignored -> Material.MAP;
                    case UiNode.ShapeNode ignored -> Material.PRISMARINE_SHARD;
                    case UiNode.GroupNode ignored -> Material.CHEST;
                };
                inventory.setItem(slot++, item(material, "<white>" + node.id(),
                        "<gray>layer: <white>" + layer.id(),
                        "<gray>type: <white>" + node.getClass().getSimpleName(),
                        "<gray>visible: <white>" + node.visible(),
                        "<dark_gray>click to edit properties"));
            }
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        EditorSession session = plugin.editorService().session(player.getUniqueId());
        if (session == null) {
            return;
        }
        Component title = event.getView().title();
        if (!MINI.serialize(title).contains(session.pageId())) {
            return;
        }
        event.setCancelled(true);

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= SIZE) {
            return;
        }
        handleAction(player, session, slot);
    }

    private void handleAction(Player player, EditorSession session, int slot) {
        switch (slot) {
            case 1 -> {
                if (session.undo()) {
                    afterChange(player, session, "Undid last change");
                } else {
                    message(player, "<red>Nothing to undo");
                }
            }
            case 2 -> {
                if (session.redo()) {
                    afterChange(player, session, "Redid change");
                } else {
                    message(player, "<red>Nothing to redo");
                }
            }
            case 3 -> {
                plugin.editorService().flush(session);
                message(player, "<green>Page saved");
            }
            case 4 -> {
                session.addNode("main", new UiNode.TextNode(
                        nextId(session, "text"),
                        new dev.lumina.ui.api.ElementBounds(24f, 24f, 160f, 40f),
                        dev.lumina.ui.api.Anchor.TOP_LEFT,
                        dev.lumina.ui.api.Style.defaults(),
                        true,
                        "<white>New text",
                        1f,
                        160));
                afterChange(player, session, "Text node added");
            }
            case 5 -> {
                session.addNode("main", new UiNode.ShapeNode(
                        nextId(session, "shape"),
                        new dev.lumina.ui.api.ElementBounds(24f, 80f, 160f, 60f),
                        dev.lumina.ui.api.Anchor.TOP_LEFT,
                        dev.lumina.ui.api.Style.builder().accent(0xFF38BDF8).build(),
                        true,
                        "minecraft:light_blue_concrete",
                        4f));
                afterChange(player, session, "Shape node added");
            }
            case 8 -> {
                plugin.editorService().close(player.getUniqueId());
                player.closeInventory();
                message(player, "<gray>Editor closed");
            }
            default -> {
                if (slot >= NODE_ROW_OFFSET) {
                    selectNode(player, session, slot - NODE_ROW_OFFSET);
                }
            }
        }
    }

    private void selectNode(Player player, EditorSession session, int index) {
        UiNode node = nodeAt(session.page(), index);
        if (node == null) {
            return;
        }
        session.select(node.id());
        message(player, "<gray>Selected <white>" + node.id()
                + "<gray>. Use <white>/lumina set <property> <value><gray>.");
        open(player, session);
    }

    private void afterChange(Player player, EditorSession session, String what) {
        plugin.editorService().markChanged(session);
        plugin.pageService().invalidatePage(session.pageId());
        message(player, "<green>" + what);
        open(player, session);
    }

    private static UiNode nodeAt(UiPage page, int index) {
        List<UiNode> flat = new ArrayList<>();
        for (UiPage.Layer layer : page.layers()) {
            flat.addAll(layer.nodes());
        }
        return index < flat.size() ? flat.get(index) : null;
    }

    private static String nextId(EditorSession session, String prefix) {
        int counter = 1;
        while (session.node(prefix + "_" + counter).isPresent()) {
            counter++;
        }
        return prefix + "_" + counter;
    }

    private static ItemStack item(Material material, String name, String... lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(MINI.deserialize(name));
            List<Component> lines = new ArrayList<>(lore.length);
            for (String line : lore) {
                lines.add(MINI.deserialize(line));
            }
            meta.lore(lines);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private static void message(Player player, String text) {
        player.sendMessage(MINI.deserialize(text));
    }
}
