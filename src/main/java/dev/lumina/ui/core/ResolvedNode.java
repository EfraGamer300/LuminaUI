package dev.lumina.ui.core;

import dev.lumina.ui.api.UiNode;

/**
 * A node transformed into absolute page coordinates, ready for rendering or hit testing.
 */
public record ResolvedNode(
        UiNode node,
        String layerId,
        int depth,
        float x,
        float y,
        float width,
        float height) {

    public boolean contains(float pointX, float pointY) {
        return pointX >= x && pointX <= x + width && pointY >= y && pointY <= y + height;
    }
}
