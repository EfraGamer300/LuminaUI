package dev.lumina.ui.core;

import java.util.List;

/**
 * Hit testing over resolved nodes. Nodes are tested from the topmost drawn element downwards, so the
 * last drawn interactive node wins - matching what the player visually sees on top.
 */
public final class HitTester {

    public ResolvedNode topmost(List<ResolvedNode> nodes, float x, float y) {
        for (int i = nodes.size() - 1; i >= 0; i--) {
            ResolvedNode candidate = nodes.get(i);
            if (candidate.contains(x, y)) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * Same as {@link #topmost} but restricted to nodes that can receive interaction. Group nodes are
     * containers only and never capture input on their own.
     */
    public ResolvedNode topmostInteractive(List<ResolvedNode> nodes, float x, float y) {
        for (int i = nodes.size() - 1; i >= 0; i--) {
            ResolvedNode candidate = nodes.get(i);
            if (candidate.node() instanceof dev.lumina.ui.api.UiNode.GroupNode) {
                continue;
            }
            if (candidate.contains(x, y)) {
                return candidate;
            }
        }
        return null;
    }
}
