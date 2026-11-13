package dev.lumina.ui.script;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Exercises the interpreter against a recording host, so language semantics are covered without a
 * running server.
 */
class InterpreterTest {

    /** Minimal host that records every effect for assertions. */
    private static final class RecordingHost implements ScriptHost {
        private final List<String> events = new ArrayList<>();
        private final Map<String, String> placeholders = new HashMap<>();
        private final List<Runnable> scheduled = new ArrayList<>();
        private boolean cancelled;

        @Override
        public String placeholder(String name) {
            return placeholders.getOrDefault(name, "%" + name + "%");
        }

        @Override
        public void message(String text) {
            events.add("message:" + text);
        }

        @Override
        public void actionBar(String text) {
            events.add("actionbar:" + text);
        }

        @Override
        public void title(String text) {
            events.add("title:" + text);
        }

        @Override
        public void command(String line) {
            events.add("command:" + line);
        }

        @Override
        public void openPage(String pageId) {
            events.add("open:" + pageId);
        }

        @Override
        public void closePage() {
            events.add("close");
        }

        @Override
        public void sound(String key) {
            events.add("sound:" + key);
        }

        @Override
        public void log(String text) {
            events.add("log:" + text);
        }

        @Override
        public void cancelEvent(boolean value) {
            cancelled = value;
            events.add("cancel:" + value);
        }

        @Override
        public void after(long ticks, Runnable continuation) {
            events.add("after:" + ticks);
            scheduled.add(continuation);
        }

        @Override
        public void unsupported(String verb, String argument) {
            events.add("unsupported:" + verb);
        }
    }

    private record Fixture(ScriptProgram program, Interpreter interpreter, RecordingHost host) {
    }

    private static Fixture run(String source) {
        return run(source, "test.uui");
    }

    private static Fixture run(String source, String name) {
        ScriptProgram program = Parser.parse(source, name);
        RecordingHost host = new RecordingHost();
        Interpreter interpreter = new Interpreter(program, host);
        interpreter.bootstrap();
        return new Fixture(program, interpreter, host);
    }

    private static void runFirst(Fixture fixture) {
        fixture.interpreter.run(fixture.program.handlers().get(0));
    }

    @Test
    void runsMessageAction() {
        Fixture fixture = run("""
                on open:
                    message "hello"
                """);

        runFirst(fixture);

        assertEquals(List.of("message:hello"), fixture.host.events);
    }

    @Test
    void resolvesPlaceholdersFromHost() {
        Fixture fixture = run("""
                on open:
                    message "hi %player_name%"
                """);
        fixture.host.placeholders.put("player_name", "Efra");

        runFirst(fixture);

        assertEquals(List.of("message:hi Efra"), fixture.host.events);
    }

    @Test
    void interpolatesExpressions() {
        Fixture fixture = run("""
                let price = 10
                let qty = 3

                on open:
                    message "total: ${price * qty}"
                """);

        runFirst(fixture);

        assertEquals(List.of("message:total: 30"), fixture.host.events);
    }

    @Test
    void evaluatesConditionals() {
        Fixture fixture = run("""
                let coins = 5

                on open:
                    if coins > 10:
                        message "rich"
                    elif coins > 0:
                        message "some"
                    else:
                        message "none"
                """);

        runFirst(fixture);

        assertEquals(List.of("message:some"), fixture.host.events);
    }

    @Test
    void loopsOverLists() {
        Fixture fixture = run("""
                on open:
                    for name in ["a", "b", "c"]:
                        message "${name}"
                """);

        runFirst(fixture);

        assertEquals(List.of("message:a", "message:b", "message:c"), fixture.host.events);
    }

    @Test
    void repeatsFixedNumberOfTimes() {
        Fixture fixture = run("""
                let counter = 0

                on open:
                    repeat 4 times:
                        set counter = counter + 1
                    message "done ${counter}"
                """);

        runFirst(fixture);

        assertEquals(List.of("message:done 4"), fixture.host.events);
    }

    @Test
    void breaksOutOfLoops() {
        Fixture fixture = run("""
                on open:
                    for name in ["a", "b", "c"]:
                        if name == "b":
                            break
                        message "${name}"
                """);

        runFirst(fixture);

        assertEquals(List.of("message:a"), fixture.host.events);
    }

    @Test
    void continuesToNextIteration() {
        Fixture fixture = run("""
                on open:
                    for name in ["a", "b", "c"]:
                        if name == "b":
                            continue
                        message "${name}"
                """);

        runFirst(fixture);

        assertEquals(List.of("message:a", "message:c"), fixture.host.events);
    }

    @Test
    void compoundAssignmentAccumulates() {
        Fixture fixture = run("""
                let total = 1

                on open:
                    set total += 2
                    set total *= 3
                    message "${total}"
                """);

        runFirst(fixture);

        assertEquals(List.of("message:9"), fixture.host.events);
    }

    @Test
    void callsFunctionsWithArguments() {
        Fixture fixture = run("""
                function greet(name):
                    return "hi ${name}"

                on open:
                    message greet("Efra")
                """);

        runFirst(fixture);

        assertEquals(List.of("message:hi Efra"), fixture.host.events);
    }

    @Test
    void functionScopesDoNotLeak() {
        Fixture fixture = run("""
                function inner():
                    let secret = "x"
                    return secret

                on open:
                    inner()
                    message "[${secret}]"
                """);

        runFirst(fixture);

        assertEquals(List.of("message:[]"), fixture.host.events);
    }

    @Test
    void waitSchedulesRemainderAndResumes() {
        Fixture fixture = run("""
                on open:
                    message "before"
                    wait 2 seconds
                    message "after"
                """);

        runFirst(fixture);

        assertEquals(List.of("message:before", "after:40"), fixture.host.events);

        fixture.host.scheduled.forEach(Runnable::run);

        assertEquals(List.of("message:before", "after:40", "message:after"), fixture.host.events);
    }

    @Test
    void dispatchesVerbsAndModifiers() {
        Fixture fixture = run("""
                on open:
                    message actionbar "in bar"
                    title "big"
                    command "/spawn"
                    open "shop"
                    close
                    sound "ui.click"
                    cancel
                """);

        runFirst(fixture);

        assertEquals(List.of(
                "actionbar:in bar",
                "title:big",
                "command:/spawn",
                "open:shop",
                "close",
                "sound:ui.click",
                "cancel:true"), fixture.host.events);
    }

    @Test
    void reportsUnknownVerbWithoutFailing() {
        Fixture fixture = run("""
                on open:
                    animate "spin"
                """);

        runFirst(fixture);

        assertEquals(List.of("unsupported:animate"), fixture.host.events);
    }

    @Test
    void comparisonAndLogicOperators() {
        Fixture fixture = run("""
                let a = 5

                on open:
                    if a >= 5 and a <= 5 and not a == 4:
                        message "ok"
                    if "x" in "xyz":
                        message "contains"
                """);

        runFirst(fixture);

        assertEquals(List.of("message:ok", "message:contains"), fixture.host.events);
    }

    @Test
    void terenarySelectsBranch() {
        Fixture fixture = run("""
                let n = 2

                on open:
                    message n > 1 ? "many" : "one"
                """);

        runFirst(fixture);

        assertEquals(List.of("message:many"), fixture.host.events);
    }

    @Test
    void mapAndIndexAccess() {
        Fixture fixture = run("""
                let data = {"name": "Efra", "coins": 7}
                let list = ["a", "b"]

                on open:
                    message data.name
                    message "${list[1]}"
                """);

        runFirst(fixture);

        assertEquals(List.of("message:Efra", "message:b"), fixture.host.events);
    }

    @Test
    void infiniteLoopIsStopped() {
        Fixture fixture = run("""
                on open:
                    while true:
                        message "spin"
                """);

        ScriptException failure = assertThrows(ScriptException.class, () -> runFirst(fixture));
        assertTrue(failure.getMessage().contains("ran too long"));
    }

    @Test
    void unknownFunctionIsReported() {
        Fixture fixture = run("""
                on open:
                    message missing()
                """);

        ScriptException failure = assertThrows(ScriptException.class, () -> runFirst(fixture));
        assertTrue(failure.getMessage().contains("unknown function"));
    }

    @Test
    void handlerMatchingHonoursPageAndElement() {
        ScriptProgram program = Parser.parse("""
                on click "buy" in "shop":
                    message "x"
                """, "test.uui");

        ScriptHandler handler = program.handlers().get(0);

        assertTrue(handler.matches("shop", "buy"));
        assertFalse(handler.matches("other", "buy"));
        assertFalse(handler.matches("shop", "other"));
    }

    @Test
    void globalsAreEvaluatedOnBootstrap() {
        Fixture fixture = run("""
                let doubled = 21 * 2

                on open:
                    message "${doubled}"
                """);

        assertEquals(42, fixture.interpreter.globals().get("doubled"));

        runFirst(fixture);

        assertEquals(List.of("message:42"), fixture.host.events);
    }
}
