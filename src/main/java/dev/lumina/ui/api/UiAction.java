package dev.lumina.ui.api;

import java.util.Map;

/**
 * Executes a side effect in response to a {@link UiEvent}.
 */
@FunctionalInterface
public interface UiAction {

    void execute(ActionContext context);

    /**
     * Runtime context handed to actions. Kept platform-neutral on purpose.
     */
    interface ActionContext {
        UiEvent event();

        /** Resolves a placeholder such as {@code %player_name%} for the acting player. */
        String placeholder(String raw);

        void openPage(String pageId);

        void closePage();

        void message(String miniMessage);

        void command(String commandLine);

        void sound(String key);

        Map<String, String> variables();
    }
}
