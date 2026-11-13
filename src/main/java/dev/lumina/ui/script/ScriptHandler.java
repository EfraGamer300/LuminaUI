package dev.lumina.ui.script;

import java.util.List;

/**
 * An event handler declared with {@code on ...:}, {@code every ...:} or {@code command ...:}.
 */
public record ScriptHandler(
        String event,
        String elementId,
        String pageId,
        String commandName,
        String permission,
        long everyTicks,
        List<Stmt> body,
        int line) {

    public ScriptHandler {
        body = List.copyOf(body);
    }

    /** Whether this handler should run for the given page and element. */
    public boolean matches(String targetPage, String targetElement) {
        if (pageId != null && !pageId.equalsIgnoreCase(targetPage)) {
            return false;
        }
        return elementId == null || elementId.equalsIgnoreCase(targetElement);
    }
}
