package dev.lumina.ui.compat;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

/**
 * Runs work on whatever thread owns a location.
 *
 * <p>On Folia each region owns its own thread, so touching entities from the global scheduler is
 * illegal. This bridge prefers the region/entity schedulers when available and falls back to the
 * classic Bukkit scheduler on plain Paper, keeping call sites identical on both.
 */
public final class SchedulerBridge {

    private final Plugin plugin;
    private final boolean folia;

    public SchedulerBridge(Plugin plugin, boolean folia) {
        this.plugin = plugin;
        this.folia = folia;
    }

    public static boolean detectFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException missing) {
            return false;
        }
    }

    public boolean isFolia() {
        return folia;
    }

    /** Executes {@code task} on the thread that owns {@code location}. */
    public void atLocation(Location location, Runnable task) {
        if (folia) {
            Bukkit.getRegionScheduler().execute(plugin, location, task);
        } else {
            task.run();
        }
    }

    /** Executes {@code task} on the thread that owns {@code entity}. */
    public void atEntity(Entity entity, Runnable task) {
        if (folia) {
            entity.getScheduler().execute(plugin, task, null, 1L);
        } else {
            task.run();
        }
    }

    /** Runs a repeating task on the region owning {@code location}. */
    public void repeatAtLocation(Location location, long delayTicks, long periodTicks, Runnable task) {
        if (folia) {
            Bukkit.getRegionScheduler()
                    .runAtFixedRate(plugin, location, scheduled -> task.run(), delayTicks, periodTicks);
        } else {
            Bukkit.getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks);
        }
    }

    /** Runs a task on the next global tick. */
    public void nextTick(Runnable task) {
        if (folia) {
            Bukkit.getGlobalRegionScheduler().execute(plugin, task);
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    /** Runs a task off the tick thread. */
    public void async(Runnable task) {
        if (folia) {
            Bukkit.getAsyncScheduler().runNow(plugin, scheduled -> task.run());
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
        }
    }
}
