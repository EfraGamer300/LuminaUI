package dev.lumina.ui.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.lumina.ui.api.Anchor;
import dev.lumina.ui.api.BillboardMode;
import dev.lumina.ui.api.ElementBounds;
import dev.lumina.ui.api.Style;
import dev.lumina.ui.api.UiNode;
import dev.lumina.ui.api.UiPage;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PageCodecTest {

    private final PageCodec codec = new PageCodec();

    private static UiPage samplePage() {
        UiNode.TextNode heading = new UiNode.TextNode("heading",
                new ElementBounds(10f, 12f, 200f, 24f), Anchor.TOP_LEFT,
                Style.builder().accent(0xFF00FF00).opacity(0.8f).billboard(BillboardMode.VERTICAL).build(),
                true, "<white>Hello", 0.5f, 180);
        UiNode.ShapeNode panel = new UiNode.ShapeNode("panel",
                new ElementBounds(0f, 0f, 240f, 100f), Anchor.CENTER,
                Style.defaults(), true, "minecraft:black_concrete", 6f);
        return new UiPage("test", "Test Page", 1, 1.5f, true,
                List.of(new UiPage.Layer("main", 1f, true, List.of(panel, heading))));
    }

    @Test
    void roundTripsPageThroughMap() {
        UiPage original = samplePage();

        UiPage decoded = codec.decode("test", codec.encode(original));

        assertEquals(original.id(), decoded.id());
        assertEquals(original.title(), decoded.title());
        assertEquals(original.scale(), decoded.scale());
        assertEquals(original.persistent(), decoded.persistent());
        assertEquals(original.nodeCount(), decoded.nodeCount());
        assertEquals(original.layers().size(), decoded.layers().size());
    }

    @Test
    void preservesNodeDetails() {
        UiPage decoded = codec.decode("test", codec.encode(samplePage()));

        UiNode.TextNode heading = (UiNode.TextNode) decoded.layers().get(0).nodes().get(1);

        assertEquals("heading", heading.id());
        assertEquals("<white>Hello", heading.text());
        assertEquals(0.5f, heading.scale());
        assertEquals(180, heading.lineWidth());
        assertEquals(Anchor.TOP_LEFT, heading.anchor());
        assertEquals(0xFF00FF00, heading.style().accent());
        assertEquals(0.8f, heading.style().opacity());
        assertEquals(BillboardMode.VERTICAL, heading.style().billboard());
    }

    @Test
    void preservesGroupChildren() {
        UiNode child = new UiNode.TextNode("child", new ElementBounds(1f, 1f, 10f, 10f),
                Anchor.TOP_LEFT, Style.defaults(), true, "c", 1f, 50);
        UiNode.GroupNode group = new UiNode.GroupNode("group",
                new ElementBounds(0f, 0f, 50f, 50f), Anchor.TOP_LEFT, Style.defaults(), true, List.of(child));
        UiPage page = new UiPage("g", "G", 1, 1f, false,
                List.of(new UiPage.Layer("l", 0f, true, List.of(group))));

        UiPage decoded = codec.decode("g", codec.encode(page));

        UiNode.GroupNode decodedGroup = (UiNode.GroupNode) decoded.layers().get(0).nodes().get(0);
        assertEquals(1, decodedGroup.children().size());
        assertEquals("child", decodedGroup.children().get(0).id());
    }

    @Test
    void rejectsUnknownNodeType() {
        Map<String, Object> root = new java.util.LinkedHashMap<>();
        root.put("schema-version", 1);
        root.put("title", "Broken");
        root.put("layers", List.of(Map.of(
                "id", "l",
                "nodes", List.of(Map.of(
                        "id", "x",
                        "type", "hologram",
                        "bounds", Map.of("x", 0, "y", 0, "width", 1, "height", 1))))));

        assertThrows(PageCodec.PageFormatException.class, () -> codec.decode("broken", root));
    }

    @Test
    void survivesYamlRoundTrip() {
        SimpleYaml yaml = new SimpleYaml();
        UiPage original = samplePage();

        String text = yaml.write(codec.encode(original));
        Map<String, Object> parsed = yaml.read(text);
        UiPage decoded = codec.decode("test", parsed);

        assertNotNull(decoded);
        assertEquals(original.nodeCount(), decoded.nodeCount());
        assertEquals("<white>Hello",
                ((UiNode.TextNode) decoded.layers().get(0).nodes().get(1)).text());
        assertTrue(text.contains("heading"));
    }
}
