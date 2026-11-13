package dev.lumina.ui.script;

/**
 * One lexed token. {@code text} keeps the raw source slice so error messages can quote it back.
 */
public record Token(TokenType type, String text, int line, int column) {

    public boolean is(String value) {
        return (type == TokenType.NAME || type == TokenType.SYMBOL) && text.equalsIgnoreCase(value);
    }

    public boolean isSymbol(String value) {
        return type == TokenType.SYMBOL && text.equals(value);
    }

    public boolean isWord(String value) {
        return type == TokenType.NAME && text.equalsIgnoreCase(value);
    }

    @Override
    public String toString() {
        return type + "('" + text + "'@" + line + ":" + column + ")";
    }
}
