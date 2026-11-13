package dev.lumina.ui.script;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * Reads the bundled example through the same path a server uses, so a syntax drift in the shipped
 * script fails the build instead of showing up as a console warning.
 */
class ScriptFormatTest {

    private static String resource(String name) throws IOException {
        try (InputStream stream = ScriptFormatTest.class.getClassLoader().getResourceAsStream(name)) {
            if (stream == null) {
                throw new IOException("missing resource: " + name);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void uuiFormatClaimsOnlyUuiFiles() {
        UuiScriptFormat format = new UuiScriptFormat();

        assertTrue(format.supports(Path.of("shop.uui")));
        assertTrue(format.supports(Path.of("SHOP.UUI")));
        assertTrue(!format.supports(Path.of("shop.yml")));
        assertEquals("uui", format.name());
    }

    @Test
    void registryPicksTheFormatByExtension() {
        ScriptFormats formats = new ScriptFormats();

        assertNotNull(formats.forFile(Path.of("a.uui")));
        assertEquals(null, formats.forFile(Path.of("a.txt")));
    }

    @Test
    void bundledExampleParses() throws IOException {
        String source = resource("contents/scripts/example.uui");

        ScriptProgram program = new UuiScriptFormat().parse(source, "example.uui");

        assertEquals("example", program.pageId());
        assertTrue(program.handlers().size() >= 5);
        assertEquals(1, program.functions().size());
        assertEquals(2, program.globals().size());
        assertTrue(program.handlers().stream().anyMatch(h -> h.event().equals("every")));
        assertTrue(program.handlers().stream().anyMatch(h -> h.event().equals("command")));
    }
}
