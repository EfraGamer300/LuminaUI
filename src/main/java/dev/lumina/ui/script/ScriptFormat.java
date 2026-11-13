package dev.lumina.ui.script;

import java.nio.file.Path;

/**
 * A script file format.
 *
 * <p>Format support is pluggable on purpose: the legacy {@code .uui} format is implemented as one
 * {@code ScriptFormat}, so a future native format can be added (and the legacy one retired) without
 * touching loading, dispatch or the interpreter.
 */
public interface ScriptFormat {

    /** Short identifier used in logs and commands. */
    String name();

    /** Whether this format claims the given file. */
    boolean supports(Path file);

    ScriptProgram parse(String source, String sourceName);

    /** Human-readable description shown by {@code /lumina script formats}. */
    default String description() {
        return name();
    }
}
