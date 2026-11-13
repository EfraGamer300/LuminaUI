package dev.lumina.ui.script;

import java.nio.file.Path;
import java.util.Locale;

/**
 * Reader for the legacy {@code .uui} script format.
 *
 * <p>Implemented from the observed language surface (indentation, {@code page}/{@code on}/
 * {@code every}/{@code command}/{@code function} declarations and the statement keywords), so
 * existing script files keep working while the native format is designed.
 */
public final class UuiScriptFormat implements ScriptFormat {

    @Override
    public String name() {
        return "uui";
    }

    @Override
    public boolean supports(Path file) {
        return file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".uui");
    }

    @Override
    public ScriptProgram parse(String source, String sourceName) {
        return Parser.parse(source, sourceName);
    }

    @Override
    public String description() {
        return ".uui (legacy indentation-based scripts)";
    }
}
