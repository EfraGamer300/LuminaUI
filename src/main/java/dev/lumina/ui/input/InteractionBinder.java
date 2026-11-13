package dev.lumina.ui.input;

import dev.lumina.ui.api.UiNode;
import dev.lumina.ui.core.ResolvedNode;
import dev.lumina.ui.render.PageProjection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Maintains one {@link Interaction} hitbox per interactive node of a page.
 *
 * <p>Hitboxes follow the node's resolved bounds, so a moving or resized element keeps an accurate
 * click area without extra work. When a viewer is present the hitboxes are shown only to that
 * player, which is what per-player HUD pages need.
 */
public final class InteractionBinder implements AutoCloseable {

    private final Plugin plugin;
    private final PageProjection projection;
    private final Player viewer;
    private final Map<String, Interaction> hitboxes = new HashMap<>();
    private final Map<Integer, String> entityToNode = new HashMap<>();

    public InteractionBinder(Plugin plugin, PageProjection projection, Player viewer) {
        this.plugin = plugin;
        this.projection = projection;
        this.viewer = viewer;
    }

    /**
     * Syncs hitboxes with the current frame. Nodes that disappeared are removed, new ones are
     * spawned and existing ones only move when needed.
     */
    public void sync(List<ResolvedNode> frame, boolean layerInteractive) {
        if (!layerInteractive) {
            clear();
            return;
        }
        List<String> seen = new ArrayList<>(frame.size());
        for (ResolvedNode node : frame) {
            if (node.node() instanceof UiNode.GroupNode) {
                continue;
            }
            String key = keyOf(node);
            seen.add(key);
            Interaction interaction = hitboxes.get(key);
            if (interaction == null || !interaction.isValid()) {
                interaction = spawn(node);
                hitboxes.put(key, interaction);
                entityToNode.put(interaction.getEntityId(), key);
            } else {
                applyGeometry(interaction, node);
            }
        }

        List<String> stale = new ArrayList<>();
        for (String key : hitboxes.keySet()) {
            if (!seen.contains(key)) {
                stale.add(key);
            }
        }
        for (String key : stale) {
            Interaction interaction = hitboxes.remove(key);
            if (interaction != null) {
                entityToNode.remove(interaction.getEntityId());
                interaction.remove();
            }
        }
    }

    /** Returns the node key behind an interaction entity id, or {@code null}. */
    public String nodeKeyOf(int entityId) {
        return entityToNode.get(entityId);
    }

    public int size() {
        return hitboxes.size();
    }

    private Interaction spawn(ResolvedNode node) {
        Interaction interaction = projection.world().spawn(centre(node), Interaction.class);
        interaction.setResponsive(true);
        interaction.setVisibleByDefault(viewer != null);
        applyGeometry(interaction, node);
        if (viewer != null) {
            viewer.showEntity(plugin, interaction);
        }
        return interaction;
    }

    private void applyGeometry(Interaction interaction, ResolvedNode node) {
        float width = Math.max(0.05f, node.width() / projection.pixelsPerBlock());
        float height = Math.max(0.05f, node.height() / projection.pixelsPerBlock());
        interaction.setInteractionWidth(width);
        interaction.setInteractionHeight(height);
        interaction.teleport(centre(node));
    }

    /**
     * Interaction hitboxes are centred on their entity position, while node bounds describe the
     * top-left corner - so the hitbox has to be placed at the bounds centre to match the visual.
     */
    private org.bukkit.Location centre(ResolvedNode node) {
        return projection.toWorld(node.x() + node.width() * 0.5f, node.y() + node.height() * 0.5f);
    }

    private static String keyOf(ResolvedNode node) {
        return node.layerId() + '/' + node.node().id();
    }

    private void clear() {
        for (Interaction interaction : hitboxes.values()) {
            if (interaction.isValid()) {
                interaction.remove();
            }
        }
        hitboxes.clear();
        entityToNode.clear();
    }

    @Override
    public void close() {
        clear();
    }
}
