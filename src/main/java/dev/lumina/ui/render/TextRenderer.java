package dev.lumina.ui.render;

import dev.lumina.ui.api.UiNode;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Color;
import org.bukkit.entity.TextDisplay;

/**
 * Applies text node data onto a {@link TextDisplay}.
 *
 * <p>Markup is parsed once per update and cached on the node's rendered state; identical text is
 * never re-parsed, which keeps a static page cheap.
 */
public final class TextRenderer {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    public Component parse(String miniMessage) {
        return MINI_MESSAGE.deserialize(miniMessage == null ? "" : miniMessage);
    }

    public void apply(TextDisplay display, UiNode.TextNode node, PageProjection projection) {
        display.text(parse(node.text()));
        display.setLineWidth(node.lineWidth());
        display.setShadowed(node.style().shadow());
        display.setAlignment(TextDisplay.TextAlignment.CENTER);
        display.setBillboard(DisplayEnums.billboard(node.style().billboard()));

        if (node.style().background() != 0) {
            display.setDefaultBackground(false);
            display.setBackgroundColor(Color.fromARGB(node.style().background()));
        } else {
            display.setDefaultBackground(true);
        }

        display.setTextOpacity(opacityByte(node.style().opacity()));

        if (node.style().glow() != 0) {
            display.setGlowColorOverride(Color.fromARGB(node.style().glow()));
        } else {
            display.setGlowColorOverride(null);
        }

        float scale = node.scale() * projection.toScale(node.bounds());
        display.setTransformation(new org.bukkit.util.Transformation(
                projection.toTranslation(node.bounds()),
                new org.joml.Quaternionf(),
                new org.joml.Vector3f(scale, scale, scale),
                new org.joml.Quaternionf()));
        display.setInterpolationDuration(2);
        display.setInterpolationDelay(0);
    }

    private static byte opacityByte(float opacity) {
        int clamped = Math.round(opacity * 255f);
        if (clamped < 0) {
            clamped = 0;
        } else if (clamped > 255) {
            clamped = 255;
        }
        return (byte) clamped;
    }
}
