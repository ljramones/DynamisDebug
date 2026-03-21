package org.dynamisengine.debug.bridge.draw;

import org.dynamisengine.debug.api.draw.DebugBoxCommand;
import org.dynamisengine.debug.api.draw.DepthMode;
import org.dynamisengine.debug.draw.DebugDrawQueue;

/**
 * Submits terrain debug draw commands for chunk bounds visualization.
 *
 * <p>Draws wireframe boxes for terrain chunks, color-coded by LOD level.
 */
public final class TerrainDebugDraw {

    private TerrainDebugDraw() {}

    // LOD colors: green (fine) → red (coarse)
    private static final float[][] LOD_COLORS = {
            {0, 1, 0},       // LOD 0 — green
            {0.5f, 1, 0},    // LOD 1 — yellow-green
            {1, 1, 0},       // LOD 2 — yellow
            {1, 0.5f, 0},    // LOD 3 — orange
            {1, 0, 0},       // LOD 4+ — red
    };

    /**
     * Draw a terrain chunk bounds box, color-coded by LOD level.
     *
     * @param queue    draw queue
     * @param cx       chunk center X (world space)
     * @param cy       chunk center Y (world space)
     * @param cz       chunk center Z (world space)
     * @param hx       chunk half-width X
     * @param hy       chunk half-height Y
     * @param hz       chunk half-depth Z
     * @param lodLevel LOD level (0 = finest)
     */
    public static void drawChunkBounds(DebugDrawQueue queue,
                                        float cx, float cy, float cz,
                                        float hx, float hy, float hz,
                                        int lodLevel) {
        int colorIdx = Math.min(lodLevel, LOD_COLORS.length - 1);
        float[] c = LOD_COLORS[colorIdx];
        queue.submit(DebugBoxCommand.of(cx, cy, cz, hx, hy, hz, c[0], c[1], c[2]));
    }
}
