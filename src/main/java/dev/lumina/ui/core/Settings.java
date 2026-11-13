package dev.lumina.ui.core;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * Typed snapshot of {@code config.yml}. Read once on load/reload so the hot path never touches
 * {@link FileConfiguration}.
 */
public final class Settings {

    private final float viewportWidth;
    private final float viewportHeight;
    private final float pixelsPerBlock;
    private final int maxNodesPerPage;
    private final int renderIntervalTicks;
    private final boolean editorAutosave;
    private final int editorAutosaveSeconds;
    private final int editorUndoLimit;
    private final boolean editorSounds;
    private final float editorSoundVolume;
    private final boolean bedrockEnabled;
    private final boolean bedrockAutoInstall;
    private final String geyserPacksFolder;
    private final String bedrockPackName;
    private final int bedrockCanvasWidth;
    private final int bedrockCanvasHeight;
    private final String headUrlTemplate;
    private final String skinUrlTemplate;
    private final boolean packApplyOnJoin;
    private final boolean debugPerf;

    private Settings(Builder builder) {
        this.viewportWidth = builder.viewportWidth;
        this.viewportHeight = builder.viewportHeight;
        this.pixelsPerBlock = builder.pixelsPerBlock;
        this.maxNodesPerPage = builder.maxNodesPerPage;
        this.renderIntervalTicks = builder.renderIntervalTicks;
        this.editorAutosave = builder.editorAutosave;
        this.editorAutosaveSeconds = builder.editorAutosaveSeconds;
        this.editorUndoLimit = builder.editorUndoLimit;
        this.editorSounds = builder.editorSounds;
        this.editorSoundVolume = builder.editorSoundVolume;
        this.bedrockEnabled = builder.bedrockEnabled;
        this.bedrockAutoInstall = builder.bedrockAutoInstall;
        this.geyserPacksFolder = builder.geyserPacksFolder;
        this.bedrockPackName = builder.bedrockPackName;
        this.bedrockCanvasWidth = builder.bedrockCanvasWidth;
        this.bedrockCanvasHeight = builder.bedrockCanvasHeight;
        this.headUrlTemplate = builder.headUrlTemplate;
        this.skinUrlTemplate = builder.skinUrlTemplate;
        this.packApplyOnJoin = builder.packApplyOnJoin;
        this.debugPerf = builder.debugPerf;
    }

    public static Settings defaults() {
        return builder().build();
    }

    public static Settings from(FileConfiguration config) {
        Builder builder = builder();
        builder.viewportWidth = (float) config.getDouble("ui.viewport-width", builder.viewportWidth);
        builder.viewportHeight = (float) config.getDouble("ui.viewport-height", builder.viewportHeight);
        builder.pixelsPerBlock = (float) config.getDouble("ui.pixels-per-block", builder.pixelsPerBlock);
        builder.maxNodesPerPage = config.getInt("ui.max-nodes-per-page", builder.maxNodesPerPage);
        builder.renderIntervalTicks = config.getInt("ui.render-interval-ticks", builder.renderIntervalTicks);

        builder.editorAutosave = config.getBoolean("editor.autosave.enabled", builder.editorAutosave);
        builder.editorAutosaveSeconds =
                config.getInt("editor.autosave.interval-seconds", builder.editorAutosaveSeconds);
        builder.editorUndoLimit = config.getInt("editor.history.undo-limit", builder.editorUndoLimit);
        builder.editorSounds = config.getBoolean("editor.sounds.enabled", builder.editorSounds);
        builder.editorSoundVolume =
                (float) config.getDouble("editor.sounds.volume", builder.editorSoundVolume);

        builder.bedrockEnabled = config.getBoolean("bedrock.enabled", builder.bedrockEnabled);
        builder.bedrockAutoInstall = config.getBoolean("bedrock.auto-install-to-geyser", builder.bedrockAutoInstall);
        builder.geyserPacksFolder = config.getString("bedrock.geyser-packs-folder", builder.geyserPacksFolder);
        builder.bedrockPackName = config.getString("bedrock.pack.name", builder.bedrockPackName);
        builder.bedrockCanvasWidth = config.getInt("bedrock.canvas.width", builder.bedrockCanvasWidth);
        builder.bedrockCanvasHeight = config.getInt("bedrock.canvas.height", builder.bedrockCanvasHeight);
        builder.headUrlTemplate =
                config.getString("bedrock.player-head.url-template", builder.headUrlTemplate);
        builder.skinUrlTemplate =
                config.getString("bedrock.player-skin.url-template", builder.skinUrlTemplate);

        builder.packApplyOnJoin = config.getBoolean("resource-pack.apply-on-join", builder.packApplyOnJoin);
        builder.debugPerf = config.getBoolean("debug.perf", builder.debugPerf);
        return builder.build();
    }

    public float viewportWidth() {
        return viewportWidth;
    }

    public float viewportHeight() {
        return viewportHeight;
    }

    public float pixelsPerBlock() {
        return pixelsPerBlock;
    }

    public int maxNodesPerPage() {
        return maxNodesPerPage;
    }

    public int renderIntervalTicks() {
        return renderIntervalTicks;
    }

    public boolean editorAutosave() {
        return editorAutosave;
    }

    public int editorAutosaveSeconds() {
        return editorAutosaveSeconds;
    }

    public int editorUndoLimit() {
        return editorUndoLimit;
    }

    public boolean editorSounds() {
        return editorSounds;
    }

    public float editorSoundVolume() {
        return editorSoundVolume;
    }

    public boolean bedrockEnabled() {
        return bedrockEnabled;
    }

    public boolean bedrockAutoInstall() {
        return bedrockAutoInstall;
    }

    public String geyserPacksFolder() {
        return geyserPacksFolder;
    }

    public String bedrockPackName() {
        return bedrockPackName;
    }

    public int bedrockCanvasWidth() {
        return bedrockCanvasWidth;
    }

    public int bedrockCanvasHeight() {
        return bedrockCanvasHeight;
    }

    public String headUrlTemplate() {
        return headUrlTemplate;
    }

    public String skinUrlTemplate() {
        return skinUrlTemplate;
    }

    public boolean packApplyOnJoin() {
        return packApplyOnJoin;
    }

    public boolean debugPerf() {
        return debugPerf;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private float viewportWidth = 427f;
        private float viewportHeight = 240f;
        private float pixelsPerBlock = 100f;
        private int maxNodesPerPage = 500;
        private int renderIntervalTicks = 2;
        private boolean editorAutosave = true;
        private int editorAutosaveSeconds = 600;
        private int editorUndoLimit = 50;
        private boolean editorSounds = true;
        private float editorSoundVolume = 1f;
        private boolean bedrockEnabled = true;
        private boolean bedrockAutoInstall = true;
        private String geyserPacksFolder = "";
        private String bedrockPackName = "LuminaUI";
        private int bedrockCanvasWidth = 1920;
        private int bedrockCanvasHeight = 1080;
        private String headUrlTemplate = "https://mc-heads.net/avatar/{id}/128";
        private String skinUrlTemplate = "https://mc-heads.net/body/{id}/256";
        private boolean packApplyOnJoin = true;
        private boolean debugPerf = false;

        public Settings build() {
            return new Settings(this);
        }
    }
}
