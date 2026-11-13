package dev.lumina.ui.bedrock;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Writes exported forms into the Geyser pack directory so Bedrock clients can open them.
 *
 * <p>The bridge only touches the filesystem: it never reflects into Geyser internals, which keeps it
 * working across Geyser versions and safe to run when Geyser is absent.
 */
public final class GeyserPackBridge {

    private final Path packsFolder;

    public GeyserPackBridge(Path packsFolder) {
        this.packsFolder = packsFolder;
    }

    public boolean isConfigured() {
        return packsFolder != null && Files.isDirectory(packsFolder);
    }

    public boolean isGeyserPresent() {
        try {
            Class.forName("org.geysermc.geyser.GeyserImpl");
            return true;
        } catch (ClassNotFoundException missing) {
            return false;
        }
    }

    /** Writes a form JSON into a versioned folder inside the packs dir, replacing the previous one. */
    public Path writeForm(String pageId, String formJson) throws IOException {
        if (!isConfigured()) {
            throw new IOException("Geyser packs folder is not configured or does not exist: " + packsFolder);
        }
        Path target = packsFolder.resolve("luminaui").resolve(pageId + ".json");
        Files.createDirectories(target.getParent());
        Files.writeString(target, formJson);
        return target;
    }

    public Path packsFolder() {
        return packsFolder;
    }
}
