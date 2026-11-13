package dev.lumina.ui.render;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;

/**
 * Reuses display entities belonging to one rendered page instead of spawning a new entity per frame.
 *
 * <p>Entities are parked far below the world and kept invisible when idle, so recycling never pops a
 * stale element on screen. Spawning happens only when the pool runs dry, which makes a steady page
 * allocation-free.
 */
public final class DisplayPool implements AutoCloseable {

    private static final double PARK_Y = -512d;

    private final Location spawnAnchor;
    private final Deque<TextDisplay> idleText = new ArrayDeque<>();
    private final Deque<ItemDisplay> idleItem = new ArrayDeque<>();
    private final List<Display> allocated = new ArrayList<>();

    public DisplayPool(Location spawnAnchor) {
        this.spawnAnchor = spawnAnchor.clone();
    }

    public TextDisplay acquireText() {
        TextDisplay display = idleText.pollFirst();
        if (display != null && display.isValid()) {
            return display;
        }
        TextDisplay created = spawnAnchor.getWorld().spawn(parked(), TextDisplay.class);
        created.setVisibleByDefault(false);
        allocated.add(created);
        return created;
    }

    public ItemDisplay acquireItem() {
        ItemDisplay display = idleItem.pollFirst();
        if (display != null && display.isValid()) {
            return display;
        }
        ItemDisplay created = spawnAnchor.getWorld().spawn(parked(), ItemDisplay.class);
        created.setVisibleByDefault(false);
        allocated.add(created);
        return created;
    }

    /**
     * Parks an entity and drops its data so a recycled instance never carries stale state.
     */
    public void release(Display display) {
        if (display == null || !display.isValid()) {
            return;
        }
        display.setVisibleByDefault(false);
        display.setTransformation(new org.bukkit.util.Transformation(
                new org.joml.Vector3f(0f, 0f, 0f),
                new org.joml.Quaternionf(),
                new org.joml.Vector3f(0f, 0f, 0f),
                new org.joml.Quaternionf()));
        display.teleport(parked());

        if (display instanceof TextDisplay text) {
            text.text(net.kyori.adventure.text.Component.empty());
            idleText.addLast(text);
        } else if (display instanceof ItemDisplay item) {
            item.setItemStack(null);
            idleItem.addLast(item);
        }
    }

    public int idleCount() {
        return idleText.size() + idleItem.size();
    }

    public int allocatedCount() {
        return allocated.size();
    }

    private Location parked() {
        Location location = spawnAnchor.clone();
        location.setY(PARK_Y);
        return location;
    }

    @Override
    public void close() {
        for (Display display : allocated) {
            if (display.isValid()) {
                display.remove();
            }
        }
        allocated.clear();
        idleText.clear();
        idleItem.clear();
    }
}
