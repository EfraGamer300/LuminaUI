package dev.lumina.ui.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.lumina.ui.api.UiNode;
import dev.lumina.ui.api.UiPage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Loads the bundled example page through the real pipeline, so a broken shipped resource fails the
 * build instead of failing on a live server.
 */
class BundledPageTest {

    private static String readResource(String name) throws IOException {
        try (InputStream stream = BundledPageTest.class.getClassLoader().getResourceAsStream(name)) {
            if (stream == null) {
                throw new IOException("missing test resource: " + name);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void examplePageParsesAndHasExpectedStructure() throws IOException {
        String text = readResource("pages/example.yml");
        Map<String, Object> root = new SimpleYaml().read(text);

        UiPage page = new PageCodec().decode("example", root);

        assertEquals("Example HUD", page.title());
        assertEquals(UiPage.CURRENT_SCHEMA_VERSION, page.schemaVersion());
        assertEquals(2, page.layers().size());
        assertEquals(3, page.nodeCount());
        assertTrue(page.layers().get(0).id().equals("background"));
        assertTrue(page.layers().get(1).interactive());
        assertEquals(-1598038790, page.layers().get(0).nodes().get(0).style().accent());
    }

    @Test
    void examplePageSurvivesSaveAndReloadCycle() throws IOException {
        Map<String, Object> root = new SimpleYaml().read(readResource("pages/example.yml"));
        UiPage original = new PageCodec().decode("example", root);

        SimpleYaml yaml = new SimpleYaml();
        PageCodec codec = new PageCodec();
        UiPage reloaded = codec.decode("example", yaml.read(yaml.write(codec.encode(original))));

        assertEquals(original.title(), reloaded.title());
        assertEquals(original.nodeCount(), reloaded.nodeCount());
        assertEquals(original.layers().get(1).nodes().size(), reloaded.layers().get(1).nodes().size());

        UiNode.TextNode heading = (UiNode.TextNode) reloaded.layers().get(1).nodes().get(0);
        assertEquals("heading", heading.id());
        assertEquals("<gradient:#7dd3fc:#a78bfa><bold>LuminaUI</bold></gradient>", heading.text());
        assertEquals(0.5f, heading.scale());
    }
}
