package dev.lumina.ui.core;

import dev.lumina.ui.api.LuminaApi;
import dev.lumina.ui.api.UiEvent;
import dev.lumina.ui.api.UiPage;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Thread-safe page registry and event bus.
 *
 * <p>Pages are stored as immutable snapshots, so a reader always sees a consistent page even while
 * an editor replaces it. Listeners are copy-on-write to keep dispatch allocation-free on the hot
 * path.
 */
public final class UiRegistry implements LuminaApi {

    private final Map<String, UiPage> pages = new ConcurrentHashMap<>();
    private final Map<String, CopyOnWriteArrayList<Consumer<UiEvent>>> listeners = new ConcurrentHashMap<>();

    @Override
    public UiPage page(String id) {
        if (id == null) {
            return null;
        }
        return pages.get(id);
    }

    @Override
    public void registerPage(UiPage page) {
        if (page == null) {
            throw new IllegalArgumentException("page must not be null");
        }
        pages.put(page.id(), page);
    }

    @Override
    public boolean unregisterPage(String id) {
        if (id == null) {
            return false;
        }
        return pages.remove(id) != null;
    }

    @Override
    public AutoCloseable on(String pageId, Consumer<UiEvent> listener) {
        if (pageId == null || pageId.isBlank()) {
            throw new IllegalArgumentException("pageId must not be blank");
        }
        if (listener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }
        CopyOnWriteArrayList<Consumer<UiEvent>> bucket =
                listeners.computeIfAbsent(pageId, key -> new CopyOnWriteArrayList<>());
        bucket.add(listener);
        return () -> bucket.remove(listener);
    }

    @Override
    public Iterable<String> pageIds() {
        return List.copyOf(pages.keySet());
    }

    /** Number of registered pages. */
    public int pageCount() {
        return pages.size();
    }

    /** Dispatches an event to every listener registered for its page. */
    public void dispatch(UiEvent event) {
        CopyOnWriteArrayList<Consumer<UiEvent>> bucket = listeners.get(event.pageId());
        if (bucket == null) {
            return;
        }
        for (Consumer<UiEvent> listener : bucket) {
            listener.accept(event);
        }
    }
}
