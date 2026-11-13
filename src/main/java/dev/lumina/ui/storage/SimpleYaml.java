package dev.lumina.ui.storage;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Serialises maps produced by {@link PageCodec} to YAML and parses them back.
 *
 * <p>Supports the subset of YAML the engine needs: nested maps, lists, scalars quoted or plain.
 * Written by hand to avoid pulling a YAML library into the API surface; a malformed file is reported
 * with the offending line.
 */
public final class SimpleYaml {

    private static final String INDENT = "  ";

    public String write(Map<String, Object> root) {
        StringBuilder out = new StringBuilder(512);
        writeMap(out, root, 0);
        return out.toString();
    }

    private void writeMap(StringBuilder out, Map<String, Object> map, int depth) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object value = entry.getValue();
            indent(out, depth);
            out.append(escapeKey(entry.getKey())).append(':');
            if (value instanceof Map<?, ?> nested) {
                out.append('\n');
                writeMap(out, cast(nested), depth + 1);
            } else if (value instanceof List<?> list) {
                if (list.isEmpty()) {
                    out.append(" []\n");
                } else {
                    out.append('\n');
                    writeList(out, list, depth + 1);
                }
            } else {
                out.append(' ').append(scalar(value)).append('\n');
            }
        }
    }

    private void writeList(StringBuilder out, List<?> list, int depth) {
        for (Object item : list) {
            indent(out, depth);
            out.append("- ");
            if (item instanceof Map<?, ?> nested) {
                String block = renderMapInline(cast(nested), depth);
                out.append(block);
            } else if (item instanceof List<?> nestedList) {
                out.append("[]\n");
                writeList(out, nestedList, depth + 1);
            } else {
                out.append(scalar(item)).append('\n');
            }
        }
    }

    /**
     * Maps inside lists are written with the first key on the dash line and the rest aligned under it,
     * which keeps files readable and stays inside the supported grammar.
     */
    private String renderMapInline(Map<String, Object> map, int depth) {
        StringBuilder out = new StringBuilder(128);
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) {
                indent(out, depth + 1);
            }
            first = false;
            out.append(escapeKey(entry.getKey())).append(':');
            Object value = entry.getValue();
            if (value instanceof Map<?, ?> nested) {
                out.append('\n');
                writeMap(out, cast(nested), depth + 2);
            } else if (value instanceof List<?> list) {
                if (list.isEmpty()) {
                    out.append(" []\n");
                } else {
                    out.append('\n');
                    writeList(out, list, depth + 2);
                }
            } else {
                out.append(' ').append(scalar(value)).append('\n');
            }
        }
        return out.toString();
    }

    private static void indent(StringBuilder out, int depth) {
        for (int i = 0; i < depth; i++) {
            out.append(INDENT);
        }
    }

    private static String escapeKey(String key) {
        return key.matches("[A-Za-z0-9_.-]+") ? key : quote(key);
    }

    private static String scalar(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        return quote(String.valueOf(value));
    }

    private static String quote(String text) {
        return '"' + text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + '"';
    }

    public Map<String, Object> read(String yaml) {
        List<Line> lines = preprocess(yaml);
        if (lines.isEmpty()) {
            return new LinkedHashMap<>();
        }
        int[] cursor = {0};
        return readMap(lines, cursor, lines.get(0).indent);
    }

    private Map<String, Object> readMap(List<Line> lines, int[] cursor, int indent) {
        Map<String, Object> result = new LinkedHashMap<>();
        while (cursor[0] < lines.size()) {
            Line line = lines.get(cursor[0]);
            if (line.indent < indent) {
                break;
            }
            if (line.indent > indent) {
                throw error(line, "unexpected indentation");
            }
            if (line.content.startsWith("- ")) {
                break;
            }
            int colon = indexOfColon(line.content);
            if (colon < 0) {
                throw error(line, "expected 'key: value'");
            }
            String key = unquote(line.content.substring(0, colon).trim());
            String rest = line.content.substring(colon + 1).trim();
            cursor[0]++;

            if (rest.isEmpty()) {
                if (cursor[0] >= lines.size() || lines.get(cursor[0]).indent <= indent) {
                    result.put(key, new LinkedHashMap<String, Object>());
                } else {
                    Line next = lines.get(cursor[0]);
                    if (next.content.startsWith("- ")) {
                        result.put(key, readList(lines, cursor, next.indent));
                    } else {
                        result.put(key, readMap(lines, cursor, next.indent));
                    }
                }
            } else if (rest.equals("[]")) {
                result.put(key, new java.util.ArrayList<>());
            } else if (rest.equals("{}")) {
                result.put(key, new LinkedHashMap<String, Object>());
            } else {
                result.put(key, parseScalar(rest));
            }
        }
        return result;
    }

    private List<Object> readList(List<Line> lines, int[] cursor, int indent) {
        List<Object> result = new java.util.ArrayList<>();
        while (cursor[0] < lines.size()) {
            Line line = lines.get(cursor[0]);
            if (line.indent < indent) {
                break;
            }
            if (line.indent > indent || !line.content.startsWith("- ")) {
                break;
            }
            result.add(readListItem(lines, cursor, indent));
        }
        return result;
    }

    private Object readListItem(List<Line> lines, int[] cursor, int indent) {
        Line line = lines.get(cursor[0]);
        String item = line.content.substring(2);
        int colon = indexOfColon(item);
        if (colon < 0) {
            cursor[0]++;
            return parseScalar(item.trim());
        }

        // Map entry starting on the dash line; subsequent keys sit at indent + 2.
        Map<String, Object> map = new LinkedHashMap<>();
        String key = unquote(item.substring(0, colon).trim());
        String rest = item.substring(colon + 1).trim();
        cursor[0]++;
        if (rest.isEmpty()) {
            if (cursor[0] < lines.size() && lines.get(cursor[0]).indent > indent) {
                map.put(key, readMap(lines, cursor, lines.get(cursor[0]).indent));
            } else {
                map.put(key, new LinkedHashMap<String, Object>());
            }
        } else {
            map.put(key, parseScalar(rest));
        }

        int childIndent = indent + 2;
        while (cursor[0] < lines.size()) {
            Line next = lines.get(cursor[0]);
            if (next.indent != childIndent || next.content.startsWith("- ")) {
                break;
            }
            Map<String, Object> extra = readMap(lines, cursor, childIndent);
            map.putAll(extra);
        }
        return map;
    }

    private List<Line> preprocess(String yaml) {
        List<Line> lines = new java.util.ArrayList<>();
        String[] raw = yaml.split("\n", -1);
        for (int i = 0; i < raw.length; i++) {
            String text = raw[i];
            if (text.endsWith("\r")) {
                text = text.substring(0, text.length() - 1);
            }
            String stripped = stripComment(text);
            if (stripped.isBlank()) {
                continue;
            }
            int indent = 0;
            while (indent < stripped.length() && stripped.charAt(indent) == ' ') {
                indent++;
            }
            lines.add(new Line(indent, stripped.substring(indent), i + 1));
        }
        return lines;
    }

    private static String stripComment(String text) {
        boolean inSingle = false;
        boolean inDouble = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\'' && !inDouble) {
                inSingle = !inSingle;
            } else if (c == '"' && !inSingle) {
                inDouble = !inDouble;
            } else if (c == '#' && !inSingle && !inDouble && (i == 0 || Character.isWhitespace(text.charAt(i - 1)))) {
                return text.substring(0, i);
            }
        }
        return text;
    }

    private static Object parseScalar(String raw) {
        String text = raw.trim();
        if (text.isEmpty()) {
            return "";
        }
        if (text.startsWith("\"") && text.endsWith("\"") && text.length() >= 2) {
            return text.substring(1, text.length() - 1)
                    .replace("\\n", "\n")
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\");
        }
        if (text.startsWith("'") && text.endsWith("'") && text.length() >= 2) {
            return text.substring(1, text.length() - 1);
        }
        if (text.equals("null") || text.equals("~")) {
            return null;
        }
        if (text.equalsIgnoreCase("true") || text.equalsIgnoreCase("false")) {
            return Boolean.parseBoolean(text);
        }
        if (text.matches("[-+]?\\d+")) {
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException ignored) {
                return Long.parseLong(text);
            }
        }
        if (text.matches("[-+]?\\d*\\.\\d+([eE][-+]?\\d+)?")) {
            return Float.parseFloat(text);
        }
        return text;
    }

    private static int indexOfColon(String content) {
        boolean inSingle = false;
        boolean inDouble = false;
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '\'' && !inDouble) {
                inSingle = !inSingle;
            } else if (c == '"' && !inSingle) {
                inDouble = !inDouble;
            } else if (c == ':' && !inSingle && !inDouble) {
                if (i + 1 == content.length() || Character.isWhitespace(content.charAt(i + 1))) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static String unquote(String text) {
        if (text.length() >= 2
                && ((text.startsWith("\"") && text.endsWith("\""))
                || (text.startsWith("'") && text.endsWith("'")))) {
            return text.substring(1, text.length() - 1);
        }
        return text;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> cast(Map<?, ?> map) {
        return (Map<String, Object>) map;
    }

    private static PageCodec.PageFormatException error(Line line, String message) {
        return new PageCodec.PageFormatException("line " + line.number + ": " + message);
    }

    private record Line(int indent, String content, int number) {
    }
}
