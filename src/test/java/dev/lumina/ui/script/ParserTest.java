package dev.lumina.ui.script;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ParserTest {

    private static ScriptProgram parse(String source) {
        return Parser.parse(source, "test.uui");
    }

    @Test
    void parsesPageAndSimpleHandler() {
        ScriptProgram program = parse("""
                page "shop"

                on open:
                    message "Welcome"
                """);

        assertEquals("shop", program.pageId());
        assertEquals(1, program.handlers().size());
        ScriptHandler handler = program.handlers().get(0);
        assertEquals("open", handler.event());
        assertEquals("shop", handler.pageId());
        assertEquals(1, handler.body().size());
        assertTrue(handler.body().get(0) instanceof Stmt.Action);
    }

    @Test
    void keepsBlockNestingFromIndentation() {
        ScriptProgram program = parse("""
                on click "buy":
                    if balance > 10:
                        message "ok"
                    else:
                        message "no"
                """);

        Stmt.If conditional = (Stmt.If) program.handlers().get(0).body().get(0);
        assertEquals(1, conditional.branches().size());
        assertEquals(1, conditional.otherwise().size());
    }

    @Test
    void parsesElifChain() {
        ScriptProgram program = parse("""
                on click:
                    if a == 1:
                        message "one"
                    elif a == 2:
                        message "two"
                    else:
                        message "many"
                """);

        Stmt.If conditional = (Stmt.If) program.handlers().get(0).body().get(0);
        assertEquals(2, conditional.branches().size());
        assertEquals(1, conditional.otherwise().size());
    }

    @Test
    void parsesEveryWithUnits() {
        ScriptProgram program = parse("""
                every 2 seconds in "hud":
                    message "tick"
                """);

        ScriptHandler handler = program.handlers().get(0);
        assertEquals("every", handler.event());
        assertEquals(40L, handler.everyTicks());
        assertEquals("hud", handler.pageId());
    }

    @Test
    void parsesCommandWithPermission() {
        ScriptProgram program = parse("""
                command /shop permission "shop.use":
                    message "opening"
                """);

        ScriptHandler handler = program.handlers().get(0);
        assertEquals("command", handler.event());
        assertEquals("shop", handler.commandName());
        assertEquals("shop.use", handler.permission());
    }

    @Test
    void parsesFunctionAndGlobals() {
        ScriptProgram program = parse("""
                let greeting = "hi"
                let total = 2 + 3 * 4

                function welcome(name):
                    message "${greeting} ${name}"
                """);

        assertEquals(2, program.globals().size());
        assertEquals(1, program.functions().size());
        assertEquals(List.of("name"), program.functions().get("welcome").parameters());
    }

    @Test
    void buildsInterpolatedStrings() {
        ScriptProgram program = parse("""
                on open:
                    message "Hello ${player}!"
                """);

        Stmt.Action action = (Stmt.Action) program.handlers().get(0).body().get(0);
        assertTrue(action.argument() instanceof Expr.Interpolated);
        Expr.Interpolated interpolated = (Expr.Interpolated) action.argument();
        assertEquals(3, interpolated.parts().size());
        assertTrue(interpolated.parts().get(1) instanceof Expr.Variable);
    }

    @Test
    void parsesPlaceholderToken() {
        ScriptProgram program = parse("""
                on open:
                    message "%player_name%"
                """);

        Stmt.Action action = (Stmt.Action) program.handlers().get(0).body().get(0);
        assertTrue(action.argument() instanceof Expr.Placeholder);
    }

    @Test
    void parsesLoops() {
        ScriptProgram program = parse("""
                on open:
                    for item in ["a", "b"]:
                        message "${item}"
                    repeat 3 times:
                        message "again"
                    while counter < 3:
                        set counter = counter + 1
                """);

        List<Stmt> body = program.handlers().get(0).body();
        assertTrue(body.get(0) instanceof Stmt.ForEach);
        assertTrue(body.get(1) instanceof Stmt.Repeat);
        assertTrue(body.get(2) instanceof Stmt.While);
    }

    @Test
    void parsesControlFlowKeywords() {
        ScriptProgram program = parse("""
                on open:
                    wait 5 ticks
                    break
                    continue
                    return 1
                """);

        List<Stmt> body = program.handlers().get(0).body();
        assertTrue(body.get(0) instanceof Stmt.Wait);
        assertTrue(body.get(1) instanceof Stmt.Break);
        assertTrue(body.get(2) instanceof Stmt.Continue);
        assertTrue(body.get(3) instanceof Stmt.Return);
    }

    @Test
    void ignoresCommentsAndBlankLines() {
        ScriptProgram program = parse("""
                # comment at top

                on open:   # trailing comment
                    message "hi"  # another

                # trailing comment
                """);

        assertEquals(1, program.handlers().size());
        assertEquals(1, program.handlers().get(0).body().size());
    }

    @Test
    void rejectsBadIndentationWithLineNumber() {
        ScriptException failure = assertThrows(ScriptException.class, () -> parse("""
                on open:
                    message "a"
                      message "b"
                """));

        assertEquals(3, failure.line());
        assertTrue(failure.getMessage().contains("test.uui:3"));
    }

    @Test
    void rejectsUnknownDeclaration() {
        ScriptException failure = assertThrows(ScriptException.class,
                () -> parse("whenever open:\n    message \"x\"\n"));

        assertTrue(failure.getMessage().contains("unknown declaration"));
    }

    @Test
    void rejectsMissingColon() {
        ScriptException failure = assertThrows(ScriptException.class, () -> parse("""
                on open
                    message "x"
                """));

        assertTrue(failure.getMessage().contains("':'"));
    }

    @Test
    void rejectsUnterminatedString() {
        ScriptException failure = assertThrows(ScriptException.class, () -> parse("""
                on open:
                    message "unclosed
                """));

        assertTrue(failure.getMessage().contains("unterminated string"));
    }

    @Test
    void emptyScriptIsValid() {
        ScriptProgram program = parse("# only a comment\n");

        assertFalse(program.hasHandlers());
        assertTrue(program.globals().isEmpty());
    }
}
