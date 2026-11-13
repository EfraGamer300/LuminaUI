package dev.lumina.ui.api;

/**
 * Point of a node that is aligned to the node's anchor position.
 */
public enum Anchor {
    TOP_LEFT(0f, 0f),
    TOP_CENTER(0.5f, 0f),
    TOP_RIGHT(1f, 0f),
    CENTER_LEFT(0f, 0.5f),
    CENTER(0.5f, 0.5f),
    CENTER_RIGHT(1f, 0.5f),
    BOTTOM_LEFT(0f, 1f),
    BOTTOM_CENTER(0.5f, 1f),
    BOTTOM_RIGHT(1f, 1f);

    private final float horizontalFactor;
    private final float verticalFactor;

    Anchor(float horizontalFactor, float verticalFactor) {
        this.horizontalFactor = horizontalFactor;
        this.verticalFactor = verticalFactor;
    }

    /** Offset applied to the node origin so the chosen anchor lands on the target point. */
    public float offsetX(float width) {
        return -width * horizontalFactor;
    }

    public float offsetY(float height) {
        return -height * verticalFactor;
    }
}
