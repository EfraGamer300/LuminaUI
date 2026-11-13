package dev.lumina.ui.api;

import java.util.List;

/**
 * A renderable UI page: an ordered list of layers, each holding nodes.
 * Pages are immutable snapshots; edits produce a new page instance.
 */
public record UiPage(
        String id,
        String title,
        int schemaVersion,
        float scale,
        boolean persistent,
        List<Layer> layers) {

    public static final int CURRENT_SCHEMA_VERSION = 1;

    public UiPage {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (!(scale > 0f)) {
            throw new IllegalArgumentException("scale must be > 0: " + scale);
        }
        layers = List.copyOf(layers);
        for (Layer layer : layers) {
            if (layer == null) {
                throw new IllegalArgumentException("layers must not contain null");
            }
        }
    }

    public static UiPage empty(String id, String title) {
        return new UiPage(id, title, CURRENT_SCHEMA_VERSION, 1f, false, List.of());
    }

    public int nodeCount() {
        int total = 0;
        for (Layer layer : layers) {
            total += layer.nodes().size();
        }
        return total;
    }

    public UiPage withLayers(List<Layer> value) {
        return new UiPage(id, title, schemaVersion, scale, persistent, value);
    }

    public UiPage withScale(float value) {
        return new UiPage(id, title, schemaVersion, value, persistent, layers);
    }

    /**
     * One depth layer of the page. Layers are drawn bottom-up in declaration order.
     */
    public record Layer(String id, float depth, boolean interactive, List<UiNode> nodes) {

        public Layer {
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("layer id must not be blank");
            }
            nodes = List.copyOf(nodes);
        }

        public Layer withNodes(List<UiNode> value) {
            return new Layer(id, depth, interactive, value);
        }
    }
}
