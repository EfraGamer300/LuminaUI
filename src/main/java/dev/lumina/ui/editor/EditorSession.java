package dev.lumina.ui.editor;

import dev.lumina.ui.api.Anchor;
import dev.lumina.ui.api.BillboardMode;
import dev.lumina.ui.api.ElementBounds;
import dev.lumina.ui.api.Style;
import dev.lumina.ui.api.UiNode;
import dev.lumina.ui.api.UiPage;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * In-memory editing state of one page for one player, with undo/redo and change notification.
 *
 * <p>The session never writes to disk itself: it marks itself dirty and lets the autosave job decide
 * when to persist. That keeps editing responsive regardless of disk speed.
 */
public final class EditorSession {

    private final UUID playerId;
    private final String pageId;
    private final HistoryStack<UiPage> history;
    private UiPage page;
    private String selectedNodeId;
    private boolean dirty;

    public EditorSession(UUID playerId, UiPage page, int undoLimit) {
        this.playerId = playerId;
        this.pageId = page.id();
        this.page = page;
        this.history = new HistoryStack<>(undoLimit);
    }

    public UUID playerId() {
        return playerId;
    }

    public String pageId() {
        return pageId;
    }

    public UiPage page() {
        return page;
    }

    public boolean dirty() {
        return dirty;
    }

    public void markSaved() {
        dirty = false;
    }

    public Optional<String> selectedNodeId() {
        return Optional.ofNullable(selectedNodeId);
    }

    public void select(String nodeId) {
        this.selectedNodeId = nodeId;
    }

    public void clearSelection() {
        this.selectedNodeId = null;
    }

    /** Adds a node to the named layer, creating the layer when it does not exist yet. */
    public void addNode(String layerId, UiNode node) {
        commit(current -> {
            List<UiPage.Layer> layers = new ArrayList<>(current.layers());
            int index = indexOfLayer(layers, layerId);
            if (index < 0) {
                layers.add(new UiPage.Layer(layerId, layers.size(), true, List.of(node)));
            } else {
                UiPage.Layer layer = layers.get(index);
                List<UiNode> nodes = new ArrayList<>(layer.nodes());
                nodes.add(node);
                layers.set(index, layer.withNodes(nodes));
            }
            return current.withLayers(layers);
        });
        this.selectedNodeId = node.id();
    }

    /** Replaces a node by id, keeping its position inside the layer. */
    public void replaceNode(String nodeId, UiNode replacement) {
        commit(current -> mapNodes(current, nodes -> {
            List<UiNode> updated = new ArrayList<>(nodes.size());
            for (UiNode node : nodes) {
                updated.add(node.id().equals(nodeId) ? replacement : node);
            }
            return updated;
        }));
    }

    public boolean removeNode(String nodeId) {
        boolean[] removed = {false};
        commit(current -> mapNodes(current, nodes -> {
            List<UiNode> updated = new ArrayList<>(nodes.size());
            for (UiNode node : nodes) {
                if (node.id().equals(nodeId)) {
                    removed[0] = true;
                } else {
                    updated.add(node);
                }
            }
            return updated;
        }));
        if (removed[0] && nodeId.equals(selectedNodeId)) {
            selectedNodeId = null;
        }
        return removed[0];
    }

    public void setText(String nodeId, String text) {
        updateNode(nodeId, node -> {
            if (node instanceof UiNode.TextNode textNode) {
                return textNode.withText(text);
            }
            return node;
        });
    }

    public void setBounds(String nodeId, ElementBounds bounds) {
        updateNode(nodeId, node -> node.withBounds(bounds));
    }

    public void setVisible(String nodeId, boolean visible) {
        updateNode(nodeId, node -> node.withVisible(visible));
    }

    public void setStyle(String nodeId, Style style) {
        updateNode(nodeId, node -> node.withStyle(style));
    }

    public void setBillboard(String nodeId, BillboardMode mode) {
        updateNode(nodeId, node -> node.withStyle(node.style().toBuilder().billboard(mode).build()));
    }

    public void setAnchor(String nodeId, Anchor anchor) {
        commit(current -> mapNodes(current, nodes -> {
            List<UiNode> updated = new ArrayList<>(nodes.size());
            for (UiNode node : nodes) {
                if (!node.id().equals(nodeId)) {
                    updated.add(node);
                    continue;
                }
                updated.add(withAnchor(node, anchor));
            }
            return updated;
        }));
    }

    public void setTitle(String title) {
        commit(current -> new UiPage(current.id(), title, current.schemaVersion(), current.scale(),
                current.persistent(), current.layers()));
    }

    public void setScale(float scale) {
        commit(current -> current.withScale(scale));
    }

    public Optional<UiNode> node(String nodeId) {
        for (UiPage.Layer layer : page.layers()) {
            for (UiNode node : layer.nodes()) {
                UiNode found = find(node, nodeId);
                if (found != null) {
                    return Optional.of(found);
                }
            }
        }
        return Optional.empty();
    }

    public boolean undo() {
        UiPage restored = history.undo(page);
        if (restored == null) {
            return false;
        }
        this.page = restored;
        this.dirty = true;
        return true;
    }

    public boolean redo() {
        UiPage restored = history.redo(page);
        if (restored == null) {
            return false;
        }
        this.page = restored;
        this.dirty = true;
        return true;
    }

    public boolean canUndo() {
        return history.canUndo();
    }

    public boolean canRedo() {
        return history.canRedo();
    }

    /** Adopts a page changed outside the session (for example a reload) after recording history. */
    public void reset(UiPage page) {
        this.history.push(this.page);
        this.page = page;
        this.dirty = true;
    }

    private void updateNode(String nodeId, java.util.function.UnaryOperator<UiNode> operation) {
        commit(current -> mapNodes(current, nodes -> {
            List<UiNode> updated = new ArrayList<>(nodes.size());
            for (UiNode node : nodes) {
                updated.add(node.id().equals(nodeId) ? operation.apply(node) : node);
            }
            return updated;
        }));
    }

    private void commit(java.util.function.UnaryOperator<UiPage> change) {
        UiPage next = change.apply(page);
        if (next == page) {
            return;
        }
        history.push(page);
        this.page = next;
        this.dirty = true;
    }

    private static UiPage mapNodes(
            UiPage page, java.util.function.UnaryOperator<List<UiNode>> operation) {
        List<UiPage.Layer> layers = new ArrayList<>(page.layers().size());
        for (UiPage.Layer layer : page.layers()) {
            layers.add(layer.withNodes(operation.apply(layer.nodes())));
        }
        return page.withLayers(layers);
    }

    private static UiNode withAnchor(UiNode node, Anchor anchor) {
        if (node instanceof UiNode.TextNode text) {
            return new UiNode.TextNode(text.id(), text.bounds(), anchor, text.style(), text.visible(),
                    text.text(), text.scale(), text.lineWidth());
        }
        if (node instanceof UiNode.ImageNode image) {
            return new UiNode.ImageNode(image.id(), image.bounds(), anchor, image.style(),
                    image.visible(), image.texture(), image.tint(), image.smooth());
        }
        if (node instanceof UiNode.ShapeNode shape) {
            return new UiNode.ShapeNode(shape.id(), shape.bounds(), anchor, shape.style(),
                    shape.visible(), shape.material(), shape.cornerRadius());
        }
        UiNode.GroupNode group = (UiNode.GroupNode) node;
        return new UiNode.GroupNode(group.id(), group.bounds(), anchor, group.style(),
                group.visible(), group.children());
    }

    private static int indexOfLayer(List<UiPage.Layer> layers, String layerId) {
        for (int i = 0; i < layers.size(); i++) {
            if (layers.get(i).id().equals(layerId)) {
                return i;
            }
        }
        return -1;
    }

    private static UiNode find(UiNode node, String nodeId) {
        if (node.id().equals(nodeId)) {
            return node;
        }
        if (node instanceof UiNode.GroupNode group) {
            for (UiNode child : group.children()) {
                UiNode found = find(child, nodeId);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
