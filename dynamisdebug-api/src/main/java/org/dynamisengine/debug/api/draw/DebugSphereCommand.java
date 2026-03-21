package org.dynamisengine.debug.api.draw;

/**
 * Draw a debug wireframe sphere at a world-space position.
 */
public record DebugSphereCommand(
        float cx, float cy, float cz,
        float radius,
        float r, float g, float b,
        float durationSeconds,
        DepthMode depthMode
) implements DebugDrawCommand {

    public static DebugSphereCommand of(float cx, float cy, float cz, float radius,
                                         float r, float g, float b) {
        return new DebugSphereCommand(cx, cy, cz, radius, r, g, b, 0, DepthMode.TESTED);
    }

    public static DebugSphereCommand of(float cx, float cy, float cz, float radius,
                                         float r, float g, float b,
                                         DepthMode depthMode) {
        return new DebugSphereCommand(cx, cy, cz, radius, r, g, b, 0, depthMode);
    }
}
