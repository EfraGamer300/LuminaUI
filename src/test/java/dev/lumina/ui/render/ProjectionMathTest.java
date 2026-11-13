package dev.lumina.ui.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.lumina.ui.api.ElementBounds;
import org.junit.jupiter.api.Test;

/**
 * Covers the page-to-world arithmetic. This is the code that decides where every element lands, so
 * it is tested directly rather than through a server.
 */
class ProjectionMathTest {

    private static final float WIDTH = 400f;
    private static final float HEIGHT = 200f;
    private static final float DENSITY = 100f;

    @Test
    void viewportCentreMapsToZeroOffset() {
        assertEquals(0d, ProjectionMath.blockX(200f, WIDTH, DENSITY), 1e-6);
        assertEquals(0d, ProjectionMath.blockY(100f, HEIGHT, DENSITY), 1e-6);
    }

    @Test
    void horizontalPositionConvertsPixelsToBlocks() {
        assertEquals(1d, ProjectionMath.blockX(300f, WIDTH, DENSITY), 1e-6);
        assertEquals(-1d, ProjectionMath.blockX(100f, WIDTH, DENSITY), 1e-6);
    }

    @Test
    void pageSpaceGrowsDownwardsSoWorldYIsFlipped() {
        assertEquals(-0.5d, ProjectionMath.blockY(150f, HEIGHT, DENSITY), 1e-6);
        assertEquals(0.5d, ProjectionMath.blockY(50f, HEIGHT, DENSITY), 1e-6);
    }

    @Test
    void elementCentreAccountsForHalfSize() {
        ElementBounds bounds = new ElementBounds(100f, 50f, 100f, 40f);

        // Centre X is 100 + 50 = 150, which is half a block left of the viewport centre (200).
        assertEquals(-0.5d, ProjectionMath.centreBlockX(bounds, WIDTH, DENSITY), 1e-6);
        // Centre Y is 50 + 20 = 70, i.e. 30px above the middle of a 200px viewport.
        assertEquals(0.3d, ProjectionMath.centreBlockY(bounds, HEIGHT, DENSITY), 1e-6);
    }

    @Test
    void scaleIsProportionalToSize() {
        assertEquals(1f, ProjectionMath.scaleY(new ElementBounds(0f, 0f, 50f, 100f), DENSITY), 1e-6);
        assertEquals(0.5f, ProjectionMath.scaleY(new ElementBounds(0f, 0f, 50f, 50f), DENSITY), 1e-6);
        assertEquals(2f, ProjectionMath.scaleX(new ElementBounds(0f, 0f, 200f, 50f), DENSITY), 1e-6);
    }
}
