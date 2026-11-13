package dev.lumina.ui.script;

import java.util.List;

/**
 * Resolves which {@link ScriptFormat} handles a file.
 */
public final class ScriptFormats {

    private final List<ScriptFormat> formats;

    public ScriptFormats() {
        this(List.of(new UuiScriptFormat()));
    }

    public ScriptFormats(List<ScriptFormat> formats) {
        this.formats = List.copyOf(formats);
    }

    /** Returns the first format claiming the file, or {@code null} when none does. */
    public ScriptFormat forFile(java.nio.file.Path file) {
        for (ScriptFormat format : formats) {
            if (format.supports(file)) {
                return format;
            }
        }
        return null;
    }

    public List<ScriptFormat> all() {
        return formats;
    }
}
