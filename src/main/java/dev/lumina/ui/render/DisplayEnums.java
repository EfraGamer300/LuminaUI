package dev.lumina.ui.render;

import dev.lumina.ui.api.BillboardMode;
import org.bukkit.entity.Display;

/**
 * Maps engine-facing enums onto Bukkit display enums. Kept in one place so the rest of the render
 * layer never mentions platform enums directly.
 */
public final class DisplayEnums {

    private DisplayEnums() {
    }

    public static Display.Billboard billboard(BillboardMode mode) {
        return switch (mode) {
            case FIXED -> Display.Billboard.FIXED;
            case VERTICAL -> Display.Billboard.VERTICAL;
            case HORIZONTAL -> Display.Billboard.HORIZONTAL;
            case CENTER -> Display.Billboard.CENTER;
        };
    }
}
