package dev.lumina.ui.editor;

import dev.lumina.ui.LuminaPlugin;
import dev.lumina.ui.api.UiPage;
import dev.lumina.ui.core.Settings;
import dev.lumina.ui.core.UiRegistry;
import dev.lumina.ui.session.PageService;
import dev.lumina.ui.storage.YamlPageStore;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of open {@link EditorSession}s plus the autosave policy.
 *
 * <p>Saving is debounced through the {@link Autoworker}: a burst of edits results in a single disk
 * write once the player stops typing, and flushing on disable guarantees nothing is lost on restart.
 */
public final class EditorService {

    private final LuminaPlugin plugin;
    private final UiRegistry registry;
    private final PageService pageService;
    private final YamlPageStore pageStore;
    private final java.util.function.Supplier<Settings> settings;
    private final Map<UUID, EditorSession> sessions = new ConcurrentHashMap<>();
    private final Map<UUID, String> pendingSaveKeys = new ConcurrentHashMap<>();

    public EditorService(
            LuminaPlugin plugin,
            UiRegistry registry,
            PageService pageService,
            YamlPageStore pageStore,
            java.util.function.Supplier<Settings> settings) {
        this.plugin = plugin;
        this.registry = registry;
        this.pageService = pageService;
        this.pageStore = pageStore;
        this.settings = settings;
    }

    /** Opens an editing session for a page, sharing the session when already open. */
    public EditorSession open(UUID playerId, UiPage page) {
        EditorSession existing = sessions.get(playerId);
        if (existing != null && existing.pageId().equals(page.id())) {
            return existing;
        }
        if (existing != null) {
            flush(existing);
        }
        EditorSession session = new EditorSession(playerId, page, settings.get().editorUndoLimit());
        sessions.put(playerId, session);
        return session;
    }

    public EditorSession session(UUID playerId) {
        return sessions.get(playerId);
    }

    /** Records that the page changed and schedules a debounced autosave. */
    public void markChanged(EditorSession session) {
        pageService.invalidatePage(session.pageId());
        registry.registerPage(session.page());
        if (!settings.get().editorAutosave()) {
            return;
        }
        UUID playerId = session.playerId();
        pendingSaveKeys.put(playerId, "save:" + playerId);
        plugin.autoworker().schedule(
                "save:" + playerId,
                Math.max(1, settings.get().editorAutosaveSeconds() * 20L),
                () -> flush(playerId));
    }

    /** Persists a session immediately. */
    public boolean flush(UUID playerId) {
        pendingSaveKeys.remove(playerId);
        EditorSession session = sessions.get(playerId);
        return session != null && flush(session);
    }

    public boolean flush(EditorSession session) {
        if (!session.dirty()) {
            return false;
        }
        try {
            pageStore.save(session.page());
            registry.registerPage(session.page());
            session.markSaved();
            return true;
        } catch (IOException | IllegalStateException failure) {
            plugin.reportFailure("saving page " + session.pageId(), failure);
            return false;
        }
    }

    public void close(UUID playerId) {
        EditorSession session = sessions.remove(playerId);
        pendingSaveKeys.remove(playerId);
        if (session != null) {
            flush(session);
            plugin.autoworker().cancel("save:" + playerId);
        }
    }

    /** Saves every open session; called on plugin shutdown. */
    public void flushAll() {
        for (EditorSession session : sessions.values()) {
            flush(session);
        }
    }

    public int openCount() {
        return sessions.size();
    }
}
