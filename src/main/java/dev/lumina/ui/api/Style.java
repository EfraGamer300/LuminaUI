package dev.lumina.ui.api;

/**
 * Immutable visual attributes shared by every node type.
 */
public final class Style {

    private static final Style DEFAULT = builder().build();

    private final int background;
    private final int accent;
    private final float opacity;
    private final boolean shadow;
    private final int glow;
    private final BillboardMode billboard;

    private Style(Builder builder) {
        this.background = builder.background;
        this.accent = builder.accent;
        this.opacity = builder.opacity;
        this.shadow = builder.shadow;
        this.glow = builder.glow;
        this.billboard = builder.billboard;
    }

    public static Style defaults() {
        return DEFAULT;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** ARGB colour, {@code 0} meaning fully transparent. */
    public int background() {
        return background;
    }

    /** ARGB accent colour used by borders, highlights and shape fills. */
    public int accent() {
        return accent;
    }

    public float opacity() {
        return opacity;
    }

    public boolean shadow() {
        return shadow;
    }

    /** ARGB glow colour, {@code 0} disables the glow. */
    public int glow() {
        return glow;
    }

    public BillboardMode billboard() {
        return billboard;
    }

    public Builder toBuilder() {
        return new Builder()
                .background(background)
                .accent(accent)
                .opacity(opacity)
                .shadow(shadow)
                .glow(glow)
                .billboard(billboard);
    }

    public static final class Builder {
        private int background;
        private int accent = 0xFFFFFFFF;
        private float opacity = 1f;
        private boolean shadow = true;
        private int glow;
        private BillboardMode billboard = BillboardMode.CENTER;

        private Builder() {
        }

        public Builder background(int argb) {
            this.background = argb;
            return this;
        }

        public Builder accent(int argb) {
            this.accent = argb;
            return this;
        }

        public Builder opacity(float value) {
            if (value < 0f || value > 1f) {
                throw new IllegalArgumentException("opacity must be within [0,1]: " + value);
            }
            this.opacity = value;
            return this;
        }

        public Builder shadow(boolean value) {
            this.shadow = value;
            return this;
        }

        public Builder glow(int argb) {
            this.glow = argb;
            return this;
        }

        public Builder billboard(BillboardMode mode) {
            this.billboard = mode;
            return this;
        }

        public Style build() {
            return new Style(this);
        }
    }
}
