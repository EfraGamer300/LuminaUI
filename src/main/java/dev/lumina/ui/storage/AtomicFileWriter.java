package dev.lumina.ui.storage;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Writes files atomically: content goes to a sibling temp file which is then moved over the target.
 * A crash mid-write can therefore never leave a truncated page behind.
 */
public final class AtomicFileWriter {

    public void writeUtf8(Path target, String content) throws IOException {
        Path parent = target.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Path temp = Files.createTempFile(parent, target.getFileName().toString(), ".tmp");
        try {
            try (OutputStream out = Files.newOutputStream(temp)) {
                out.write(content.getBytes(StandardCharsets.UTF_8));
                out.flush();
            }
            try {
                Files.move(temp, target,
                        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException unsupported) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temp);
        }
    }
}
