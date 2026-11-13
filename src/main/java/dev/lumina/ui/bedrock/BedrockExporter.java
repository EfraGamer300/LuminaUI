package dev.lumina.ui.bedrock;

import dev.lumina.ui.api.UiNode;
import dev.lumina.ui.api.UiPage;
import java.util.ArrayList;
import java.util.List;

/**
 * Converts a Java page into Bedrock form definitions.
 *
 * <p>The mapping is intentionally lossy and explicit: Bedrock cannot place free-floating elements,
 * so the exporter reports every node it had to fold into a widget, letting the operator know what
 * the Bedrock surface will not show.
 */
public final class BedrockExporter {

    /** Result of an export, including the nodes that could not be represented literally. */
    public record ExportResult(
            String customForm, String simpleForm, List<String> unsupportedNodes, int widgetCount) {
    }

    public ExportResult export(UiPage page) {
        FormBuilder form = new FormBuilder(page.title());
        List<String> unsupported = new ArrayList<>();
        List<String> buttons = new ArrayList<>();

        int widgets = 0;
        for (UiPage.Layer layer : page.layers()) {
            for (UiNode node : layer.nodes()) {
                widgets += appendNode(form, node, buttons, unsupported);
            }
        }

        String customForm = form.buildCustomForm();
        String simpleForm = form.buildSimpleForm(
                buttons.isEmpty() ? List.of("Close") : buttons,
                page.title() + " - " + page.nodeCount() + " element(s)");
        return new ExportResult(customForm, simpleForm, List.copyOf(unsupported), widgets);
    }

    private int appendNode(FormBuilder form, UiNode node, List<String> buttons, List<String> unsupported) {
        if (!node.visible()) {
            return 0;
        }
        if (node instanceof UiNode.TextNode text) {
            form.label(text.text());
            return 1;
        }
        if (node instanceof UiNode.ShapeNode shape) {
            form.label("[ " + shape.material() + " ]");
            return 1;
        }
        if (node instanceof UiNode.ImageNode image) {
            unsupported.add(node.id() + " (image: " + image.texture() + ")");
            form.label("[ image: " + image.texture() + " ]");
            return 1;
        }
        if (node instanceof UiNode.GroupNode group) {
            int count = 0;
            for (UiNode child : group.children()) {
                count += appendNode(form, child, buttons, unsupported);
            }
            return count;
        }
        unsupported.add(node.id() + " (unknown node type)");
        return 0;
    }
}
