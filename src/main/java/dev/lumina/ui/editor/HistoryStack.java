package dev.lumina.ui.editor;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Bounded undo/redo history over page snapshots.
 *
 * <p>Storing whole immutable page snapshots is cheap here (pages are small value trees) and removes
 * an entire class of bugs that per-property command objects bring with them.
 */
public final class HistoryStack<T> {

    private final int limit;
    private final Deque<T> undo = new ArrayDeque<>();
    private final Deque<T> redo = new ArrayDeque<>();

    public HistoryStack(int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be > 0: " + limit);
        }
        this.limit = limit;
    }

    /** Records the state before a change. Clears the redo branch, as any new edit invalidates it. */
    public void push(T previousState) {
        undo.push(previousState);
        while (undo.size() > limit) {
            undo.removeLast();
        }
        redo.clear();
    }

    /** Returns the state to restore, or {@code null} when there is nothing to undo. */
    public T undo(T currentState) {
        if (undo.isEmpty()) {
            return null;
        }
        redo.push(currentState);
        return undo.pop();
    }

    public T redo(T currentState) {
        if (redo.isEmpty()) {
            return null;
        }
        undo.push(currentState);
        return redo.pop();
    }

    public boolean canUndo() {
        return !undo.isEmpty();
    }

    public boolean canRedo() {
        return !redo.isEmpty();
    }

    public int undoDepth() {
        return undo.size();
    }

    public int redoDepth() {
        return redo.size();
    }

    public void clear() {
        undo.clear();
        redo.clear();
    }
}
