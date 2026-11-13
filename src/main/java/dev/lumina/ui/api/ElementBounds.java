package dev.lumina.ui.api;

/**
 * Rectangle in virtual UI coordinates. Origin is the top-left corner, Y grows downwards.
 */
public record ElementBounds(float x, float y, float width, float height) {

    public ElementBounds {
        if (width < 0f || height < 0f) {
            throw new IllegalArgumentException("width and height must be >= 0: " + width + "x" + height);
        }
        if (!Float.isFinite(x) || !Float.isFinite(y)) {
            throw new IllegalArgumentException("origin must be finite: " + x + "," + y);
        }
    }

    public static ElementBounds of(float x, float y, float width, float height) {
        return new ElementBounds(x, y, width, height);
    }

    public float maxX() {
        return x + width;
    }

    public float maxY() {
        return y + height;
    }

    public float centerX() {
        return x + width * 0.5f;
    }

    public float centerY() {
        return y + height * 0.5f;
    }

    public boolean contains(float pointX, float pointY) {
        return pointX >= x && pointX <= maxX() && pointY >= y && pointY <= maxY();
    }

    public ElementBounds translate(float dx, float dy) {
        return new ElementBounds(x + dx, y + dy, width, height);
    }

    public ElementBounds scale(float factor) {
        if (factor < 0f) {
            throw new IllegalArgumentException("factor must be >= 0");
        }
        return new ElementBounds(x * factor, y * factor, width * factor, height * factor);
    }

    public ElementBounds withSize(float newWidth, float newHeight) {
        return new ElementBounds(x, y, newWidth, newHeight);
    }
}
