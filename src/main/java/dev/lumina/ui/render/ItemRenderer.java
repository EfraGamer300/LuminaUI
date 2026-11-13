package dev.lumina.ui.render;

import dev.lumina.ui.api.UiNode;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;

/**
 * Applies image and shape nodes onto item displays, caching {@link ItemStack} prototypes per
 * material so a page with many shapes does not rebuild stacks every frame.
 */
public final class ItemRenderer {

    private final Map<String, Material> materialCache = new HashMap<>();
    private final Map<String, ItemStack> stackCache = new HashMap<>();

    public void apply(ItemDisplay display, UiNode.ShapeNode node, PageProjection projection) {
        display.setItemStack(stackFor(node.material()));
        display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
        display.setBillboard(DisplayEnums.billboard(node.style().billboard()));
        display.setGlowColorOverride(
                node.style().glow() == 0 ? null : org.bukkit.Color.fromARGB(node.style().glow()));

        float scale = projection.toScale(node.bounds());
        display.setTransformation(new org.bukkit.util.Transformation(
                new org.joml.Vector3f(0f, 0f, 0f),
                new org.joml.Quaternionf(),
                new org.joml.Vector3f(scale, scale, scale),
                new org.joml.Quaternionf()));
        display.setInterpolationDuration(2);
        display.setInterpolationDelay(0);
    }

    public void apply(ItemDisplay display, UiNode.ImageNode node, PageProjection projection) {
        display.setItemStack(stackFor(node.texture()));
        display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.GUI);
        display.setBillboard(DisplayEnums.billboard(node.style().billboard()));

        float scaleY = projection.toScale(node.bounds());
        float scaleX = projection.toScaleX(node.bounds());
        display.setTransformation(new org.bukkit.util.Transformation(
                new org.joml.Vector3f(0f, 0f, 0f),
                new org.joml.Quaternionf(),
                new org.joml.Vector3f(scaleX, scaleY, scaleY),
                new org.joml.Quaternionf()));
        display.setInterpolationDuration(2);
        display.setInterpolationDelay(0);
    }

    private ItemStack stackFor(String materialName) {
        return stackCache.computeIfAbsent(materialName, name -> {
            Material material = materialCache.computeIfAbsent(name, Material::matchMaterial);
            if (material == null || !material.isItem()) {
                material = Material.WHITE_CONCRETE;
            }
            return new ItemStack(material);
        });
    }

    /** Drops cached stacks, used after a resource reload so stale materials are not reused. */
    public void invalidate() {
        materialCache.clear();
        stackCache.clear();
    }

    /** Number of cached prototypes, exposed for the perf counters. */
    public int cacheSize() {
        return stackCache.size();
    }
}
