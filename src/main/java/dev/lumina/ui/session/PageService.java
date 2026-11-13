package dev.lumina.ui.session;

import dev.lumina.ui.LuminaPlugin;
import dev.lumina.ui.api.UiEvent;
import dev.lumina.ui.api.UiEventType;
import dev.lumina.ui.api.UiPage;
import dev.lumina.ui.core.LayoutEngine;
import dev.lumina.ui.compat.SchedulerBridge;
import dev.lumina.ui.core.Settings;
import dev.lumina.ui.core.UiRegistry;
import dev.lumina.ui.input.InteractionBinder;
import dev.lumina.ui.render.ItemRenderer;
import dev.lumina.ui.render.PageProjection;
import dev.lumina.ui.render.RenderSession;
import dev.lumina.ui.render.TextRenderer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Owns per-player page sessions: creating the render session, keeping the frame ticking and turning
 * interaction hits into {@link UiEvent}s.
 */
public final class PageService implements Listener, AutoCloseable {

    private final LuminaPlugin plugin;
    private final UiRegistry registry;
    private final Settings settings;
    private final SchedulerBridge scheduler;
    private final TextRenderer textRenderer;
    private final ItemRenderer itemRenderer;
    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();

    public PageService(
            LuminaPlugin plugin,
            UiRegistry registry,
            Settings settings,
            SchedulerBridge scheduler,
            TextRenderer textRenderer,
            ItemRenderer itemRenderer) {
        this.plugin = plugin;
        this.registry = registry;
        this.settings = settings;
        this.scheduler = scheduler;
        this.textRenderer = textRenderer;
        this.itemRenderer = itemRenderer;
    }

    /** Opens a page for a player at {@code origin}, replacing any page already open. */
    public boolean open(Player player, String pageId, Location origin) {
        UiPage page = registry.page(pageId);
        if (page == null) {
            return false;
        }
        close(player.getUniqueId(), false);

        PageProjection projection = new PageProjection(
                origin, settings.pixelsPerBlock(), settings.viewportWidth(), settings.viewportHeight());
        LayoutEngine layout = new LayoutEngine(settings.viewportWidth(), settings.viewportHeight());
        RenderSession render = new RenderSession(
                plugin, player, page, projection, layout, textRenderer, itemRenderer);
        InteractionBinder binder = new InteractionBinder(plugin, projection, player);
        Session session = new Session(player.getUniqueId(), page, render, binder);

        sessions.put(player.getUniqueId(), session);
        startTicker(session);
        return true;
    }

    public boolean isOpen(UUID playerId) {
        return sessions.containsKey(playerId);
    }

    public String openPageId(UUID playerId) {
        Session session = sessions.get(playerId);
        return session == null ? null : session.page.id();
    }

    public void close(UUID playerId) {
        close(playerId, true);
    }

    public void close(UUID playerId, boolean notify) {
        Session session = sessions.remove(playerId);
        if (session == null) {
            return;
        }
        session.cancel();
        session.renderer.close();
        session.binder.close();
        if (notify) {
            registry.dispatch(new UiEvent(
                    UiEventType.PAGE_CLOSE, session.page.id(), null, playerId, 0f, 0f, 0f, ""));
        }
    }

    /** Closes every session currently showing {@code pageId}. */
    public void closePage(String pageId) {
        List<UUID> affected = new ArrayList<>();
        for (Map.Entry<UUID, Session> entry : sessions.entrySet()) {
            if (entry.getValue().page.id().equals(pageId)) {
                affected.add(entry.getKey());
            }
        }
        for (UUID playerId : affected) {
            close(playerId);
        }
    }

    /** Closes sessions whose page no longer exists after a reload. */
    public void closeMissingPages() {
        List<UUID> stale = new ArrayList<>();
        for (Map.Entry<UUID, Session> entry : sessions.entrySet()) {
            if (registry.page(entry.getValue().page.id()) == null) {
                stale.add(entry.getKey());
            }
        }
        for (UUID playerId : stale) {
            close(playerId);
        }
    }

    public void closeAll() {
        for (UUID playerId : new ArrayList<>(sessions.keySet())) {
            close(playerId, false);
        }
    }

    /** Forces a full redraw of every open session, used after a page is edited. */
    public void invalidatePage(String pageId) {
        for (Session session : sessions.values()) {
            if (session.page.id().equals(pageId)) {
                session.renderer.invalidate();
            }
        }
    }

    /** Perf counters: live element count and last frame delta, summed over sessions. */
    public PerfSnapshot perfSnapshot() {
        int elements = 0;
        int dirty = 0;
        for (Session session : sessions.values()) {
            elements += session.renderer.liveCount();
            dirty += session.renderer.lastDirtySize();
        }
        return new PerfSnapshot(sessions.size(), elements, dirty);
    }

    private void startTicker(Session session) {
        UUID playerId = session.playerId;
        Player player = Bukkit.getPlayer(playerId);
        if (player == null) {
            return;
        }
        long period = Math.max(1L, settings.renderIntervalTicks());
        session.task = scheduler.repeatForPlayer(player, 1L, period, () -> {
            Player online = Bukkit.getPlayer(playerId);
            if (online == null || !online.isOnline()) {
                close(playerId, false);
                return;
            }
            session.renderer.tick();
            session.binder.sync(session.renderer.lastFrame(), true);
        }, () -> close(playerId, false));
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Interaction interaction)) {
            return;
        }
        dispatchOnHitbox(event.getPlayer(), interaction, UiEventType.RIGHT_CLICK);
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onAttackEntity(org.bukkit.event.entity.EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)
                || !(event.getEntity() instanceof Interaction interaction)) {
            return;
        }
        if (dispatchOnHitbox(player, interaction, UiEventType.LEFT_CLICK)) {
            event.setCancelled(true);
        }
    }

    private boolean dispatchOnHitbox(Player player, Interaction interaction, UiEventType type) {
        Session session = sessions.get(player.getUniqueId());
        if (session == null) {
            return false;
        }
        String nodeKey = session.binder.nodeKeyOf(interaction.getEntityId());
        if (nodeKey == null) {
            return false;
        }
        String nodeId = nodeKey.substring(nodeKey.indexOf('/') + 1);
        registry.dispatch(UiEvent.click(type, session.page.id(), nodeId, player.getUniqueId()));
        return true;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        close(event.getPlayer().getUniqueId(), false);
    }

    @Override
    public void close() {
        closeAll();
    }

    private static final class Session {
        private final UUID playerId;
        private final UiPage page;
        private final RenderSession renderer;
        private final InteractionBinder binder;
        private dev.lumina.ui.compat.SchedulerBridge.ScheduledTaskHandle task;

        private Session(UUID playerId, UiPage page, RenderSession renderer, InteractionBinder binder) {
            this.playerId = playerId;
            this.page = page;
            this.renderer = renderer;
            this.binder = binder;
        }

        private void cancel() {
            if (task != null) {
                task.cancel();
                task = null;
            }
        }
    }

    /** Counters exposed by {@code /lumina debug perf}. */
    public record PerfSnapshot(int sessions, int elements, int dirtyNodes) {
    }
}
