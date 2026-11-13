package dev.lumina.ui.render;

import dev.lumina.ui.api.UiNode;
import dev.lumina.ui.api.UiPage;
import dev.lumina.ui.core.DirtyTracker;
import dev.lumina.ui.core.LayoutEngine;
import dev.lumina.ui.core.ResolvedNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.Plugin;

/**
 * Owns the display entities of one rendered page and redraws only what changed.
 *
 * <p>The pipeline is split in two steps: layout (pure, off the region thread) and apply (on the
 * region thread, touching entities). A static page therefore costs a layout pass and an empty delta;
 * no packets and no entity updates are produced when nothing moved.
 *
 * <p>When a viewer is supplied the displays exist server-side but are shown only to that player, so
 * two players can hold different pages at the same spot without seeing each other's elements.
 */
public final class RenderSession implements AutoCloseable {

    private final Plugin plugin;
    private final Player viewer;
    private final UiPage page;
    private final PageProjection projection;
    private final LayoutEngine layout;
    private final DirtyTracker dirty = new DirtyTracker();
    private final DisplayPool pool;
    private final TextRenderer textRenderer;
    private final ItemRenderer itemRenderer;
    private final Map<String, Display> live = new HashMap<>();

    private List<ResolvedNode> lastFrame = List.of();
    private int lastDirtySize;

    public RenderSession(
            Plugin plugin,
            Player viewer,
            UiPage page,
            PageProjection projection,
            LayoutEngine layout,
            TextRenderer textRenderer,
            ItemRenderer itemRenderer) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.page = page;
        this.projection = projection;
        this.layout = layout;
        this.textRenderer = textRenderer;
        this.itemRenderer = itemRenderer;
        this.pool = new DisplayPool(projection.origin());
    }

    /** Recomputes layout and applies the delta to the world. Must run on the region thread. */
    public void tick() {
        List<ResolvedNode> current = layout.resolve(page);
        DirtyTracker.RenderDelta delta = dirty.diff(current);
        lastDirtySize = delta.size();
        lastFrame = current;

        for (String key : delta.removed()) {
            releaseKey(key);
        }
        for (ResolvedNode node : delta.added()) {
            spawn(node);
        }
        for (ResolvedNode node : delta.changed()) {
            update(node);
        }
    }

    /**
     * Rebuilds every element from scratch. Used when a page definition is replaced or a resource
     * reload invalidates prototypes.
     */
    public void invalidate() {
        dirty.reset();
        for (Display display : new ArrayList<>(live.values())) {
            pool.release(display);
        }
        live.clear();
        itemRenderer.invalidate();
    }

    public int lastDirtySize() {
        return lastDirtySize;
    }

    public int liveCount() {
        return live.size();
    }

    public List<ResolvedNode> lastFrame() {
        return lastFrame;
    }

    public PageProjection projection() {
        return projection;
    }

    public UiPage page() {
        return page;
    }

    public DisplayPool pool() {
        return pool;
    }

    public UUID viewerId() {
        return viewer == null ? null : viewer.getUniqueId();
    }

    private void spawn(ResolvedNode node) {
        if (node.node() instanceof UiNode.TextNode text) {
            TextDisplay display = pool.acquireText();
            textRenderer.apply(display, text, projection);
            place(display, node);
            live.put(key(node), display);
        } else if (node.node() instanceof UiNode.ShapeNode shape) {
            ItemDisplay display = pool.acquireItem();
            itemRenderer.apply(display, shape, projection);
            place(display, node);
            live.put(key(node), display);
        } else if (node.node() instanceof UiNode.ImageNode image) {
            ItemDisplay display = pool.acquireItem();
            itemRenderer.apply(display, image, projection);
            place(display, node);
            live.put(key(node), display);
        }
    }

    private void place(Display display, ResolvedNode node) {
        display.teleport(projection.toWorld(
                node.x() + node.width() * 0.5f, node.y() + node.height() * 0.5f));
        if (viewer == null) {
            display.setVisibleByDefault(true);
        } else {
            display.setVisibleByDefault(false);
            viewer.showEntity(plugin, display);
        }
    }

    private void update(ResolvedNode node) {
        Display display = live.get(key(node));
        if (display == null) {
            spawn(node);
            return;
        }
        display.teleport(projection.toWorld(
                node.x() + node.width() * 0.5f, node.y() + node.height() * 0.5f));
        if (display instanceof TextDisplay textDisplay && node.node() instanceof UiNode.TextNode text) {
            textRenderer.apply(textDisplay, text, projection);
        } else if (display instanceof ItemDisplay itemDisplay) {
            if (node.node() instanceof UiNode.ShapeNode shape) {
                itemRenderer.apply(itemDisplay, shape, projection);
            } else if (node.node() instanceof UiNode.ImageNode image) {
                itemRenderer.apply(itemDisplay, image, projection);
            }
        }
    }

    private void releaseKey(String key) {
        Display display = live.remove(key);
        if (display != null) {
            pool.release(display);
        }
    }

    private static String key(ResolvedNode node) {
        return node.layerId() + '/' + node.node().id();
    }

    @Override
    public void close() {
        for (Display display : live.values()) {
            pool.release(display);
        }
        live.clear();
        pool.close();
        dirty.reset();
    }
}
