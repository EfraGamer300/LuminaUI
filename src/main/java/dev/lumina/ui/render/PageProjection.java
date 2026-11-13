package dev.lumina.ui.render;

import dev.lumina.ui.api.ElementBounds;
import org.bukkit.Location;
import org.bukkit.World;

/**
 * Pixel-to-world projection for a page anchored at a location.
 *
 * <p>A page is defined on a virtual grid (the {@link dev.lumina.ui.core.LayoutEngine} viewport) and
 * placed in the world by scaling that grid. Keeping the conversion in one class makes HUD, world
 * placement and editor previews agree on the same geometry; the arithmetic itself lives in
 * {@link ProjectionMath} so it can be tested without a server.
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
        return origin.clone().add(
                ProjectionMath.blockX(pageX, viewportWidth, pixelsPerBlock),
                ProjectionMath.blockY(pageY, viewportHeight, pixelsPerBlock),
                0d);
    }

    /**
     * World location of an element's centre.
     *
     * <p>Display entities render their content centred on the entity position, so elements are
     * placed by centre and the transformation only carries scale. Translation stays zero, which
     * keeps billboarding from rotating an offset along with the element.
     */
    public Location toWorldCentre(ElementBounds bounds) {
        return origin.clone().add(
                ProjectionMath.centreBlockX(bounds, viewportWidth, pixelsPerBlock),
                ProjectionMath.centreBlockY(bounds, viewportHeight, pixelsPerBlock),
                0d);
    }

    /** Scale factor that makes a block-sized display cover the given bounds. */
    public float toScale(ElementBounds bounds) {
        return ProjectionMath.scaleY(bounds, pixelsPerBlock);
    }

    /** Horizontal scale keeping aspect ratio for image elements. */
    public float toScaleX(ElementBounds bounds) {
        return ProjectionMath.scaleX(bounds, pixelsPerBlock);
    }

    public World world() {
        return origin.getWorld();
    }
}
