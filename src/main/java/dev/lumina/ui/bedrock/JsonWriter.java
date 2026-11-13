package dev.lumina.ui.bedrock;

import java.util.List;
import java.util.Map;

/**
 * Minimal JSON writer for Bedrock form payloads.
 *
 * <p>Hand-written to keep the plugin dependency-free. It only supports the value shapes forms need:
 * maps, lists, strings, numbers, booleans and null.
 */
public final class JsonWriter {

    public String write(Object value) {
        StringBuilder out = new StringBuilder(256);
        writeValue(out, value);
        return out.toString();
    }

    public String writePretty(Object value) {
        StringBuilder out = new StringBuilder(512);
        writeValuePretty(out, value, 0);
        return out.toString();
    }

    private void writeValue(StringBuilder out, Object value) {
        if (value == null) {
            out.append("null");
        } else if (value instanceof Map<?, ?> map) {
            out.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) {
                    out.append(',');
                }
                first = false;
                writeString(out, String.valueOf(entry.getKey()));
                out.append(':');
                writeValue(out, entry.getValue());
            }
            out.append('}');
        } else if (value instanceof List<?> list) {
            out.append('[');
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) {
                    out.append(',');
                }
                writeValue(out, list.get(i));
            }
            out.append(']');
        } else if (value instanceof Number || value instanceof Boolean) {
            out.append(value);
        } else {
            writeString(out, String.valueOf(value));
        }
    }

    private void writeValuePretty(StringBuilder out, Object value, int depth) {
        if (value instanceof Map<?, ?> map && !map.isEmpty()) {
            out.append("{\n");
            int index = 0;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                indent(out, depth + 1);
                writeString(out, String.valueOf(entry.getKey()));
                out.append(": ");
                writeValuePretty(out, entry.getValue(), depth + 1);
                if (++index < map.size()) {
                    out.append(',');
                }
                out.append('\n');
            }
            indent(out, depth);
            out.append('}');
        } else if (value instanceof List<?> list && !list.isEmpty()) {
            out.append("[\n");
            for (int i = 0; i < list.size(); i++) {
                indent(out, depth + 1);
                writeValuePretty(out, list.get(i), depth + 1);
                if (i + 1 < list.size()) {
                    out.append(',');
                }
                out.append('\n');
            }
            indent(out, depth);
            out.append(']');
        } else {
            writeValue(out, value);
        }
    }

    private static void indent(StringBuilder out, int depth) {
        for (int i = 0; i < depth; i++) {
            out.append("  ");
        }
    }

    private static void writeString(StringBuilder out, String text) {
        out.append('"');
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        out.append('"');
    }
}
