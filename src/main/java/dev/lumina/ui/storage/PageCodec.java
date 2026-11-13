package dev.lumina.ui.storage;

import dev.lumina.ui.api.Anchor;
import dev.lumina.ui.api.BillboardMode;
import dev.lumina.ui.api.ElementBounds;
import dev.lumina.ui.api.Style;
import dev.lumina.ui.api.UiNode;
import dev.lumina.ui.api.UiPage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Converts pages to and from plain maps. The map form is what gets written to YAML, keeping the
 * storage format decoupled from the YAML library.
 */
public final class PageCodec {

    public Map<String, Object> encode(UiPage page) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("schema-version", page.schemaVersion());
        root.put("title", page.title());
        root.put("scale", page.scale());
        root.put("persistent", page.persistent());

        List<Object> layers = new ArrayList<>(page.layers().size());
        for (UiPage.Layer layer : page.layers()) {
            Map<String, Object> encodedLayer = new LinkedHashMap<>();
            encodedLayer.put("id", layer.id());
            encodedLayer.put("depth", layer.depth());
            encodedLayer.put("interactive", layer.interactive());

            List<Object> nodes = new ArrayList<>(layer.nodes().size());
            for (UiNode node : layer.nodes()) {
                nodes.add(encodeNode(node));
            }
            encodedLayer.put("nodes", nodes);
            layers.add(encodedLayer);
        }
        root.put("layers", layers);
        return root;
    }

    private Map<String, Object> encodeNode(UiNode node) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", node.id());
        map.put("type", typeOf(node));
        map.put("bounds", encodeBounds(node.bounds()));
        map.put("anchor", node.anchor().name());
        map.put("visible", node.visible());
        map.put("style", encodeStyle(node.style()));

        if (node instanceof UiNode.TextNode text) {
            map.put("text", text.text());
            map.put("scale", text.scale());
            map.put("line-width", text.lineWidth());
        } else if (node instanceof UiNode.ImageNode image) {
            map.put("texture", image.texture());
            map.put("tint", image.tint());
            map.put("smooth", image.smooth());
        } else if (node instanceof UiNode.ShapeNode shape) {
            map.put("material", shape.material());
            map.put("corner-radius", shape.cornerRadius());
        } else if (node instanceof UiNode.GroupNode group) {
            List<Object> children = new ArrayList<>(group.children().size());
            for (UiNode child : group.children()) {
                children.add(encodeNode(child));
            }
            map.put("children", children);
        }
        return map;
    }

    private static Map<String, Object> encodeBounds(ElementBounds bounds) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("x", bounds.x());
        map.put("y", bounds.y());
        map.put("width", bounds.width());
        map.put("height", bounds.height());
        return map;
    }

    private static Map<String, Object> encodeStyle(Style style) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("background", style.background());
        map.put("accent", style.accent());
        map.put("opacity", style.opacity());
        map.put("shadow", style.shadow());
        map.put("glow", style.glow());
        map.put("billboard", style.billboard().name());
        return map;
    }

    public UiPage decode(String id, Map<String, Object> root) {
        if (root == null) {
            throw new PageFormatException("page '" + id + "' is empty");
        }
        int schemaVersion = intValue(root, "schema-version", UiPage.CURRENT_SCHEMA_VERSION);
        String title = stringValue(root, "title", id);
        float scale = floatValue(root, "scale", 1f);
        boolean persistent = booleanValue(root, "persistent", false);

        List<UiPage.Layer> layers = new ArrayList<>();
        for (Object rawLayer : listValue(root, "layers")) {
            if (!(rawLayer instanceof Map<?, ?> layerMap)) {
                throw new PageFormatException("page '" + id + "' has a non-map layer entry");
            }
            String layerId = stringValue(cast(layerMap), "id", "default");
            float depth = floatValue(cast(layerMap), "depth", 0f);
            boolean interactive = booleanValue(cast(layerMap), "interactive", true);

            List<UiNode> nodes = new ArrayList<>();
            for (Object rawNode : listValue(cast(layerMap), "nodes")) {
                if (!(rawNode instanceof Map<?, ?> nodeMap)) {
                    throw new PageFormatException("page '" + id + "' has a non-map node entry");
                }
                nodes.add(decodeNode(cast(nodeMap)));
            }
            layers.add(new UiPage.Layer(layerId, depth, interactive, nodes));
        }

        return new UiPage(id, title, schemaVersion, scale, persistent, layers);
    }

    private UiNode decodeNode(Map<String, Object> map) {
        String id = requiredString(map, "id");
        String type = stringValue(map, "type", "text");
        ElementBounds bounds = decodeBounds(map.get("bounds"));
        Anchor anchor = enumValue(Anchor.class, map, "anchor", Anchor.TOP_LEFT);
        boolean visible = booleanValue(map, "visible", true);
        Style style = decodeStyle(map.get("style"));

        return switch (type) {
            case "text" -> new UiNode.TextNode(
                    id,
                    bounds,
                    anchor,
                    style,
                    visible,
                    stringValue(map, "text", ""),
                    floatValue(map, "scale", 1f),
                    intValue(map, "line-width", 200));
            case "image" -> new UiNode.ImageNode(
                    id,
                    bounds,
                    anchor,
                    style,
                    visible,
                    requiredString(map, "texture"),
                    stringValue(map, "tint", ""),
                    booleanValue(map, "smooth", true));
            case "shape" -> new UiNode.ShapeNode(
                    id,
                    bounds,
                    anchor,
                    style,
                    visible,
                    stringValue(map, "material", "minecraft:white_concrete"),
                    floatValue(map, "corner-radius", 0f));
            case "group" -> {
                List<UiNode> children = new ArrayList<>();
                for (Object rawChild : listValue(map, "children")) {
                    if (!(rawChild instanceof Map<?, ?> childMap)) {
                        throw new PageFormatException("group '" + id + "' has a non-map child");
                    }
                    children.add(decodeNode(cast(childMap)));
                }
                yield new UiNode.GroupNode(id, bounds, anchor, style, visible, children);
            }
            default -> throw new PageFormatException("unknown node type '" + type + "' on node '" + id + "'");
        };
    }

    private static ElementBounds decodeBounds(Object raw) {
        if (!(raw instanceof Map<?, ?> map)) {
            throw new PageFormatException("missing bounds");
        }
        Map<String, Object> bounds = cast(map);
        return new ElementBounds(
                floatValue(bounds, "x", 0f),
                floatValue(bounds, "y", 0f),
                floatValue(bounds, "width", 1f),
                floatValue(bounds, "height", 1f));
    }

    private static Style decodeStyle(Object raw) {
        if (!(raw instanceof Map<?, ?> map)) {
            return Style.defaults();
        }
        Map<String, Object> style = cast(map);
        return Style.builder()
                .background(intValue(style, "background", 0))
                .accent(intValue(style, "accent", 0xFFFFFFFF))
                .opacity(floatValue(style, "opacity", 1f))
                .shadow(booleanValue(style, "shadow", true))
                .glow(intValue(style, "glow", 0))
                .billboard(enumValue(BillboardMode.class, style, "billboard", BillboardMode.CENTER))
                .build();
    }

    private static String typeOf(UiNode node) {
        if (node instanceof UiNode.ImageNode) {
            return "image";
        }
        if (node instanceof UiNode.ShapeNode) {
            return "shape";
        }
        if (node instanceof UiNode.GroupNode) {
            return "group";
        }
        return "text";
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> cast(Map<?, ?> map) {
        return (Map<String, Object>) map;
    }

    private static List<Object> listValue(Map<String, Object> map, String key) {
        Object raw = map.get(key);
        if (raw == null) {
            return List.of();
        }
        if (!(raw instanceof List<?> list)) {
            throw new PageFormatException("'" + key + "' must be a list");
        }
        return new ArrayList<>(list);
    }

    private static String requiredString(Map<String, Object> map, String key) {
        String value = stringValue(map, key, null);
        if (value == null) {
            throw new PageFormatException("missing required field '" + key + "'");
        }
        return value;
    }

    private static String stringValue(Map<String, Object> map, String key, String fallback) {
        Object raw = map.get(key);
        return raw == null ? fallback : String.valueOf(raw);
    }

    private static int intValue(Map<String, Object> map, String key, int fallback) {
        Object raw = map.get(key);
        if (raw instanceof Number number) {
            return number.intValue();
        }
        if (raw instanceof String text) {
            try {
                return Integer.parseInt(text.trim());
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private static float floatValue(Map<String, Object> map, String key, float fallback) {
        Object raw = map.get(key);
        if (raw instanceof Number number) {
            return number.floatValue();
        }
        if (raw instanceof String text) {
            try {
                return Float.parseFloat(text.trim());
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private static boolean booleanValue(Map<String, Object> map, String key, boolean fallback) {
        Object raw = map.get(key);
        if (raw instanceof Boolean bool) {
            return bool;
        }
        if (raw instanceof String text) {
            return Boolean.parseBoolean(text.trim());
        }
        return fallback;
    }

    private static <E extends Enum<E>> E enumValue(
            Class<E> type, Map<String, Object> map, String key, E fallback) {
        Object raw = map.get(key);
        if (raw == null) {
            return fallback;
        }
        try {
            return Enum.valueOf(type, String.valueOf(raw).trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    /** Thrown when a stored page cannot be interpreted. */
    public static final class PageFormatException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        public PageFormatException(String message) {
            super(message);
        }
    }
}
