package dev.lumina.ui.bedrock;

import dev.lumina.ui.LuminaPlugin;
import dev.lumina.ui.api.UiPage;
import dev.lumina.ui.core.Settings;
import dev.lumina.ui.core.UiRegistry;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Coordinates Bedrock exports: builds form JSON for a page and, when configured, deploys it to the
 * Geyser packs folder.
 */
public final class BedrockService {

    private final LuminaPlugin plugin;
    private final UiRegistry registry;
    private final Settings settings;
    private final BedrockExporter exporter = new BedrockExporter();

    public BedrockService(LuminaPlugin plugin, UiRegistry registry, Settings settings) {
        this.plugin = plugin;
        this.registry = registry;
        this.settings = settings;
    }

    public boolean enabled() {
        return settings.bedrockEnabled();
    }

    /** Exports one page and returns the generated custom form JSON. */
    public BedrockExporter.ExportResult export(String pageId) {
        UiPage page = registry.page(pageId);
        if (page == null) {
            throw new IllegalArgumentException("unknown page: " + pageId);
        }
        return exporter.export(page);
    }

    /** Exports every registered page and returns the files written. */
    public List<Path> exportAll() throws IOException {
        GeyserPackBridge bridge = bridge();
        List<Path> written = new ArrayList<>();
        Path localOutput = plugin.getDataFolder().toPath()
                .resolve(dev.lumina.ui.core.LuminaLayout.BEDROCK_GENERATED);
        for (String pageId : registry.pageIds()) {
            BedrockExporter.ExportResult result = export(pageId);
            written.add(writeLocal(localOutput, pageId, result.customForm()));
            if (bridge.isConfigured()) {
                written.add(bridge.writeForm(pageId, result.customForm()));
            }
        }
        return written;
    }

    /**
     * Always keeps a copy of the generated form inside the plugin folder, so operators can inspect
     * or hand-deploy the output even when no Geyser packs folder is configured.
     */
    private Path writeLocal(Path directory, String pageId, String json) throws IOException {
        java.nio.file.Files.createDirectories(directory);
        Path target = directory.resolve(pageId + ".json");
        java.nio.file.Files.writeString(target, json);
        return target;
    }

    public GeyserPackBridge bridge() {
        String folder = settings.geyserPacksFolder();
        Path path = (folder == null || folder.isBlank())
                ? null
                : plugin.getServer().getWorldContainer().toPath().resolve(folder);
        return new GeyserPackBridge(path);
    }

    /** Short status text for {@code /lumina bedrock status}. */
    public String status() {
        GeyserPackBridge bridge = bridge();
        return "enabled=" + enabled()
                + ", geyser=" + bridge.isGeyserPresent()
                + ", packs-folder=" + (bridge.isConfigured() ? bridge.packsFolder() : "unset");
    }
}
