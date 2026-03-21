package org.dynamisengine.debug.api.draw;

/**
 * Draw a debug line between two world-space points.
 */
public record DebugLineCommand(
        float x1, float y1, float z1,
        float x2, float y2, float z2,
        float r, float g, float b,
        float durationSeconds
) implements DebugDrawCommand {

    /** Single-frame line (duration 0). */
    public static DebugLineCommand of(float x1, float y1, float z1,
                                       float x2, float y2, float z2,
                                       float r, float g, float b) {
        return new DebugLineCommand(x1, y1, z1, x2, y2, z2, r, g, b, 0);
    }
}
