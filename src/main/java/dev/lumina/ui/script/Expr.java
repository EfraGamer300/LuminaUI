package dev.lumina.ui.script;

import java.util.List;

/**
 * Expression tree of the script language.
 */
public sealed interface Expr {

    int line();

    record Literal(Object value, int line) implements Expr {
    }

    record Variable(String name, int line) implements Expr {
    }

    /** {@code %player_name%} style placeholder resolved by the host at evaluation time. */
    record Placeholder(String name, int line) implements Expr {
    }

    record Binary(String operator, Expr left, Expr right, int line) implements Expr {
    }

    record Unary(String operator, Expr operand, int line) implements Expr {
    }

    record Ternary(Expr condition, Expr whenTrue, Expr whenFalse, int line) implements Expr {
    }

    record ListLiteral(List<Expr> items, int line) implements Expr {
        public ListLiteral {
            items = List.copyOf(items);
        }
    }

    record MapEntry(Expr key, Expr value) {
    }

    record MapLiteral(List<MapEntry> entries, int line) implements Expr {
        public MapLiteral {
            entries = List.copyOf(entries);
        }
    }

    record Index(Expr target, Expr index, int line) implements Expr {
    }

    record Property(Expr target, String name, int line) implements Expr {
    }

    record Call(Expr target, List<Expr> arguments, int line) implements Expr {
        public Call {
            arguments = List.copyOf(arguments);
        }
    }

    /** String built from literal slices and embedded expressions. */
    record Interpolated(List<Object> parts, int line) implements Expr {
        public Interpolated {
            parts = List.copyOf(parts);
        }
    }
}
