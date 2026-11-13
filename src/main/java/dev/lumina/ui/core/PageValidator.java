package dev.lumina.ui.core;

import dev.lumina.ui.api.UiNode;
import java.util.ArrayList;
import java.util.List;

/**
 * Validates page definitions before they are persisted or opened.
 */
public final class PageValidator {

    private final int maxNodesPerPage;

    public PageValidator(int maxNodesPerPage) {
        if (maxNodesPerPage <= 0) {
            throw new IllegalArgumentException("maxNodesPerPage must be > 0: " + maxNodesPerPage);
        }
        this.maxNodesPerPage = maxNodesPerPage;
    }

    public int maxNodesPerPage() {
        return maxNodesPerPage;
    }

    /** Returns human-readable problems; an empty list means the page is valid. */
    public List<String> validate(dev.lumina.ui.api.UiPage page) {
        List<String> problems = new ArrayList<>();
        if (page.title() == null || page.title().isBlank()) {
            problems.add("title must not be blank");
        }
        if (page.nodeCount() > maxNodesPerPage) {
            problems.add("node count " + page.nodeCount() + " exceeds limit " + maxNodesPerPage);
        }

        List<String> ids = new ArrayList<>();
        for (dev.lumina.ui.api.UiPage.Layer layer : page.layers()) {
            if (layer.depth() < 0f) {
                problems.add("layer '" + layer.id() + "' has negative depth");
            }
            for (UiNode node : layer.nodes()) {
                collectIds(node, ids, problems);
            }
        }

        List<String> seen = new ArrayList<>(ids.size());
        for (String id : ids) {
            if (seen.contains(id)) {
                problems.add("duplicate node id '" + id + "'");
            } else {
                seen.add(id);
            }
        }
        return problems;
    }

    private static void collectIds(UiNode node, List<String> ids, List<String> problems) {
        ids.add(node.id());
        if (!(node.bounds().width() > 0f) || !(node.bounds().height() > 0f)) {
            problems.add("node '" + node.id() + "' has a zero-sized bounds");
        }
        if (node instanceof UiNode.GroupNode group) {
            for (UiNode child : group.children()) {
                collectIds(child, ids, problems);
            }
        }
    }
}
