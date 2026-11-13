package dev.lumina.ui.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.lumina.ui.api.Anchor;
import dev.lumina.ui.api.ElementBounds;
import dev.lumina.ui.api.Style;
import dev.lumina.ui.api.UiNode;
import dev.lumina.ui.api.UiPage;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EditorSessionTest {

    private static UiPage emptyPage() {
        return new UiPage("test", "Test", 1, 1f, false,
                List.of(new UiPage.Layer("main", 0f, true, List.of())));
    }

    private static UiNode.TextNode textWith(String id, String value) {
        return new UiNode.TextNode(id, new ElementBounds(0f, 0f, 100f, 20f), Anchor.TOP_LEFT,
                Style.defaults(), true, value, 1f, 100);
    }

    private static EditorSession session() {
        return new EditorSession(UUID.randomUUID(), emptyPage(), 10);
    }

    @Test
    void addsNodesAndSelectsThem() {
        EditorSession session = session();

        session.addNode("main", textWith("a", "hello"));

        assertEquals(1, session.page().nodeCount());
        assertEquals("a", session.selectedNodeId().orElseThrow());
        assertTrue(session.node("a").isPresent());
        assertTrue(session.dirty());
    }

    @Test
    void editsTextOfTextNodesOnly() {
        EditorSession session = session();
        session.addNode("main", textWith("a", "before"));

        session.setText("a", "after");

        UiNode.TextNode node = (UiNode.TextNode) session.node("a").orElseThrow();
        assertEquals("after", node.text());
    }

    @Test
    void undoAndRedoRestorePreviousStates() {
        EditorSession session = session();
        session.addNode("main", textWith("a", "one"));
        session.setText("a", "two");

        assertTrue(session.undo());
        assertEquals("one", ((UiNode.TextNode) session.node("a").orElseThrow()).text());

        assertTrue(session.redo());
        assertEquals("two", ((UiNode.TextNode) session.node("a").orElseThrow()).text());
    }

    @Test
    void undoStackIsEmptyOnFreshSession() {
        EditorSession session = session();

        assertFalse(session.canUndo());
        assertFalse(session.undo());
    }

    @Test
    void removesNodesAndClearsSelection() {
        EditorSession session = session();
        session.addNode("main", textWith("a", "x"));

        assertTrue(session.removeNode("a"));

        assertEquals(0, session.page().nodeCount());
        assertTrue(session.selectedNodeId().isEmpty());
        assertFalse(session.removeNode("a"));
    }

    @Test
    void historyIsBoundedByConfiguredLimit() {
        EditorSession session = new EditorSession(UUID.randomUUID(), emptyPage(), 2);
        session.addNode("main", textWith("a", "0"));
        session.setText("a", "1");
        session.setText("a", "2");
        session.setText("a", "3");

        int undos = 0;
        while (session.undo()) {
            undos++;
        }

        assertEquals(2, undos);
    }

    @Test
    void resetAdoptsExternalPageAndAllowsUndo() {
        EditorSession session = session();
        session.addNode("main", textWith("a", "mine"));

        UiPage external = new UiPage("test", "Adopted", 1, 1f, false, List.of());
        session.reset(external);

        assertEquals("Adopted", session.page().title());
        assertTrue(session.undo());
        assertEquals("Test", session.page().title());
    }
}
