package dev.lumina.ui.api;

/**
 * A single element of a page. All node kinds are immutable value types; use the {@code with*}
 * methods to derive modified copies.
 */
public sealed interface UiNode
        permits UiNode.TextNode, UiNode.ImageNode, UiNode.ShapeNode, UiNode.GroupNode {

    /** Unique, stable identifier inside a page. Never contains whitespace. */
    String id();

    ElementBounds bounds();

    Anchor anchor();

    Style style();

    boolean visible();

    UiNode withBounds(ElementBounds bounds);

    UiNode withStyle(Style style);

    UiNode withVisible(boolean visible);

    /**
     * Text node. Rendered by a text display entity.
     */
    record TextNode(
            String id,
            ElementBounds bounds,
            Anchor anchor,
            Style style,
            boolean visible,
            String text,
            float scale,
            int lineWidth) implements UiNode {

        public TextNode {
            validateId(id);
            if (text == null) {
                throw new IllegalArgumentException("text must not be null");
            }
            if (!(scale > 0f)) {
                throw new IllegalArgumentException("scale must be > 0: " + scale);
            }
            if (lineWidth <= 0) {
                throw new IllegalArgumentException("lineWidth must be > 0: " + lineWidth);
            }
        }

        @Override
        public TextNode withBounds(ElementBounds value) {
            return new TextNode(id, value, anchor, style, visible, text, scale, lineWidth);
        }

        @Override
        public TextNode withStyle(Style value) {
            return new TextNode(id, bounds, anchor, value, visible, text, scale, lineWidth);
        }

        @Override
        public TextNode withVisible(boolean value) {
            return new TextNode(id, bounds, anchor, style, value, text, scale, lineWidth);
        }

        public TextNode withText(String value) {
            return new TextNode(id, bounds, anchor, style, visible, value, scale, lineWidth);
        }

        public TextNode withScale(float value) {
            return new TextNode(id, bounds, anchor, style, visible, text, value, lineWidth);
        }
    }

    /**
     * Image node, backed by a texture reference resolved by the render layer.
     */
    record ImageNode(
            String id,
            ElementBounds bounds,
            Anchor anchor,
            Style style,
            boolean visible,
            String texture,
            String tint,
            boolean smooth) implements UiNode {

        public ImageNode {
            validateId(id);
            if (texture == null || texture.isBlank()) {
                throw new IllegalArgumentException("texture must not be blank");
            }
        }

        @Override
        public ImageNode withBounds(ElementBounds value) {
            return new ImageNode(id, value, anchor, style, visible, texture, tint, smooth);
        }

        @Override
        public ImageNode withStyle(Style value) {
            return new ImageNode(id, bounds, anchor, value, visible, texture, tint, smooth);
        }

        @Override
        public ImageNode withVisible(boolean value) {
            return new ImageNode(id, bounds, anchor, style, value, texture, tint, smooth);
        }
    }

    /**
     * Solid or outlined shape, rendered with a block/item display entity.
     */
    record ShapeNode(
            String id,
            ElementBounds bounds,
            Anchor anchor,
            Style style,
            boolean visible,
            String material,
            float cornerRadius) implements UiNode {

        public ShapeNode {
            validateId(id);
            if (material == null || material.isBlank()) {
                throw new IllegalArgumentException("material must not be blank");
            }
            if (cornerRadius < 0f) {
                throw new IllegalArgumentException("cornerRadius must be >= 0");
            }
        }

        @Override
        public ShapeNode withBounds(ElementBounds value) {
            return new ShapeNode(id, value, anchor, style, visible, material, cornerRadius);
        }

        @Override
        public ShapeNode withStyle(Style value) {
            return new ShapeNode(id, bounds, anchor, value, visible, material, cornerRadius);
        }

        @Override
        public ShapeNode withVisible(boolean value) {
            return new ShapeNode(id, bounds, anchor, style, value, material, cornerRadius);
        }
    }

    /**
     * Logical container. Children are positioned relative to the group origin.
     */
    record GroupNode(
            String id,
            ElementBounds bounds,
            Anchor anchor,
            Style style,
            boolean visible,
            java.util.List<UiNode> children) implements UiNode {

        public GroupNode {
            validateId(id);
            children = java.util.List.copyOf(children == null ? java.util.List.of() : children);
        }

        @Override
        public GroupNode withBounds(ElementBounds value) {
            return new GroupNode(id, value, anchor, style, visible, children);
        }

        @Override
        public GroupNode withStyle(Style value) {
            return new GroupNode(id, bounds, anchor, value, visible, children);
        }

        @Override
        public GroupNode withVisible(boolean value) {
            return new GroupNode(id, bounds, anchor, style, value, children);
        }
    }

    private static void validateId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        for (int i = 0; i < id.length(); i++) {
            if (Character.isWhitespace(id.charAt(i))) {
                throw new IllegalArgumentException("id must not contain whitespace: " + id);
            }
        }
    }
}
