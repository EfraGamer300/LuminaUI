package dev.lumina.ui.script;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Result of parsing a script file: declarations plus every handler it registered.
 */
public record ScriptProgram(
        String sourceName,
        String pageId,
        Map<String, Expr> globals,
        Map<String, ScriptFunction> functions,
        List<ScriptHandler> handlers) {

    public ScriptProgram {
        globals = Map.copyOf(globals);
        functions = Map.copyOf(functions);
        handlers = List.copyOf(handlers);
    }

    public static ScriptProgram empty(String sourceName) {
        return new ScriptProgram(sourceName, null, new LinkedHashMap<>(), new LinkedHashMap<>(), List.of());
    }

    public boolean hasHandlers() {
        return !handlers.isEmpty();
    }
}
