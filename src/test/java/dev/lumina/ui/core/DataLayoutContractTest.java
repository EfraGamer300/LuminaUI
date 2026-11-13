package dev.lumina.ui.core;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Guards the on-disk layout. Operators (and migration guides) depend on these exact paths, so a
 * rename must fail the build rather than surprise someone after an update.
 */
class DataLayoutContractTest {

    private static final List<String> REQUIRED_DIRECTORIES = List.of(
            "contents",
            "contents/pages",
            "contents/components",
            "contents/effects",
            "contents/images",
            "contents/scripts",
            "contents/world",
            "editor-defaults",
            "editor-defaults/pages",
            "editor-defaults/components",
            "resourcepack",
            "bedrock",
            "bedrock/generated",
            "bedrock/pack",
            "guis",
            "player_data",
            "userdata",
            "cache",
            "backups");

    @Test
    void pluginDeclaresEveryExpectedDirectory() {
        for (String directory : REQUIRED_DIRECTORIES) {
            assertTrue(LuminaLayout.DATA_DIRECTORIES.contains(directory),
                    "missing data directory in layout: " + directory);
        }
    }

    @Test
    void bundledResourcesUseTheSameLayout() throws IOException {
        assertResourceExists("config.yml");
        assertResourceExists("plugin.yml");
        assertResourceExists("contents/pages/example.yml");
        assertResourceExists("locales/en.yml");
        assertResourceExists("locales/pt_BR.yml");
    }

    @Test
    void pagesLiveUnderContents() {
        assertTrue(LuminaLayout.pages().equals("contents/pages"));
        assertTrue(LuminaLayout.langFile().equals("lang.yml"));
        assertTrue(LuminaLayout.components().equals("contents/components"));
    }

    private static void assertResourceExists(String path) throws IOException {
        try (InputStream stream = DataLayoutContractTest.class.getClassLoader().getResourceAsStream(path)) {
            assertTrue(stream != null, "missing bundled resource: " + path);
        }
    }
}
