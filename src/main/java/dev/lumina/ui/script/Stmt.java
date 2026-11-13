package dev.lumina.ui.script;

import java.util.List;

/**
 * Statement tree of the script language.
 */
public sealed interface Stmt {

    int line();

    record VarDecl(String name, Expr initial, int line) implements Stmt {
    }

    /** {@code set name = value} and the compound forms {@code += -= *= /=}. */
    record Assign(String name, String operator, Expr value, int line) implements Stmt {
    }

    record Branch(Expr condition, List<Stmt> body) {
        public Branch {
            body = List.copyOf(body);
        }
    }

    record If(List<Branch> branches, List<Stmt> otherwise, int line) implements Stmt {
        public If {
            branches = List.copyOf(branches);
            otherwise = List.copyOf(otherwise);
        }
    }

    record While(Expr condition, List<Stmt> body, int line) implements Stmt {
        public While {
            body = List.copyOf(body);
        }
    }

    record ForEach(String variable, Expr iterable, List<Stmt> body, int line) implements Stmt {
        public ForEach {
            body = List.copyOf(body);
        }
    }

    record Repeat(Expr count, List<Stmt> body, int line) implements Stmt {
        public Repeat {
            body = List.copyOf(body);
        }
    }

    record Break(int line) implements Stmt {
    }

    record Continue(int line) implements Stmt {
    }

    record Return(Expr value, int line) implements Stmt {
    }

    /** Suspends the handler for the given amount, expressed in {@code unit}. */
    record Wait(Expr amount, String unit, int line) implements Stmt {
    }

    /**
     * One side-effecting verb such as {@code message}, {@code command} or {@code open}. Verbs are
     * dispatched at runtime, so the parser stays independent of what the host supports.
     */
    record Action(String verb, List<String> modifiers, Expr argument, int line) implements Stmt {
        public Action {
            modifiers = List.copyOf(modifiers);
        }
    }

    record ExprStmt(Expr expression, int line) implements Stmt {
    }
}
