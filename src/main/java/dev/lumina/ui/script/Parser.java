package dev.lumina.ui.script;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Recursive-descent parser for the indentation-based script language.
 *
 * <p>Top level accepts declarations ({@code page}, {@code let}/{@code var}/{@code set},
 * {@code function}/{@code def}) and handlers ({@code on}, {@code when}, {@code every},
 * {@code command}). Blocks are delimited by a trailing colon plus an indented body, which the lexer
 * already turned into explicit tokens.
 */
public final class Parser {

    private final List<Token> tokens;
    private final String sourceName;
    private int position;

    private final Map<String, Expr> globals = new LinkedHashMap<>();
    private final Map<String, ScriptFunction> functions = new LinkedHashMap<>();
    private final List<ScriptHandler> handlers = new ArrayList<>();
    private String pageId;

    public Parser(List<Token> tokens, String sourceName) {
        this.tokens = tokens;
        this.sourceName = sourceName;
    }

    public static ScriptProgram parse(String source, String sourceName) {
        Parser parser = new Parser(new Lexer(source, sourceName).tokenize(), sourceName);
        return parser.parseProgram();
    }

    public ScriptProgram parseProgram() {
        skipNewlines();
        while (!check(TokenType.EOF)) {
            parseTopLevel();
            skipNewlines();
        }
        return new ScriptProgram(sourceName, pageId, globals, functions, handlers);
    }

    private void parseTopLevel() {
        Token token = peek();
        if (token.type() != TokenType.NAME) {
            throw error(token, "expected a declaration like page, on, every or function");
        }
        String keyword = token.text().toLowerCase(Locale.ROOT);
        switch (keyword) {
            case "page" -> {
                advance();
                pageId = expect(TokenType.STRING, "the page name").text();
                expectLineEnd("after the page name");
            }
            case "let", "var" -> {
                advance();
                String name = expect(TokenType.NAME, "a variable name").text();
                Expr initial = null;
                if (matchSymbol("=")) {
                    initial = parseExpression();
                }
                globals.put(name, initial == null ? new Expr.Literal(null, token.line()) : initial);
                expectLineEnd("after the variable declaration");
            }
            case "set" -> {
                advance();
                String name = expect(TokenType.NAME, "a variable name").text();
                if (peek().type() == TokenType.SYMBOL && isAssignOperator(peek().text())) {
                    // Compound assignment at top level is only meaningful in a handler, so it is
                    // rejected here instead of silently discarding the operator.
                    throw error(peek(), "compound assignment is only allowed inside handlers");
                }
                expectSymbol("=", "after the variable name");
                Expr value = parseExpression();
                globals.put(name, value);
                expectLineEnd("after the assignment");
            }
            case "function", "def" -> parseFunction(token);
            case "on", "when" -> parseEventHandler(token);
            case "every" -> parseEveryHandler(token);
            case "command" -> parseCommandHandler(token);
            default -> throw error(token, "unknown declaration '" + token.text()
                    + "'; expected page, on, every, command, function, let or set");
        }
    }

    private void parseFunction(Token start) {
        advance();
        String name = expect(TokenType.NAME, "the function name").text();
        expectSymbol("(", "after the function name");
        List<String> parameters = new ArrayList<>();
        if (!checkSymbol(")")) {
            do {
                parameters.add(expect(TokenType.NAME, "a parameter name").text());
            } while (matchSymbol(","));
        }
        expectSymbol(")", "after the parameter list");
        List<Stmt> body = parseBlock("the function body");
        if (functions.containsKey(name)) {
            throw error(start, "function '" + name + "' is already declared");
        }
        functions.put(name, new ScriptFunction(name, parameters, body, start.line()));
    }

    private void parseEventHandler(Token start) {
        advance();
        String event = expect(TokenType.NAME, "an event name, like click or open")
                .text().toLowerCase(Locale.ROOT);
        String elementId = null;
        String page = null;

        if (check(TokenType.STRING)) {
            elementId = advance().text();
        }
        if (matchWord("in")) {
            page = expect(TokenType.STRING, "the page name").text();
        } else if (pageId != null) {
            page = pageId;
        }

        List<Stmt> body = parseBlock("the handler body");
        handlers.add(new ScriptHandler(
                normalizeEvent(event, start), elementId, page, null, null, 0L, body, start.line()));
    }

    private void parseCommandHandler(Token start) {
        advance();
        String name;
        if (matchSymbol("/")) {
            name = expect(TokenType.NAME, "the command name").text();
        } else if (check(TokenType.STRING)) {
            name = advance().text();
        } else {
            name = expect(TokenType.NAME, "the command name, like /shop").text();
        }
        name = name.startsWith("/") ? name.substring(1) : name;

        String permission = null;
        if (matchWord("permission")) {
            permission = expect(TokenType.STRING, "the permission node").text();
        }
        List<Stmt> body = parseBlock("the command body");
        handlers.add(new ScriptHandler(
                "command", null, pageId, name.toLowerCase(Locale.ROOT), permission, 0L, body, start.line()));
    }

    private void parseEveryHandler(Token start) {
        advance();
        Expr amount = parseExpression();
        String unit = "seconds";
        if (check(TokenType.NAME)) {
            unit = advance().text().toLowerCase(Locale.ROOT);
        }
        String page = pageId;
        if (matchWord("in")) {
            page = expect(TokenType.STRING, "the page name").text();
        }
        long ticks = Math.max(1L, Math.round(toDouble(amount, start) * ticksPerUnit(unit, start)));
        List<Stmt> body = parseBlock("the timer body");
        handlers.add(new ScriptHandler("every", null, page, null, null, ticks, body, start.line()));
    }

    private static String normalizeEvent(String event, Token token) {
        return switch (event) {
            case "left", "right" -> event + " click";
            case "enter" -> "hover";
            case "leave", "unhover" -> "unhover";
            case "drop" -> "drop";
            case "submit" -> "input";
            case "load", "join", "quit", "open", "close", "opening" -> event;
            default -> event;
        };
    }

    private static double ticksPerUnit(String unit, Token token) {
        return switch (unit) {
            case "tick", "ticks" -> 1d;
            case "second", "seconds" -> 20d;
            case "minute", "minutes" -> 1200d;
            default -> throw new ScriptException(token.line() + "", token.line(),
                    "unknown time unit '" + unit + "'; use ticks, seconds or minutes");
        };
    }

    private static double toDouble(Expr expr, Token token) {
        if (expr instanceof Expr.Literal literal && literal.value() instanceof Number number) {
            return number.doubleValue();
        }
        throw new ScriptException("<script>", token.line(), "every needs a plain number, like: every 2 seconds:");
    }

    // --- statements -------------------------------------------------------

    private List<Stmt> parseBlock(String what) {
        expectSymbol(":", "at the end of the line before " + what);
        if (!check(TokenType.NEWLINE)) {
            // Single-line block: "on open: message \"hi\"".
            List<Stmt> single = new ArrayList<>();
            single.add(parseStatement());
            consumeLineEnd();
            return single;
        }
        advanceNewline();
        if (!check(TokenType.INDENT)) {
            throw error(peek(), "the lines inside this block must be indented");
        }
        advance();
        List<Stmt> body = new ArrayList<>();
        skipNewlines();
        while (!check(TokenType.DEDENT) && !check(TokenType.EOF)) {
            body.add(parseStatement());
            skipNewlines();
        }
        if (check(TokenType.DEDENT)) {
            advance();
        }
        return body;
    }

    private Stmt parseStatement() {
        Token token = peek();
        if (token.type() == TokenType.NAME) {
            String word = token.text().toLowerCase(Locale.ROOT);
            switch (word) {
                case "if" -> {
                    return parseIf();
                }
                case "while" -> {
                    advance();
                    Expr condition = parseExpression();
                    return new Stmt.While(condition, parseBlock("the loop body"), token.line());
                }
                case "repeat" -> {
                    advance();
                    Expr count = parseExpression();
                    String unit = "";
                    if (check(TokenType.NAME) && isLoopUnit(peek().text())) {
                        unit = advance().text().toLowerCase(Locale.ROOT);
                    }
                    List<Stmt> body = parseBlock("the loop body");
                    return new Stmt.Repeat(withUnit(count, unit, token), body, token.line());
                }
                case "for" -> {
                    return parseFor();
                }
                case "break" -> {
                    advance();
                    expectLineEnd("after break");
                    return new Stmt.Break(token.line());
                }
                case "continue" -> {
                    advance();
                    expectLineEnd("after continue");
                    return new Stmt.Continue(token.line());
                }
                case "return" -> {
                    advance();
                    Expr value = check(TokenType.NEWLINE) ? null : parseExpression();
                    expectLineEnd("after return");
                    return new Stmt.Return(value, token.line());
                }
                case "wait", "delay" -> {
                    advance();
                    Expr amount = parseExpression();
                    String unit = "ticks";
                    if (check(TokenType.NAME) && isTimeUnit(peek().text())) {
                        unit = advance().text().toLowerCase(Locale.ROOT);
                    }
                    expectLineEnd("after wait");
                    return new Stmt.Wait(amount, unit, token.line());
                }
                case "let", "var" -> {
                    advance();
                    String name = expect(TokenType.NAME, "a variable name").text();
                    Expr initial = null;
                    if (matchSymbol("=")) {
                        initial = parseExpression();
                    }
                    expectLineEnd("after the variable declaration");
                    return new Stmt.VarDecl(name, initial, token.line());
                }
                case "set" -> {
                    advance();
                    String name = expect(TokenType.NAME, "a variable name").text();
                    String operator = "=";
                    if (peek().type() == TokenType.SYMBOL && isAssignOperator(peek().text())) {
                        operator = advance().text();
                    } else {
                        expectSymbol("=", "after the variable name");
                    }
                    Expr value = parseExpression();
                    expectLineEnd("after the assignment");
                    return new Stmt.Assign(name, operator, value, token.line());
                }
                default -> {
                    // fall through to verb handling
                }
            }
            if (isVerb(word)) {
                return parseAction();
            }
        }
        Expr expression = parseExpression();
        expectLineEnd("after the expression");
        return new Stmt.ExprStmt(expression, token.line());
    }

    private Stmt parseIf() {
        Token start = advance();
        List<Stmt.Branch> branches = new ArrayList<>();
        branches.add(new Stmt.Branch(parseExpression(), parseBlock("the if body")));
        skipNewlines();
        List<Stmt> otherwise = List.of();

        while (check(TokenType.NAME)) {
            Token branchToken = peek();
            boolean isElif = branchToken.isWord("elif") || branchToken.isWord("elseif");
            boolean isElse = branchToken.isWord("else") || branchToken.isWord("otherwise");
            if (!isElif && !isElse) {
                break;
            }
            advance();
            if (isElif) {
                branches.add(new Stmt.Branch(parseExpression(), parseBlock("the elif body")));
                skipNewlines();
                continue;
            }
            // "else:" directly, or "else if <condition>:" as a chained branch.
            if (check(TokenType.NAME) && peek().isWord("if")) {
                advance();
                branches.add(new Stmt.Branch(parseExpression(), parseBlock("the else-if body")));
                skipNewlines();
                continue;
            }
            otherwise = parseBlock("the else body");
            break;
        }
        return new Stmt.If(branches, otherwise, start.line());
    }

    private Stmt parseFor() {
        Token start = advance();
        if (matchWord("each")) {
            // "for each item in list" reads naturally and is accepted as an alias.
        }
        String variable = expect(TokenType.NAME, "a loop variable").text();
        expectWord("in", "after the loop variable");
        Expr iterable = parseExpression();
        return new Stmt.ForEach(variable, iterable, parseBlock("the loop body"), start.line());
    }

    private Stmt parseAction() {
        Token verbToken = advance();
        String verb = verbToken.text().toLowerCase(Locale.ROOT);
        List<String> modifiers = new ArrayList<>();

        // Modifiers keep the verb readable: "message actionbar", "sound play", "open in page".
        while (check(TokenType.NAME) && isModifier(peek().text())) {
            modifiers.add(advance().text().toLowerCase(Locale.ROOT));
        }
        Expr argument = null;
        if (!check(TokenType.NEWLINE) && !check(TokenType.EOF)) {
            argument = parseExpression();
        }
        expectLineEnd("after the " + verb + " statement");
        return new Stmt.Action(verb, modifiers, argument, verbToken.line());
    }

    // --- expressions ------------------------------------------------------

    public Expr parseExpression() {
        return parseTernary();
    }

    /**
     * Conditional expression, the lowest-precedence operator: {@code condition ? a : b}.
     */
    private Expr parseTernary() {
        Expr condition = parseOr();
        if (checkSymbol("?") && !check(TokenType.NEWLINE)) {
            Token question = advance();
            Expr whenTrue = parseTernary();
            expectSymbol(":", "in the conditional expression");
            Expr whenFalse = parseTernary();
            return new Expr.Ternary(condition, whenTrue, whenFalse, question.line());
        }
        return condition;
    }

    private Expr parseOr() {
        Expr left = parseAnd();
        while (check(TokenType.NAME) && peek().isWord("or") || checkSymbol("||")) {
            Token operator = advance();
            Expr right = parseAnd();
            left = new Expr.Binary("or", left, right, operator.line());
        }
        return left;
    }

    private Expr parseAnd() {
        Expr left = parseNot();
        while (check(TokenType.NAME) && peek().isWord("and") || checkSymbol("&&")) {
            Token operator = advance();
            Expr right = parseNot();
            left = new Expr.Binary("and", left, right, operator.line());
        }
        return left;
    }

    /**
     * {@code not} binds looser than comparison so {@code not a == 4} reads as {@code not (a == 4)},
     * matching how the operator is used in scripts. The {@code !} symbol keeps tight binding.
     */
    private Expr parseNot() {
        if (check(TokenType.NAME) && peek().isWord("not")) {
            Token token = advance();
            return new Expr.Unary("not", parseNot(), token.line());
        }
        return parseComparison();
    }

    private Expr parseComparison() {
        Expr left = parseAdditive();
        while (true) {
            Token token = peek();
            if (token.type() != TokenType.SYMBOL
                    && !(token.type() == TokenType.NAME && token.isWord("in"))) {
                return left;
            }
            String operator;
            if (token.type() == TokenType.SYMBOL
                    && (token.text().equals("==") || token.text().equals("!=")
                    || token.text().equals("<=") || token.text().equals(">=")
                    || token.text().equals("<") || token.text().equals(">")
                    || token.text().equals("="))) {
                operator = advance().text();
            } else if (token.isWord("in")) {
                advance();
                operator = "in";
            } else if (token.isWord("contains")) {
                advance();
                operator = "contains";
            } else {
                return left;
            }
            Expr right = parseAdditive();
            left = new Expr.Binary(operator, left, right, token.line());
        }
    }

    private Expr parseAdditive() {
        Expr left = parseMultiplicative();
        while (checkSymbol("+") || checkSymbol("-")) {
            Token operator = advance();
            Expr right = parseMultiplicative();
            left = new Expr.Binary(operator.text(), left, right, operator.line());
        }
        return left;
    }

    private Expr parseMultiplicative() {
        Expr left = parseUnary();
        while (checkSymbol("*") || checkSymbol("/") || checkSymbol("%")) {
            Token operator = advance();
            Expr right = parseUnary();
            left = new Expr.Binary(operator.text(), left, right, operator.line());
        }
        return left;
    }

    private Expr parseUnary() {
        Token token = peek();
        if (checkSymbol("-") || checkSymbol("!")) {
            advance();
            return new Expr.Unary(token.text(), parseUnary(), token.line());
        }
        return parsePostfix();
    }

    private Expr parsePostfix() {
        Expr expression = parsePrimary();
        while (true) {
            if (checkSymbol("(")) {
                Token start = advance();
                List<Expr> arguments = new ArrayList<>();
                if (!checkSymbol(")")) {
                    do {
                        arguments.add(parseExpression());
                    } while (matchSymbol(","));
                }
                expectSymbol(")", "after the argument list");
                expression = new Expr.Call(expression, arguments, start.line());
                continue;
            }
            if (checkSymbol("[")) {
                Token start = advance();
                Expr index = parseExpression();
                expectSymbol("]", "after the index expression");
                expression = new Expr.Index(expression, index, start.line());
                continue;
            }
            if (checkSymbol(".") && peekAt(1).type() == TokenType.NAME) {
                advance();
                Token name = advance();
                expression = new Expr.Property(expression, name.text(), name.line());
                continue;
            }
            break;
        }
        return expression;
    }

    private Expr parsePrimary() {
        Token token = peek();
        switch (token.type()) {
            case NUMBER -> {
                advance();
                return new Expr.Literal(parseNumber(token), token.line());
            }
            case STRING -> {
                advance();
                if (token.text().contains("${") || token.text().contains("%")) {
                    return parseInterpolated(token.text(), token.line());
                }
                return new Expr.Literal(token.text(), token.line());
            }
            case PLACEHOLDER -> {
                advance();
                return new Expr.Placeholder(token.text(), token.line());
            }
            case NAME -> {
                String word = token.text().toLowerCase(Locale.ROOT);
                if (word.equals("true") || word.equals("false")) {
                    advance();
                    return new Expr.Literal(Boolean.parseBoolean(word), token.line());
                }
                if (word.equals("null") || word.equals("nothing")) {
                    advance();
                    return new Expr.Literal(null, token.line());
                }
                advance();
                return new Expr.Variable(token.text(), token.line());
            }
            case SYMBOL -> {
                if (token.isSymbol("(")) {
                    advance();
                    Expr inner = parseExpression();
                    expectSymbol(")", "after the parenthesised expression");
                    return inner;
                }
                if (token.isSymbol("[")) {
                    advance();
                    List<Expr> items = new ArrayList<>();
                    if (!checkSymbol("]")) {
                        do {
                            items.add(parseExpression());
                        } while (matchSymbol(","));
                    }
                    expectSymbol("]", "after the list literal");
                    return new Expr.ListLiteral(items, token.line());
                }
                if (token.isSymbol("{")) {
                    advance();
                    List<Expr.MapEntry> entries = new ArrayList<>();
                    if (!checkSymbol("}")) {
                        do {
                            Expr key = parseExpression();
                            expectSymbol(":", "between a map key and its value");
                            Expr value = parseExpression();
                            entries.add(new Expr.MapEntry(key, value));
                        } while (matchSymbol(","));
                    }
                    expectSymbol("}", "after the map literal");
                    return new Expr.MapLiteral(entries, token.line());
                }
            }
            default -> {
                // handled below
            }
        }
        throw error(token, "expected a value but found '" + token.text() + "'");
    }

    /**
     * Splits a string literal on {@code ${...}} and {@code %name%}.
     *
     * <p>Embedded expressions are re-lexed and parsed with the same grammar, so interpolation never
     * drifts from the rest of the language. Placeholders become {@link Expr.Placeholder} nodes and
     * are resolved by the host when the string is evaluated.
     */
    private Expr parseInterpolated(String text, int line) {
        List<Object> parts = new ArrayList<>();
        StringBuilder literal = new StringBuilder();
        int index = 0;
        while (index < text.length()) {
            int expressionStart = text.indexOf("${", index);
            int placeholderStart = indexOfPlaceholder(text, index);

            if (expressionStart < 0 && placeholderStart < 0) {
                literal.append(text, index, text.length());
                break;
            }
            boolean useExpression = expressionStart >= 0
                    && (placeholderStart < 0 || expressionStart < placeholderStart);

            if (useExpression) {
                literal.append(text, index, expressionStart);
                int depth = 1;
                int cursor = expressionStart + 2;
                while (cursor < text.length() && depth > 0) {
                    char c = text.charAt(cursor);
                    if (c == '{') {
                        depth++;
                    } else if (c == '}') {
                        depth--;
                    }
                    cursor++;
                }
                if (depth != 0) {
                    throw new ScriptException(sourceName, line, "unclosed ${...} in string");
                }
                flushLiteral(parts, literal);
                String fragment = text.substring(expressionStart + 2, cursor - 1);
                if (!fragment.isBlank()) {
                    List<Token> fragmentTokens = Lexer.tokenizeFragment(fragment, sourceName, line);
                    Parser nested = new Parser(fragmentTokens, sourceName);
                    parts.add(nested.parseExpression());
                }
                index = cursor;
                continue;
            }

            literal.append(text, index, placeholderStart);
            int close = text.indexOf('%', placeholderStart + 1);
            flushLiteral(parts, literal);
            parts.add(new Expr.Placeholder(text.substring(placeholderStart + 1, close), line));
            index = close + 1;
        }
        flushLiteral(parts, literal);
        if (parts.isEmpty()) {
            parts.add("");
        }
        // A string that is exactly one placeholder is that placeholder, which keeps handlers that
        // only pass "%player%" free of interpolation overhead.
        if (parts.size() == 1 && parts.get(0) instanceof Expr.Placeholder) {
            return (Expr) parts.get(0);
        }
        return new Expr.Interpolated(parts, line);
    }

    private static void flushLiteral(List<Object> parts, StringBuilder literal) {
        if (!literal.isEmpty()) {
            parts.add(literal.toString());
            literal.setLength(0);
        }
    }

    /** Index of the next {@code %name%} pair, or -1 when there is none. */
    private static int indexOfPlaceholder(String text, int from) {
        int cursor = from;
        while (cursor < text.length()) {
            int start = text.indexOf('%', cursor);
            if (start < 0) {
                return -1;
            }
            int close = text.indexOf('%', start + 1);
            if (close < 0) {
                return -1;
            }
            String name = text.substring(start + 1, close);
            if (name.matches("[A-Za-z0-9_]+")) {
                return start;
            }
            cursor = close + 1;
        }
        return -1;
    }

    private static Object parseNumber(Token token) {
        String text = token.text();
        if (text.contains(".")) {
            return Double.parseDouble(text);
        }
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException tooLarge) {
            return Long.parseLong(text);
        }
    }

    // --- token helpers ----------------------------------------------------

    private Token peek() {
        return tokens.get(Math.min(position, tokens.size() - 1));
    }

    private Token peekAt(int offset) {
        return tokens.get(Math.min(position + offset, tokens.size() - 1));
    }

    private Token advance() {
        Token token = peek();
        if (position < tokens.size() - 1) {
            position++;
        }
        return token;
    }

    private boolean check(TokenType type) {
        return peek().type() == type;
    }

    private boolean checkSymbol(String symbol) {
        return peek().isSymbol(symbol);
    }

    private boolean matchSymbol(String symbol) {
        if (checkSymbol(symbol)) {
            advance();
            return true;
        }
        return false;
    }

    private boolean matchWord(String word) {
        if (check(TokenType.NAME) && peek().isWord(word)) {
            advance();
            return true;
        }
        return false;
    }

    private void expectSymbol(String symbol, String context) {
        if (!matchSymbol(symbol)) {
            throw error(peek(), "expected '" + symbol + "' " + context + " but found '" + peek().text() + "'");
        }
    }

    private void expectWord(String word, String context) {
        if (!matchWord(word)) {
            throw error(peek(), "expected '" + word + "' " + context);
        }
    }

    private Token expect(TokenType type, String what) {
        if (!check(type)) {
            throw error(peek(), "expected " + what + " but found '" + peek().text() + "'");
        }
        return advance();
    }

    private void expectLineEnd(String context) {
        if (check(TokenType.NEWLINE)) {
            advance();
            return;
        }
        if (check(TokenType.EOF) || check(TokenType.DEDENT)) {
            return;
        }
        throw error(peek(), "unexpected '" + peek().text() + "' " + context);
    }

    private void consumeLineEnd() {
        if (check(TokenType.NEWLINE)) {
            advance();
        }
    }

    private void skipNewlines() {
        while (check(TokenType.NEWLINE)) {
            advance();
        }
    }

    private void advanceNewline() {
        if (check(TokenType.NEWLINE)) {
            advance();
        }
    }

    private static boolean isAssignOperator(String symbol) {
        return switch (symbol) {
            case "+=", "-=", "*=", "/=" -> true;
            default -> false;
        };
    }

    private static boolean isVerb(String word) {
        return switch (word) {
            case "message", "tell", "send", "broadcast", "actionbar", "title",
                 "command", "run", "console", "open", "close", "play", "sound",
                 "show", "hide", "toggle", "set", "add", "remove", "delete", "move",
                 "resize", "teleport", "give", "click", "action", "scroll", "clone",
                 "stop", "cancel", "uncancel", "log", "print", "create", "animate" -> true;
            default -> false;
        };
    }

    private static boolean isModifier(String word) {
        return switch (word) {
            case "actionbar", "title", "chat", "console", "player", "as", "in", "at",
                 "with", "up", "down", "left", "right", "start", "end", "move" -> true;
            default -> false;
        };
    }

    private static boolean isLoopUnit(String word) {
        return switch (word) {
            case "times", "tick", "ticks", "second", "seconds", "minute", "minutes" -> true;
            default -> false;
        };
    }

    private static boolean isTimeUnit(String word) {
        return isLoopUnit(word);
    }

    /** {@code repeat 3 times} keeps the unit so the runtime can report it, but only the count matters. */
    private static Expr withUnit(Expr count, String unit, Token token) {
        return count;
    }

    private ScriptException error(Token token, String message) {
        return new ScriptException(sourceName, token.line(), message);
    }
}
