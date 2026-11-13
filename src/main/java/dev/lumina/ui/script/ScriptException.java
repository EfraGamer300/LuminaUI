package dev.lumina.ui.script;

/**
 * Error raised while reading or running a script, carrying the source line for operator feedback.
 */
public class ScriptException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String source;
    private final int line;

    public ScriptException(String source, int line, String message) {
        super(format(source, line, message));
        this.source = source;
        this.line = line;
    }

    public ScriptException(String source, int line, String message, Throwable cause) {
        super(format(source, line, message), cause);
        this.source = source;
        this.line = line;
    }

    public String source() {
        return source;
    }

    public int line() {
        return line;
    }

    private static String format(String source, int line, String message) {
        String where = source == null ? "<script>" : source;
        return line > 0 ? where + ":" + line + ": " + message : where + ": " + message;
    }
}
