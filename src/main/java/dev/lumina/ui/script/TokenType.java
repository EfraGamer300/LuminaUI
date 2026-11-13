package dev.lumina.ui.script;

/**
 * Token kinds of the indentation-based script grammar.
 */
public enum TokenType {
    NAME,
    NUMBER,
    STRING,
    PLACEHOLDER,
    SYMBOL,
    NEWLINE,
    INDENT,
    DEDENT,
    EOF
}
