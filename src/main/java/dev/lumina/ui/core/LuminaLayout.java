package dev.lumina.ui.core;

import java.util.List;

/**
 * Canonical data-folder layout.
 *
 * <p>Every path the plugin creates lives here as a plain string constant, free of Bukkit types, so
 * the layout can be asserted by unit tests and referenced by any layer without pulling in the
 * server API. The structure is intentionally familiar to operators migrating from existing setups:
 * authored content under {@code contents/}, templates under {@code editor-defaults/}, generated pack
 * output under {@code resourcepack/} and {@code bedrock/}.
 */
public final class LuminaLayout {

    public static final String CONTENTS = "contents";
    public static final String PAGES = CONTENTS + "/pages";
    public static final String COMPONENTS = CONTENTS + "/components";
    public static final String EFFECTS = CONTENTS + "/effects";
    public static final String IMAGES = CONTENTS + "/images";
    public static final String SCRIPTS = CONTENTS + "/scripts";
    public static final String WORLD = CONTENTS + "/world";

    public static final String EDITOR_DEFAULTS = "editor-defaults";
    public static final String EDITOR_DEFAULT_PAGES = EDITOR_DEFAULTS + "/pages";
    public static final String EDITOR_DEFAULT_COMPONENTS = EDITOR_DEFAULTS + "/components";

    public static final String RESOURCE_PACK = "resourcepack";
    public static final String BEDROCK = "bedrock";
    public static final String BEDROCK_GENERATED = BEDROCK + "/generated";
    public static final String BEDROCK_PACK = BEDROCK + "/pack";

    public static final String GUIS = "guis";
    public static final String PLAYER_DATA = "player_data";
    public static final String USER_DATA = "userdata";
    public static final String CACHE = "cache";
    public static final String BACKUPS = "backups";

    /** Language file: one editable file, not a directory of locales. */
    public static final String LANG_FILE = "lang.yml";
    public static final String CONFIG_FILE = "config.yml";

    /** Every directory created on first run, parents before children. */
    public static final List<String> DATA_DIRECTORIES = List.of(
            CONTENTS,
            PAGES,
            COMPONENTS,
            EFFECTS,
            IMAGES,
            SCRIPTS,
            WORLD,
            EDITOR_DEFAULTS,
            EDITOR_DEFAULT_PAGES,
            EDITOR_DEFAULT_COMPONENTS,
            RESOURCE_PACK,
            BEDROCK,
            BEDROCK_GENERATED,
            BEDROCK_PACK,
            GUIS,
            PLAYER_DATA,
            USER_DATA,
            CACHE,
            BACKUPS);

    private LuminaLayout() {
    }

    public static String pages() {
        return PAGES;
    }

    public static String components() {
        return COMPONENTS;
    }

    public static String langFile() {
        return LANG_FILE;
    }
}
