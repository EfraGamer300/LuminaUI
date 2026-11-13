package dev.lumina.ui.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Computes the minimal set of node changes between two frames of the same page.
 *
 * <p>Rendering only what changed keeps packet traffic low when a page is static: after the first
 * frame a steady page produces an empty delta and therefore no work at all.
 */
public final class DirtyTracker {

    private final Map<String, ResolvedNode> previous = new LinkedHashMap<>();

    /**
     * Diffs {@code current} against the last frame and stores it as the new baseline.
     */
    public RenderDelta diff(List<ResolvedNode> current) {
        List<ResolvedNode> added = new ArrayList<>();
        List<ResolvedNode> changed = new ArrayList<>();
        Map<String, ResolvedNode> next = new LinkedHashMap<>(current.size() * 2);

        for (ResolvedNode node : current) {
            String id = keyOf(node);
            next.put(id, node);
            ResolvedNode old = previous.get(id);
            if (old == null) {
                added.add(node);
            } else if (!sameVisualState(old, node)) {
                changed.add(node);
            }
        }

        List<String> removed = new ArrayList<>();
        for (String id : previous.keySet()) {
            if (!next.containsKey(id)) {
                removed.add(id);
            }
        }

        previous.clear();
        previous.putAll(next);
        return new RenderDelta(added, changed, removed);
    }

    /** Drops the baseline, forcing the next frame to be a full redraw. */
    public void reset() {
        previous.clear();
    }

    public int trackedCount() {
        return previous.size();
    }

    private static String keyOf(ResolvedNode node) {
        return node.layerId() + '/' + node.node().id();
    }

    private static boolean sameVisualState(ResolvedNode a, ResolvedNode b) {
        if (Float.compare(a.x(), b.x()) != 0
                || Float.compare(a.y(), b.y()) != 0
                || Float.compare(a.width(), b.width()) != 0
                || Float.compare(a.height(), b.height()) != 0
                || a.depth() != b.depth()) {
            return false;
        }
        return a.node().equals(b.node());
    }

    /** Frame delta produced by {@link #diff}. */
    public record RenderDelta(
            List<ResolvedNode> added, List<ResolvedNode> changed, List<String> removed) {

        public RenderDelta {
            added = List.copyOf(added);
            changed = List.copyOf(changed);
            removed = List.copyOf(removed);
        }

        public boolean isEmpty() {
            return added.isEmpty() && changed.isEmpty() && removed.isEmpty();
        }

        /** Total number of touched elements; used by the perf counters. */
        public int size() {
            return added.size() + changed.size() + removed.size();
        }
    }
}
