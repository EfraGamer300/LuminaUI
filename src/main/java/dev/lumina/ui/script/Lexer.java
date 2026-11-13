package dev.lumina.ui.script;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Turns script source into a token stream with explicit {@code INDENT}/{@code DEDENT} markers.
 *
 * <p>The grammar is indentation-based, so block structure is recovered here rather than in the
 * parser. Blank lines and {@code #} comments produce no tokens, and a tab counts as four spaces so a
 * file mixed by different editors still parses predictably.
 */
public final class Lexer {

    private static final int TAB_WIDTH = 4;

    private final String sourceName;
    private final List<String> lines;
    private final List<Token> tokens = new ArrayList<>();
    private final Deque<Integer> indents = new ArrayDeque<>();

    private int lineNumber;

    public Lexer(String source, String sourceName) {
        this.sourceName = sourceName;
        this.lines = List.of(source.split("\n", -1));
        this.indents.push(0);
    }

    /** Tokenizes a single expression fragment, used for {@code ${...}} interpolation bodies. */
    public static List<Token> tokenizeFragment(String fragment, String sourceName, int line) {
        Lexer lexer = new Lexer(fragment, sourceName);
        List<Token> raw = lexer.tokenize();
        List<Token> out = new ArrayList<>(raw.size());
        for (Token token : raw) {
            if (token.type() == TokenType.EOF) {
                continue;
            }
            if (token.type() == TokenType.NEWLINE) {
                continue;
            }
            out.add(new Token(token.type(), token.text(), line, token.column()));
        }
        return out;
    }

    public List<Token> tokenize() {
        for (int index = 0; index < lines.size(); index++) {
            lineNumber = index + 1;
            String raw = lines.get(index);
            if (raw.endsWith("\r")) {
                raw = raw.substring(0, raw.length() - 1);
            }
            String trimmed = raw.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            String code = stripComment(raw);
            if (code.isBlank()) {
                continue;
            }
            int indent = measureIndent(code);
            applyIndent(indent);
            scanLine(code, indent);
            tokens.add(new Token(TokenType.NEWLINE, "", lineNumber, code.length()));
        }

        while (indents.size() > 1) {
            indents.pop();
            tokens.add(new Token(TokenType.DEDENT, "", lineNumber, 0));
        }
        tokens.add(new Token(TokenType.EOF, "", lineNumber, 0));
        return tokens;
    }

    private void applyIndent(int indent) {
        int current = indents.peek();
        if (indent > current) {
            indents.push(indent);
            tokens.add(new Token(TokenType.INDENT, "", lineNumber, 0));
            return;
        }
        while (indent < indents.peek()) {
            indents.pop();
            tokens.add(new Token(TokenType.DEDENT, "", lineNumber, 0));
        }
        if (indent != indents.peek()) {
            throw error("indentation does not match any surrounding block (" + indent + " spaces)");
        }
    }

    private void scanLine(String code, int indent) {
        int index = indent;
        while (index < code.length()) {
            char c = code.charAt(index);
            if (c == ' ' || c == '\t') {
                index++;
                continue;
            }
            if (c == '#') {
                return;
            }
            if (Character.isLetter(c) || c == '_') {
                int start = index;
                while (index < code.length()
                        && (Character.isLetterOrDigit(code.charAt(index)) || code.charAt(index) == '_')) {
                    index++;
                }
                tokens.add(new Token(TokenType.NAME, code.substring(start, index), lineNumber, start));
                continue;
            }
            if (Character.isDigit(c)) {
                int start = index;
                while (index < code.length()
                        && (Character.isDigit(code.charAt(index)) || code.charAt(index) == '.')) {
                    index++;
                }
                tokens.add(new Token(TokenType.NUMBER, code.substring(start, index), lineNumber, start));
                continue;
            }
            if (c == '"' || c == '\'') {
                index = scanString(code, index);
                continue;
            }
            if (c == '%') {
                int close = code.indexOf('%', index + 1);
                if (close > index + 1) {
                    String name = code.substring(index + 1, close);
                    if (name.matches("[A-Za-z0-9_]+")) {
                        tokens.add(new Token(TokenType.PLACEHOLDER, name, lineNumber, index));
                        index = close + 1;
                        continue;
                    }
                }
            }
            index = scanSymbol(code, index);
        }
    }

    private int scanString(String code, int start) {
        char quote = code.charAt(start);
        StringBuilder value = new StringBuilder();
        int index = start + 1;
        while (index < code.length()) {
            char c = code.charAt(index);
            if (c == '\\' && index + 1 < code.length()) {
                char escaped = code.charAt(index + 1);
                value.append(switch (escaped) {
                    case 'n' -> '\n';
                    case 't' -> '\t';
                    case 'r' -> '\r';
                    case '"', '\'', '\\' -> escaped;
                    default -> escaped;
                });
                index += 2;
                continue;
            }
            if (c == quote) {
                tokens.add(new Token(TokenType.STRING, value.toString(), lineNumber, start));
                return index + 1;
            }
            value.append(c);
            index++;
        }
        throw error("unterminated string starting at column " + start);
    }

    private int scanSymbol(String code, int start) {
        String[] twoChar = {"==", "!=", "<=", ">=", "&&", "||", "+=", "-=", "*=", "/="};
        if (start + 1 < code.length()) {
            String pair = code.substring(start, start + 2);
            for (String candidate : twoChar) {
                if (candidate.equals(pair)) {
                    tokens.add(new Token(TokenType.SYMBOL, pair, lineNumber, start));
                    return start + 2;
                }
            }
        }
        char c = code.charAt(start);
        if ("()[]{},:.+-*/%=<>!?".indexOf(c) >= 0) {
            tokens.add(new Token(TokenType.SYMBOL, String.valueOf(c), lineNumber, start));
            return start + 1;
        }
        throw error("unexpected character '" + c + "' at column " + start);
    }

    private static String stripComment(String line) {
        boolean inSingle = false;
        boolean inDouble = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\\') {
                i++;
                continue;
            }
            if (c == '\'' && !inDouble) {
                inSingle = !inSingle;
            } else if (c == '"' && !inSingle) {
                inDouble = !inDouble;
            } else if (c == '#' && !inSingle && !inDouble) {
                return line.substring(0, i);
            }
        }
        return line;
    }

    private static int measureIndent(String line) {
        int width = 0;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == ' ') {
                width++;
            } else if (c == '\t') {
                width += TAB_WIDTH;
            } else {
                break;
            }
        }
        return width;
    }

    private ScriptException error(String message) {
        return new ScriptException(sourceName, lineNumber, message);
    }
}
