package dev.lumina.ui.script;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Evaluates a parsed {@link ScriptProgram} against a {@link ScriptHost}.
 *
 * <p>Statements are interpreted directly; there is no bytecode stage. That keeps the runtime small
 * and the error messages close to the source line, at the cost of re-walking the tree per event -
 * which is fine for UI callbacks.
 */
public final class Interpreter {

    private static final int MAX_STEPS = 100_000;

    private final ScriptProgram program;
    private final ScriptHost host;
    private final Map<String, Object> globals = new LinkedHashMap<>();
    private Scope globalScope = new Scope(null);

    private int steps;

    public Interpreter(ScriptProgram program, ScriptHost host) {
        this.program = program;
        this.host = host;
    }

    /**
     * Initialises globals. Declarations can reference earlier ones, so they are evaluated in order
     * into a scope that every handler inherits.
     */
    public void bootstrap() {
        globals.clear();
        globalScope = new Scope(null);
        for (Map.Entry<String, Expr> entry : program.globals().entrySet()) {
            Object value = evaluate(entry.getValue(), globalScope);
            globals.put(entry.getKey(), value);
            globalScope.set(entry.getKey(), value);
        }
    }

    /** Runs one handler body. */
    public void run(ScriptHandler handler) {
        steps = 0;
        Scope scope = new Scope(globalScope);
        try {
            execute(handler.body(), scope);
        } catch (BreakSignal | ContinueSignal ignored) {
            // A stray break/continue outside a loop simply ends the handler.
        } catch (ReturnSignal ignored) {
            // Returning from a handler is a normal early exit.
        }
    }

    /** Runs a named function with the given arguments; used by commands and tests. */
    public Object callFunction(String name, List<Object> arguments) {
        ScriptFunction function = program.functions().get(name);
        if (function == null) {
            throw new ScriptException(program.sourceName(), 0, "unknown function '" + name + "'");
        }
        return invoke(function, arguments);
    }

    public Map<String, Object> globals() {
        return Map.copyOf(globals);
    }

    private void execute(List<Stmt> body, Scope scope) {
        for (int index = 0; index < body.size(); index++) {
            Stmt statement = body.get(index);
            if (statement instanceof Stmt.Wait wait) {
                long ticks = toTicks(wait);
                List<Stmt> remaining = body.subList(index + 1, body.size());
                if (remaining.isEmpty()) {
                    return;
                }
                List<Stmt> copy = List.copyOf(remaining);
                host.after(ticks, () -> {
                    try {
                        execute(copy, scope);
                    } catch (BreakSignal | ContinueSignal | ReturnSignal signal) {
                        // Control flow cannot cross a scheduled continuation.
                    } catch (RuntimeException failure) {
                        host.log("script error after wait: " + failure.getMessage());
                    }
                });
                return;
            }
            executeStatement(statement, scope);
        }
    }

    private void executeStatement(Stmt statement, Scope scope) {
        if (++steps > MAX_STEPS) {
            throw new ScriptException(program.sourceName(), statement.line(),
                    "script ran too long (over " + MAX_STEPS + " steps); check for an endless loop");
        }
        if (statement instanceof Stmt.VarDecl decl) {
            scope.set(decl.name(), decl.initial() == null ? null : evaluate(decl.initial(), scope));
            return;
        }
        if (statement instanceof Stmt.Assign assign) {
            Object value = evaluate(assign.value(), scope);
            Object current = scope.get(assign.name());
            Object result = switch (assign.operator()) {
                case "=" -> value;
                case "+=" -> binary("+", current, value, assign.line());
                case "-=" -> binary("-", current, value, assign.line());
                case "*=" -> binary("*", current, value, assign.line());
                case "/=" -> binary("/", current, value, assign.line());
                default -> value;
            };
            scope.set(assign.name(), result);
            return;
        }
        if (statement instanceof Stmt.If conditional) {
            for (Stmt.Branch branch : conditional.branches()) {
                if (truthy(evaluate(branch.condition(), scope))) {
                    execute(branch.body(), new Scope(scope));
                    return;
                }
            }
            if (!conditional.otherwise().isEmpty()) {
                execute(conditional.otherwise(), new Scope(scope));
            }
            return;
        }
        if (statement instanceof Stmt.While loop) {
            while (truthy(evaluate(loop.condition(), scope))) {
                if (++steps > MAX_STEPS) {
                    throw new ScriptException(program.sourceName(), loop.line(), "loop ran too long");
                }
                try {
                    execute(loop.body(), new Scope(scope));
                } catch (BreakSignal ignored) {
                    break;
                } catch (ContinueSignal ignored) {
                    // next iteration
                }
            }
            return;
        }
        if (statement instanceof Stmt.ForEach loop) {
            Object iterable = evaluate(loop.iterable(), scope);
            for (Object item : iterate(iterable, loop.line())) {
                if (++steps > MAX_STEPS) {
                    throw new ScriptException(program.sourceName(), loop.line(), "loop ran too long");
                }
                Scope inner = new Scope(scope);
                inner.set(loop.variable(), item);
                try {
                    execute(loop.body(), inner);
                } catch (BreakSignal ignored) {
                    break;
                } catch (ContinueSignal ignored) {
                    // next iteration
                }
            }
            return;
        }
        if (statement instanceof Stmt.Repeat loop) {
            long count = (long) toNumber(evaluate(loop.count(), scope), loop.line());
            for (long i = 0; i < count; i++) {
                if (++steps > MAX_STEPS) {
                    throw new ScriptException(program.sourceName(), loop.line(), "loop ran too long");
                }
                try {
                    execute(loop.body(), new Scope(scope));
                } catch (BreakSignal ignored) {
                    break;
                } catch (ContinueSignal ignored) {
                    // next iteration
                }
            }
            return;
        }
        if (statement instanceof Stmt.Break) {
            throw BreakSignal.INSTANCE;
        }
        if (statement instanceof Stmt.Continue) {
            throw ContinueSignal.INSTANCE;
        }
        if (statement instanceof Stmt.Return userReturn) {
            throw new ReturnSignal(userReturn.value() == null ? null : evaluate(userReturn.value(), scope));
        }
        if (statement instanceof Stmt.Action action) {
            dispatch(action, scope);
            return;
        }
        if (statement instanceof Stmt.ExprStmt expression) {
            evaluate(expression.expression(), scope);
        }
    }

    private void dispatch(Stmt.Action action, Scope scope) {
        String verb = action.verb();
        Object raw = action.argument() == null ? null : evaluate(action.argument(), scope);
        String text = raw == null ? "" : String.valueOf(raw);
        String modifier = action.modifiers().isEmpty() ? "" : action.modifiers().get(0);

        switch (verb) {
            case "message", "tell", "send", "broadcast" -> {
                if ("actionbar".equals(modifier)) {
                    host.actionBar(text);
                } else if ("title".equals(modifier)) {
                    host.title(text);
                } else {
                    host.message(text);
                }
            }
            case "actionbar" -> host.actionBar(text);
            case "title" -> host.title(text);
            case "command", "console", "run" -> host.command(text);
            case "open" -> host.openPage(text);
            case "close" -> host.closePage();
            case "play", "sound" -> host.sound(text);
            case "log", "print" -> host.log(text);
            case "cancel" -> host.cancelEvent(true);
            case "uncancel" -> host.cancelEvent(false);
            default -> host.unsupported(verb, text);
        }
    }

    private Object evaluate(Expr expression, Scope scope) {
        if (expression instanceof Expr.Literal literal) {
            return literal.value();
        }
        if (expression instanceof Expr.Variable variable) {
            return scope.get(variable.name());
        }
        if (expression instanceof Expr.Placeholder placeholder) {
            return host.placeholder(placeholder.name());
        }
        if (expression instanceof Expr.Interpolated interpolated) {
            StringBuilder out = new StringBuilder();
            for (Object part : interpolated.parts()) {
                out.append(part instanceof Expr nested
                        ? stringify(evaluate(nested, scope))
                        : String.valueOf(part));
            }
            return out.toString();
        }
        if (expression instanceof Expr.Binary binary) {
            if (binary.operator().equals("and")) {
                Object left = evaluate(binary.left(), scope);
                return truthy(left) ? evaluate(binary.right(), scope) : left;
            }
            if (binary.operator().equals("or")) {
                Object left = evaluate(binary.left(), scope);
                return truthy(left) ? left : evaluate(binary.right(), scope);
            }
            return binary(binary.operator(), evaluate(binary.left(), scope),
                    evaluate(binary.right(), scope), binary.line());
        }
        if (expression instanceof Expr.Unary unary) {
            Object value = evaluate(unary.operand(), scope);
            return switch (unary.operator()) {
                case "not", "!" -> !truthy(value);
                case "-" -> -toNumber(value, unary.line());
                default -> value;
            };
        }
        if (expression instanceof Expr.Ternary ternary) {
            return truthy(evaluate(ternary.condition(), scope))
                    ? evaluate(ternary.whenTrue(), scope)
                    : evaluate(ternary.whenFalse(), scope);
        }
        if (expression instanceof Expr.ListLiteral list) {
            List<Object> values = new ArrayList<>(list.items().size());
            for (Expr item : list.items()) {
                values.add(evaluate(item, scope));
            }
            return values;
        }
        if (expression instanceof Expr.MapLiteral map) {
            Map<Object, Object> values = new LinkedHashMap<>();
            for (Expr.MapEntry entry : map.entries()) {
                values.put(evaluate(entry.key(), scope), evaluate(entry.value(), scope));
            }
            return values;
        }
        if (expression instanceof Expr.Index index) {
            return indexValue(evaluate(index.target(), scope), evaluate(index.index(), scope), index.line());
        }
        if (expression instanceof Expr.Property property) {
            return propertyValue(evaluate(property.target(), scope), property.name(), property.line());
        }
        if (expression instanceof Expr.Call call) {
            return callValue(call, scope);
        }
        return null;
    }

    private Object callValue(Expr.Call call, Scope scope) {
        if (!(call.target() instanceof Expr.Variable variable)) {
            throw new ScriptException(program.sourceName(), call.line(),
                    "only named functions can be called");
        }
        ScriptFunction function = program.functions().get(variable.name());
        if (function == null) {
            throw new ScriptException(program.sourceName(), call.line(),
                    "unknown function '" + variable.name() + "'");
        }
        List<Object> arguments = new ArrayList<>(call.arguments().size());
        for (Expr argument : call.arguments()) {
            arguments.add(evaluate(argument, scope));
        }
        return invoke(function, arguments);
    }

    private Object invoke(ScriptFunction function, List<Object> arguments) {
        if (arguments.size() != function.parameters().size()) {
            throw new ScriptException(program.sourceName(), function.line(),
                    "function '" + function.name() + "' expects " + function.parameters().size()
                            + " argument(s) but got " + arguments.size());
        }
        Scope frame = new Scope(null);
        for (int i = 0; i < function.parameters().size(); i++) {
            frame.set(function.parameters().get(i), arguments.get(i));
        }
        try {
            execute(function.body(), frame);
        } catch (ReturnSignal signal) {
            return signal.value;
        } catch (BreakSignal | ContinueSignal ignored) {
            // A stray break/continue ends the function.
        }
        return null;
    }

    private Object binary(String operator, Object left, Object right, int line) {
        switch (operator) {
            case "+":
                if (left instanceof String || right instanceof String) {
                    return stringify(left) + stringify(right);
                }
                if (left instanceof Number && right instanceof Number) {
                    return numberMath(left, right, (a, b) -> a + b);
                }
                return stringify(left) + stringify(right);
            case "-":
                return numberMath(left, right, (a, b) -> a - b);
            case "*":
                return numberMath(left, right, (a, b) -> a * b);
            case "/":
                return numberMath(left, right, (a, b) -> a / b);
            case "%":
                return numberMath(left, right, (a, b) -> a % b);
            case "==", "=":
                return equalsValue(left, right);
            case "!=":
                return !equalsValue(left, right);
            case "<":
                return compare(left, right, line) < 0;
            case "<=":
                return compare(left, right, line) <= 0;
            case ">":
                return compare(left, right, line) > 0;
            case ">=":
                return compare(left, right, line) >= 0;
            case "in":
                return contains(right, left);
            case "contains":
                return contains(left, right);
            default:
                throw new ScriptException(program.sourceName(), line, "unsupported operator '" + operator + "'");
        }
    }

    private static boolean equalsValue(Object left, Object right) {
        if (left instanceof Number a && right instanceof Number b) {
            return a.doubleValue() == b.doubleValue();
        }
        return java.util.Objects.equals(left, right);
    }

    private static int compare(Object left, Object right, int line) {
        if (left instanceof Number a && right instanceof Number b) {
            return Double.compare(a.doubleValue(), b.doubleValue());
        }
        return String.valueOf(left).compareTo(String.valueOf(right));
    }

    private static boolean contains(Object container, Object value) {
        if (container instanceof String text) {
            return text.contains(String.valueOf(value));
        }
        if (container instanceof Collection<?> collection) {
            return collection.contains(value);
        }
        if (container instanceof Map<?, ?> map) {
            return map.containsKey(value);
        }
        return false;
    }

    private static Object numberMath(Object left, Object right, DoubleBinary operation) {
        double a = toNumber(left, 0);
        double b = toNumber(right, 0);
        double result = operation.apply(a, b);
        if (result == Math.rint(result) && Math.abs(result) < Integer.MAX_VALUE) {
            return (int) result;
        }
        return result;
    }

    private Object indexValue(Object target, Object index, int line) {
        if (target instanceof List<?> list && index instanceof Number number) {
            int position = number.intValue();
            if (position < 0 || position >= list.size()) {
                return null;
            }
            return list.get(position);
        }
        if (target instanceof Map<?, ?> map) {
            return map.get(index);
        }
        if (target instanceof String text && index instanceof Number number) {
            int position = number.intValue();
            if (position < 0 || position >= text.length()) {
                return null;
            }
            return String.valueOf(text.charAt(position));
        }
        throw new ScriptException(program.sourceName(), line, "value is not indexable");
    }

    private Object propertyValue(Object target, String name, int line) {
        if (target instanceof Map<?, ?> map) {
            return map.get(name);
        }
        throw new ScriptException(program.sourceName(), line,
                "cannot read property '" + name + "' from " + describe(target));
    }

    private List<Object> iterate(Object value, int line) {
        if (value instanceof Collection<?> collection) {
            return new ArrayList<>(collection);
        }
        if (value instanceof Map<?, ?> map) {
            return new ArrayList<>(map.keySet());
        }
        if (value instanceof String text) {
            List<Object> characters = new ArrayList<>(text.length());
            for (char c : text.toCharArray()) {
                characters.add(String.valueOf(c));
            }
            return characters;
        }
        if (value instanceof Number number) {
            List<Object> range = new ArrayList<>(Math.max(0, number.intValue()));
            for (int i = 0; i < number.intValue(); i++) {
                range.add(i);
            }
            return range;
        }
        throw new ScriptException(program.sourceName(), line,
                "cannot loop over " + describe(value));
    }

    private long toTicks(Stmt.Wait wait) {
        double amount = toNumber(evaluate(wait.amount(), new Scope(null)), wait.line());
        return switch (wait.unit().toLowerCase(Locale.ROOT)) {
            case "second", "seconds" -> Math.max(1L, Math.round(amount * 20d));
            case "minute", "minutes" -> Math.max(1L, Math.round(amount * 1200d));
            default -> Math.max(1L, Math.round(amount));
        };
    }

    private static double toNumber(Object value, int line) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String text) {
            try {
                return Double.parseDouble(text.trim());
            } catch (NumberFormatException ignored) {
                return 0d;
            }
        }
        if (value instanceof Boolean bool) {
            return bool ? 1d : 0d;
        }
        return 0d;
    }

    private static boolean truthy(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.doubleValue() != 0d;
        }
        if (value instanceof String text) {
            return !text.isEmpty();
        }
        if (value instanceof Collection<?> collection) {
            return !collection.isEmpty();
        }
        if (value instanceof Map<?, ?> map) {
            return !map.isEmpty();
        }
        return true;
    }

    private static String stringify(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Double number && number == Math.rint(number)) {
            return String.valueOf(number.longValue());
        }
        return String.valueOf(value);
    }

    private static String describe(Object value) {
        return value == null ? "null" : value.getClass().getSimpleName();
    }

    @FunctionalInterface
    private interface DoubleBinary {
        double apply(double left, double right);
    }

    /** Lexical scope with a parent chain; functions get a fresh root. */
    private static final class Scope {
        private final Scope parent;
        private final Map<String, Object> values = new LinkedHashMap<>();

        private Scope(Scope parent) {
            this.parent = parent;
        }

        private Object get(String name) {
            if (values.containsKey(name)) {
                return values.get(name);
            }
            return parent == null ? null : parent.get(name);
        }

        private void set(String name, Object value) {
            Scope scope = this;
            while (scope != null) {
                if (scope.values.containsKey(name)) {
                    scope.values.put(name, value);
                    return;
                }
                scope = scope.parent;
            }
            values.put(name, value);
        }
    }

    private static final class BreakSignal extends RuntimeException {
        private static final long serialVersionUID = 1L;
        private static final BreakSignal INSTANCE = new BreakSignal();

        private BreakSignal() {
            super(null, null, false, false);
        }
    }

    private static final class ContinueSignal extends RuntimeException {
        private static final long serialVersionUID = 1L;
        private static final ContinueSignal INSTANCE = new ContinueSignal();

        private ContinueSignal() {
            super(null, null, false, false);
        }
    }

    private static final class ReturnSignal extends RuntimeException {
        private static final long serialVersionUID = 1L;
        private final transient Object value;

        private ReturnSignal(Object value) {
            super(null, null, false, false);
            this.value = value;
        }
    }
}
