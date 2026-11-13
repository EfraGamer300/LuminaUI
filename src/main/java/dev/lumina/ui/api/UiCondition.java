package dev.lumina.ui.api;

/**
 * Predicate gating whether a node or action is active for a given player.
 */
@FunctionalInterface
public interface UiCondition {

    boolean test(UiAction.ActionContext context);
}
