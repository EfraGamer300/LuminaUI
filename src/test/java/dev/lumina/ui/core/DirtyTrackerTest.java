package dev.lumina.ui.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.lumina.ui.api.Anchor;
import dev.lumina.ui.api.ElementBounds;
import dev.lumina.ui.api.Style;
import dev.lumina.ui.api.UiNode;
import dev.lumina.ui.api.UiPage;
import java.util.List;
import org.junit.jupiter.api.Test;

class DirtyTrackerTest {

    private static ResolvedNode node(String id, float x) {
        UiNode.TextNode text = new UiNode.TextNode(id, new ElementBounds(x, 0f, 10f, 10f),
                Anchor.TOP_LEFT, Style.defaults(), true, "t", 1f, 100);
        return new ResolvedNode(text, "main", 0, x, 0f, 10f, 10f);
    }

    @Test
    void firstFrameReportsEverythingAsAdded() {
        DirtyTracker tracker = new DirtyTracker();

        DirtyTracker.RenderDelta delta = tracker.diff(List.of(node("a", 0f), node("b", 10f)));

        assertEquals(2, delta.added().size());
        assertEquals(0, delta.changed().size());
        assertEquals(0, delta.removed().size());
    }

    @Test
    void steadyFrameProducesEmptyDelta() {
        DirtyTracker tracker = new DirtyTracker();
        tracker.diff(List.of(node("a", 0f)));

        DirtyTracker.RenderDelta delta = tracker.diff(List.of(node("a", 0f)));

        assertTrue(delta.isEmpty());
        assertEquals(1, tracker.trackedCount());
    }

    @Test
    void movedNodeIsReportedAsChanged() {
        DirtyTracker tracker = new DirtyTracker();
        tracker.diff(List.of(node("a", 0f)));

        DirtyTracker.RenderDelta delta = tracker.diff(List.of(node("a", 42f)));

        assertEquals(1, delta.changed().size());
        assertEquals(0, delta.added().size());
    }

    @Test
    void missingNodeIsReportedAsRemoved() {
        DirtyTracker tracker = new DirtyTracker();
        tracker.diff(List.of(node("a", 0f), node("b", 1f)));

        DirtyTracker.RenderDelta delta = tracker.diff(List.of(node("a", 0f)));

        assertEquals(List.of("main/b"), delta.removed());
        assertFalse(delta.isEmpty());
    }

    @Test
    void resetForcesFullRedraw() {
        DirtyTracker tracker = new DirtyTracker();
        tracker.diff(List.of(node("a", 0f)));
        tracker.reset();

        DirtyTracker.RenderDelta delta = tracker.diff(List.of(node("a", 0f)));

        assertEquals(1, delta.added().size());
    }
}
