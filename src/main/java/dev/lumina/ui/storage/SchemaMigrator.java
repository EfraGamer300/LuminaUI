package dev.lumina.ui.storage;

import dev.lumina.ui.api.UiPage;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Brings older stored pages up to {@link UiPage#CURRENT_SCHEMA_VERSION}.
 *
 * <p>Every step is additive and ordered; a file newer than the engine is rejected instead of being
 * silently downgraded, so a newer server never corrupts pages written by an older one.
 */
public final class SchemaMigrator {

    public Map<String, Object> migrate(String pageId, Map<String, Object> root) {
        if (root == null) {
            throw new PageCodec.PageFormatException("page '" + pageId + "' is empty");
        }
        int version = readVersion(root);
        if (version > UiPage.CURRENT_SCHEMA_VERSION) {
            throw new PageCodec.PageFormatException("page '" + pageId + "' uses schema " + version
                    + " but this build supports up to " + UiPage.CURRENT_SCHEMA_VERSION);
        }
        if (version == UiPage.CURRENT_SCHEMA_VERSION) {
            return root;
        }

        Map<String, Object> current = new LinkedHashMap<>(root);
        if (version < 1) {
            // Schema 0 had no version field and no anchor: every node defaulted to top-left.
            current.put("schema-version", 1);
        }
        return current;
    }

    private static int readVersion(Map<String, Object> root) {
        Object raw = root.get("schema-version");
        if (raw instanceof Number number) {
            return number.intValue();
        }
        if (raw instanceof String text) {
            try {
                return Integer.parseInt(text.trim());
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }
}
