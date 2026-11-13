package dev.lumina.ui.api;

import java.util.function.Consumer;

/**
 * Entry point of the public LuminaUI API. Actions register here and pages are resolved through it.
 */
public interface LuminaApi {

    /** Returns the page registered under {@code id}, or {@code null} when absent. */
    UiPage page(String id);

    /** Registers or replaces a page definition. */
    void registerPage(UiPage page);

    /** Removes a page definition; returns whether anything was removed. */
    boolean unregisterPage(String id);

    /** Subscribes to events of a specific page. Returns a handle used to unsubscribe. */
    AutoCloseable on(String pageId, Consumer<UiEvent> listener);

    /** Available page identifiers, unordered. */
    Iterable<String> pageIds();
}
