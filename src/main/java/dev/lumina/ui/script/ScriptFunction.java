package dev.lumina.ui.script;

import java.util.List;

/**
 * A {@code function} declaration: parameters plus the statements to run.
 */
public record ScriptFunction(String name, List<String> parameters, List<Stmt> body, int line) {

    public ScriptFunction {
        parameters = List.copyOf(parameters);
        body = List.copyOf(body);
    }
}
