package dev.lumina.ui.editor;

import dev.lumina.ui.LuminaPlugin;
import dev.lumina.ui.core.Settings;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Runs deferred work from a single scheduler task instead of one task per feature.
 *
 * <p>Autosave, preview refresh and pack rebuilds are all registered here with a due time, so the
 * server sees one timer per plugin rather than one per open editor.
 */
public final class Autoworker {

    private final LuminaPlugin plugin;
    private final Map<String, Job> jobs = new ConcurrentHashMap<>();
    private int taskId = -1;

    public Autoworker(LuminaPlugin plugin, Settings settings) {
        this.plugin = plugin;
        start(settings.renderIntervalTicks());
    }

    private void start(long periodTicks) {
        if (plugin.scheduler().isFolia()) {
            org.bukkit.Bukkit.getGlobalRegionScheduler()
                    .runAtFixedRate(plugin, scheduled -> runDueJobs(), 1L, Math.max(1L, periodTicks));
        } else {
            taskId = org.bukkit.Bukkit.getScheduler()
                    .runTaskTimer(plugin, this::runDueJobs, 1L, Math.max(1L, periodTicks))
                    .getTaskId();
        }
    }

    /**
     * Schedules {@code runnable} to be executed after {@code delayTicks}, replacing any pending job
     * with the same key. Debouncing keeps autosave from firing on every keystroke.
     */
    public void schedule(String key, long delayTicks, Runnable runnable) {
        jobs.put(key, new Job(System.nanoTime() + delayTicks * 50_000_000L, runnable));
    }

    public void cancel(String key) {
        jobs.remove(key);
    }

    public int pending() {
        return jobs.size();
    }

    private void runDueJobs() {
        if (jobs.isEmpty()) {
            return;
        }
        long now = System.nanoTime();
        for (Map.Entry<String, Job> entry : jobs.entrySet()) {
            Job job = entry.getValue();
            if (now < job.dueNanos) {
                continue;
            }
            jobs.remove(entry.getKey(), job);
            try {
                job.runnable.run();
            } catch (RuntimeException failure) {
                plugin.getLogger().log(Level.WARNING, "Deferred job '" + entry.getKey() + "' failed", failure);
            }
        }
    }

    public void shutdown() {
        jobs.clear();
        if (taskId != -1) {
            org.bukkit.Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }

    private record Job(long dueNanos, Runnable runnable) {
    }
}
