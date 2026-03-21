package org.dynamisengine.debug.bridge.draw;

import org.dynamisengine.debug.api.draw.DebugBoxCommand;
import org.dynamisengine.debug.api.draw.DepthMode;
import org.dynamisengine.debug.draw.DebugDrawQueue;

/**
 * Submits scene graph debug draw commands from node bounds data.
 *
 * <p>Draws wireframe boxes for node bounds. Uses plain float values
 * since SceneGraph internal types may not be exported.
 */
public final class SceneGraphDebugDraw {

    private SceneGraphDebugDraw() {}

    /**
     * Draw a wireframe box for a scene node's world bounds.
     *
     * @param queue draw queue
     * @param cx    center X
     * @param cy    center Y
     * @param cz    center Z
     * @param hx    half-extent X
     * @param hy    half-extent Y
     * @param hz    half-extent Z
     */
    public static void drawNodeBounds(DebugDrawQueue queue,
                                       float cx, float cy, float cz,
                                       float hx, float hy, float hz) {
        queue.submit(DebugBoxCommand.of(cx, cy, cz, hx, hy, hz,
                0.5f, 0.5f, 1.0f)); // light blue
    }

    /**
     * Draw bounds for a batch of nodes from flat arrays.
     *
     * @param queue   draw queue
     * @param centers interleaved [cx,cy,cz, cx,cy,cz, ...] array
     * @param halves  interleaved [hx,hy,hz, hx,hy,hz, ...] array
     * @param count   number of nodes
     */
    public static void drawNodeBoundsBatch(DebugDrawQueue queue,
                                            float[] centers, float[] halves, int count) {
        for (int i = 0; i < count; i++) {
            int base = i * 3;
            queue.submit(DebugBoxCommand.of(
                    centers[base], centers[base + 1], centers[base + 2],
                    halves[base], halves[base + 1], halves[base + 2],
                    0.5f, 0.5f, 1.0f));
        }
    }
}
