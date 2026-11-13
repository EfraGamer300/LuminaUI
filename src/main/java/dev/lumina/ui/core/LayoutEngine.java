package dev.lumina.ui.core;

import dev.lumina.ui.api.Anchor;
import dev.lumina.ui.api.UiNode;
import dev.lumina.ui.api.UiPage;
import java.util.ArrayList;
import java.util.List;

/**
 * Resolves a {@link UiPage} into absolute coordinates inside a virtual viewport.
 *
 * <p>The engine is intentionally free of platform types: it can run on any thread and is covered by
 * plain unit tests. Layers are emitted bottom-up; nodes keep their declared order inside a layer.
 */
public final class LayoutEngine {

    private final float viewportWidth;
    private final float viewportHeight;

    public LayoutEngine(float viewportWidth, float viewportHeight) {
        if (!(viewportWidth > 0f) || !(viewportHeight > 0f)) {
            throw new IllegalArgumentException("viewport must be positive: " + viewportWidth + "x" + viewportHeight);
        }
        this.viewportWidth = viewportWidth;
        this.viewportHeight = viewportHeight;
    }

    public float viewportWidth() {
        return viewportWidth;
    }

    public float viewportHeight() {
        return viewportHeight;
    }

    /**
     * Resolves every visible node of the page. Nodes marked invisible are skipped, as are children
     * of invisible groups.
     */
    public List<ResolvedNode> resolve(UiPage page) {
        List<UiPage.Layer> layers = new ArrayList<>(page.layers());
        layers.sort((a, b) -> Float.compare(a.depth(), b.depth()));

        List<ResolvedNode> resolved = new ArrayList<>();
        for (UiPage.Layer layer : layers) {
            for (UiNode node : layer.nodes()) {
                appendNode(resolved, node, layer.id(), 0, 0f, 0f);
            }
        }
        return resolved;
    }

    private void appendNode(
            List<ResolvedNode> out, UiNode node, String layerId, int depth, float offsetX, float offsetY) {
        if (!node.visible()) {
            return;
        }
        Anchor anchor = node.anchor();
        float x = offsetX + node.bounds().x() + anchor.offsetX(node.bounds().width());
        float y = offsetY + node.bounds().y() + anchor.offsetY(node.bounds().height());
        float width = node.bounds().width();
        float height = node.bounds().height();

        out.add(new ResolvedNode(node, layerId, depth, x, y, width, height));

        if (node instanceof UiNode.GroupNode group) {
            for (UiNode child : group.children()) {
                appendNode(out, child, layerId, depth + 1, x, y);
            }
        }
    }

    /**
     * Converts a page-space X coordinate to a normalised viewport coordinate in {@code [0,1]}.
     */
    public float normaliseX(float pageX) {
        return pageX / viewportWidth;
    }

    public float normaliseY(float pageY) {
        return pageY / viewportHeight;
    }
}
