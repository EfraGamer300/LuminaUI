package dev.lumina.ui.bedrock;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds Bedrock form payloads.
 *
 * <p>Bedrock forms are a fixed set of widgets, so a Java page cannot be reproduced element by
 * element. The builder keeps the page's intent: text becomes labels, toggles stay toggles, numeric
 * properties become sliders, choices become dropdowns and every interactive node gets a button.
 */
public final class FormBuilder {

    private final String title;
    private final List<Object> elements = new ArrayList<>();
    private final JsonWriter json = new JsonWriter();

    public FormBuilder(String title) {
        this.title = title;
    }

    public FormBuilder label(String text) {
        Map<String, Object> element = new LinkedHashMap<>();
        element.put("type", "label");
        element.put("text", text);
        elements.add(element);
        return this;
    }

    public FormBuilder toggle(String text, boolean defaultValue) {
        Map<String, Object> element = new LinkedHashMap<>();
        element.put("type", "toggle");
        element.put("text", text);
        element.put("default", defaultValue);
        elements.add(element);
        return this;
    }

    public FormBuilder slider(String text, int min, int max, int step, int defaultValue) {
        Map<String, Object> element = new LinkedHashMap<>();
        element.put("type", "slider");
        element.put("text", text);
        element.put("min", min);
        element.put("max", max);
        element.put("step", Math.max(1, step));
        element.put("default", defaultValue);
        elements.add(element);
        return this;
    }

    public FormBuilder dropdown(String text, List<String> options, int defaultIndex) {
        if (options.isEmpty()) {
            throw new IllegalArgumentException("dropdown needs at least one option");
        }
        Map<String, Object> element = new LinkedHashMap<>();
        element.put("type", "dropdown");
        element.put("text", text);
        element.put("options", List.copyOf(options));
        element.put("default", Math.max(0, Math.min(defaultIndex, options.size() - 1)));
        elements.add(element);
        return this;
    }

    public FormBuilder input(String text, String placeholder, String defaultValue) {
        Map<String, Object> element = new LinkedHashMap<>();
        element.put("type", "input");
        element.put("text", text);
        element.put("placeholder", placeholder);
        element.put("default", defaultValue);
        elements.add(element);
        return this;
    }

    /** Adds a button for a simple form. Buttons replace the element list entirely. */
    public FormBuilder buttons(List<String> labels) {
        Map<String, Object> element = new LinkedHashMap<>();
        element.put("type", "buttons");
        element.put("labels", List.copyOf(labels));
        elements.add(element);
        return this;
    }

    public int elementCount() {
        return elements.size();
    }

    /** Serialises as a {@code custom_form}. */
    public String buildCustomForm() {
        Map<String, Object> form = new LinkedHashMap<>();
        form.put("type", "custom_form");
        form.put("title", title);
        form.put("content", List.copyOf(elements));
        return json.writePretty(form);
    }

    /** Serialises as a {@code simple_form} with one button per action. */
    public String buildSimpleForm(List<String> buttonLabels, String content) {
        Map<String, Object> form = new LinkedHashMap<>();
        form.put("type", "simple_form");
        form.put("title", title);
        form.put("content", content == null ? "" : content);
        form.put("buttons", toButtonObjects(buttonLabels));
        return json.writePretty(form);
    }

    /** Serialises the elements as a modal (two buttons). */
    public String buildModalForm(String content, String yes, String no) {
        Map<String, Object> form = new LinkedHashMap<>();
        form.put("type", "modal");
        form.put("title", title);
        form.put("content", content);
        form.put("button1", yes);
        form.put("button2", no);
        return json.writePretty(form);
    }

    private static List<Object> toButtonObjects(List<String> labels) {
        List<Object> buttons = new ArrayList<>(labels.size());
        for (String label : labels) {
            Map<String, Object> button = new LinkedHashMap<>();
            button.put("text", label);
            buttons.add(button);
        }
        return buttons;
    }
}
