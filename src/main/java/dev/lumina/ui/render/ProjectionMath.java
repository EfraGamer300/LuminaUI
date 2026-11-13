package dev.lumina.ui.render;

import dev.lumina.ui.api.ElementBounds;

/**
 * Pure projection maths: page space to block offsets, and element sizes to display scales.
 *
 * <p>Kept free of Bukkit types so the numbers that decide where every element lands can be unit
 * tested without a server.
 */
public final class ProjectionMath {

    private ProjectionMath() {
    }

    /** Page-space X to block offset from the page origin centre. */
    public static double blockX(float pageX, float viewportWidth, float pixelsPerBlock) {
        return (pageX - viewportWidth * 0.5f) / pixelsPerBlock;
    }

    /**
     * Page-space Y to block offset. Page space grows downwards while world Y grows upwards, so the
     * sign is flipped here in exactly one place.
     */
    public static double blockY(float pageY, float viewportHeight, float pixelsPerBlock) {
        return (viewportHeight * 0.5f - pageY) / pixelsPerBlock;
    }

    /** Vertical scale that makes a one-block display cover the element height. */
    public static float scaleY(ElementBounds bounds, float pixelsPerBlock) {
        return bounds.height() / pixelsPerBlock;
    }

    /** Horizontal scale that makes a one-block display cover the element width. */
    public static float scaleX(ElementBounds bounds, float pixelsPerBlock) {
        return bounds.width() / pixelsPerBlock;
    }

    /** World X offset of an element's centre. */
    public static double centreBlockX(ElementBounds bounds, float viewportWidth, float pixelsPerBlock) {
        return blockX(bounds.x() + bounds.width() * 0.5f, viewportWidth, pixelsPerBlock);
    }

    /** World Y offset of an element's centre. */
    public static double centreBlockY(ElementBounds bounds, float viewportHeight, float pixelsPerBlock) {
        return blockY(bounds.y() + bounds.height() * 0.5f, viewportHeight, pixelsPerBlock);
    }
}
