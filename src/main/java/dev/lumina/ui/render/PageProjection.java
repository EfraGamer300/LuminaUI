package dev.lumina.ui.render;

import dev.lumina.ui.api.ElementBounds;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;
import org.joml.Vector3f;

/**
 * Pixel-to-world projection for a page anchored at a location.
 *
 * <p>A page is defined on a virtual grid (the {@link dev.lumina.ui.core.LayoutEngine} viewport) and
 * placed in the world by scaling that grid. Keeping the conversion in one class makes HUD, world
 * placement and editor previews agree on the same geometry.
 */
public final class PageProjection {

    private final Location origin;
    private final float pixelsPerBlock;
    private final float viewportWidth;
    private final float viewportHeight;

    public PageProjection(Location origin, float pixelsPerBlock, float viewportWidth, float viewportHeight) {
        if (!(pixelsPerBlock > 0f)) {
            throw new IllegalArgumentException("pixelsPerBlock must be > 0");
        }
        this.origin = origin.clone();
        this.pixelsPerBlock = pixelsPerBlock;
        this.viewportWidth = viewportWidth;
        this.viewportHeight = viewportHeight;
    }

    public Location origin() {
        return origin.clone();
    }

    public float pixelsPerBlock() {
        return pixelsPerBlock;
    }

    /** Converts a page-space point into a world location relative to the origin. */
    public Location toWorld(float pageX, float pageY) {
        double blockX = (pageX - viewportWidth * 0.5f) / pixelsPerBlock;
        double blockY = (viewportHeight * 0.5f - pageY) / pixelsPerBlock;
        return origin.clone().add(blockX, blockY, 0d);
    }

    /** Computes the display translation so the element is centred on its bounds. */
    public Vector3f toTranslation(ElementBounds bounds) {
        float centreX = bounds.x() + bounds.width() * 0.5f - viewportWidth * 0.5f;
        float centreY = viewportHeight * 0.5f - (bounds.y() + bounds.height() * 0.5f);
        return new Vector3f(centreX / pixelsPerBlock, centreY / pixelsPerBlock, 0f);
    }

    /** Scale factor that makes a block-sized display cover the given bounds. */
    public float toScale(ElementBounds bounds) {
        return bounds.height() / pixelsPerBlock;
    }

    /** Horizontal scale keeping aspect ratio for image elements. */
    public float toScaleX(ElementBounds bounds) {
        return bounds.width() / pixelsPerBlock;
    }

    public World world() {
        return origin.getWorld();
    }

    /** Removes every display entity belonging to a page render, used when a page closes. */
    public static void removeAll(Iterable<Entity> entities) {
        for (Entity entity : entities) {
            if (entity instanceof TextDisplay || entity instanceof ItemDisplay) {
                entity.remove();
            }
        }
    }
}
