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

class LayoutEngineTest {

    private static UiNode.TextNode text(String id, float x, float y, float w, float h) {
        return new UiNode.TextNode(id, new ElementBounds(x, y, w, h), Anchor.TOP_LEFT,
                Style.defaults(), true, id, 1f, 100);
    }

    @Test
    void resolvesNodesInLayerOrder() {
        UiPage page = new UiPage("p", "P", 1, 1f, false, List.of(
                new UiPage.Layer("top", 5f, true, List.of(text("b", 0, 0, 10, 10))),
                new UiPage.Layer("bottom", 0f, true, List.of(text("a", 0, 0, 10, 10)))));

        List<ResolvedNode> resolved = new LayoutEngine(100f, 100f).resolve(page);

        assertEquals(2, resolved.size());
        assertEquals("a", resolved.get(0).node().id());
        assertEquals("b", resolved.get(1).node().id());
    }

    @Test
    void appliesAnchorOffset() {
        UiNode centered = new UiNode.TextNode("c", new ElementBounds(50f, 50f, 20f, 10f),
                Anchor.CENTER, Style.defaults(), true, "c", 1f, 100);
        UiPage page = new UiPage("p", "P", 1, 1f, false,
                List.of(new UiPage.Layer("l", 0f, true, List.of(centered))));

        ResolvedNode node = new LayoutEngine(100f, 100f).resolve(page).get(0);

        assertEquals(40f, node.x());
        assertEquals(45f, node.y());
    }

    @Test
    void skipsInvisibleNodesAndTheirChildren() {
        UiNode child = text("child", 0, 0, 5, 5);
        UiNode hiddenGroup = new UiNode.GroupNode("g", new ElementBounds(0, 0, 50, 50),
                Anchor.TOP_LEFT, Style.defaults(), false, List.of(child));
        UiPage page = new UiPage("p", "P", 1, 1f, false,
                List.of(new UiPage.Layer("l", 0f, true, List.of(hiddenGroup))));

        assertTrue(new LayoutEngine(100f, 100f).resolve(page).isEmpty());
    }

    @Test
    void offsetsGroupChildrenByGroupOrigin() {
        UiNode child = text("child", 5, 5, 10, 10);
        UiNode group = new UiNode.GroupNode("g", new ElementBounds(20, 30, 100, 100),
                Anchor.TOP_LEFT, Style.defaults(), true, List.of(child));
        UiPage page = new UiPage("p", "P", 1, 1f, false,
                List.of(new UiPage.Layer("l", 0f, true, List.of(group))));

        List<ResolvedNode> resolved = new LayoutEngine(200f, 200f).resolve(page);

        assertEquals(2, resolved.size());
        assertEquals(25f, resolved.get(1).x());
        assertEquals(35f, resolved.get(1).y());
    }

    @Test
    void normalisesCoordinatesAgainstViewport() {
        LayoutEngine engine = new LayoutEngine(200f, 100f);

        assertEquals(0.5f, engine.normaliseX(100f));
        assertEquals(0.25f, engine.normaliseY(25f));
    }

    @Test
    void hitTestingPrefersTopmostNode() {
        UiPage page = new UiPage("p", "P", 1, 1f, false, List.of(
                new UiPage.Layer("a", 0f, true, List.of(text("under", 0, 0, 100, 100))),
                new UiPage.Layer("b", 1f, true, List.of(text("over", 0, 0, 100, 100)))));
        List<ResolvedNode> resolved = new LayoutEngine(200f, 200f).resolve(page);

        HitTester tester = new HitTester();

        assertEquals("over", tester.topmost(resolved, 10f, 10f).node().id());
        assertFalse(tester.topmost(resolved, 500f, 500f) != null);
    }
}
