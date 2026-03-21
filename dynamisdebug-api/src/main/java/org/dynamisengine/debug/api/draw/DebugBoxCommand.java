package org.dynamisengine.debug.api.draw;

/**
 * Draw a debug wireframe box at a world-space position.
 */
public record DebugBoxCommand(
        float cx, float cy, float cz,
        float halfX, float halfY, float halfZ,
        float r, float g, float b,
        float durationSeconds,
        DepthMode depthMode
) implements DebugDrawCommand {

    public static DebugBoxCommand of(float cx, float cy, float cz,
                                      float halfX, float halfY, float halfZ,
                                      float r, float g, float b) {
        return new DebugBoxCommand(cx, cy, cz, halfX, halfY, halfZ, r, g, b, 0, DepthMode.TESTED);
    }

    public static DebugBoxCommand of(float cx, float cy, float cz,
                                      float halfX, float halfY, float halfZ,
                                      float r, float g, float b,
                                      DepthMode depthMode) {
        return new DebugBoxCommand(cx, cy, cz, halfX, halfY, halfZ, r, g, b, 0, depthMode);
    }
}
