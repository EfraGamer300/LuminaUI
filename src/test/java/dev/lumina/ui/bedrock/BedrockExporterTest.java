package dev.lumina.ui.bedrock;

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

class BedrockExporterTest {

    private static UiPage page(UiNode... nodes) {
        return new UiPage("p", "Test", 1, 1f, false,
                List.of(new UiPage.Layer("main", 0f, true, List.of(nodes))));
    }

    private static UiNode.TextNode text(String id, String value) {
        return new UiNode.TextNode(id, new ElementBounds(0, 0, 10, 10), Anchor.TOP_LEFT,
                Style.defaults(), true, value, 1f, 100);
    }

    @Test
    void mapsTextNodesToLabels() {
        BedrockExporter.ExportResult result = new BedrockExporter()
                .export(page(text("a", "Hello"), text("b", "World")));

        assertEquals(2, result.widgetCount());
        assertTrue(result.customForm().contains("\"type\": \"label\""));
        assertTrue(result.customForm().contains("Hello"));
        assertTrue(result.customForm().contains("\"type\": \"custom_form\""));
    }

    @Test
    void reportsImagesAsUnsupported() {
        UiNode.ImageNode image = new UiNode.ImageNode("img", new ElementBounds(0, 0, 10, 10),
                Anchor.TOP_LEFT, Style.defaults(), true, "minecraft:textures/x.png", "", true);

        BedrockExporter.ExportResult result = new BedrockExporter().export(page(image));

        assertEquals(1, result.unsupportedNodes().size());
        assertTrue(result.unsupportedNodes().get(0).startsWith("img"));
    }

    @Test
    void simpleFormHasButtons() {
        BedrockExporter.ExportResult result = new BedrockExporter().export(page(text("a", "Hi")));

        assertTrue(result.simpleForm().contains("\"type\": \"simple_form\""));
        assertTrue(result.simpleForm().contains("Close"));
        assertFalse(result.simpleForm().contains("custom_form"));
    }

    @Test
    void skipsInvisibleNodes() {
        UiNode.TextNode hidden = new UiNode.TextNode("h", new ElementBounds(0, 0, 10, 10),
                Anchor.TOP_LEFT, Style.defaults(), false, "secret", 1f, 100);

        BedrockExporter.ExportResult result = new BedrockExporter().export(page(hidden));

        assertEquals(0, result.widgetCount());
        assertFalse(result.customForm().contains("secret"));
    }
}
