package org.dynamisengine.debug.api.draw;

/**
 * Draw debug text at a world-space or screen-space position.
 */
public record DebugTextCommand(
        String text,
        float x, float y, float z,
        float r, float g, float b,
        boolean screenSpace,
        float durationSeconds
) implements DebugDrawCommand {

    /** Screen-space text (x,y in pixels). */
    public static DebugTextCommand screen(String text, float x, float y,
                                           float r, float g, float b) {
        return new DebugTextCommand(text, x, y, 0, r, g, b, true, 0);
    }

    /** World-space text. */
    public static DebugTextCommand world(String text, float x, float y, float z,
                                          float r, float g, float b) {
        return new DebugTextCommand(text, x, y, z, r, g, b, false, 0);
    }
}
