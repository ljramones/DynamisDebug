package org.dynamisengine.debug.draw;

import org.dynamisengine.debug.api.draw.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Accumulates debug draw commands per frame.
 *
 * Renderers consume this queue each frame and clear it.
 * Commands with duration > 0 persist across frames until expired.
 */
public final class DebugDrawQueue {

    private final List<TimedCommand> commands = new ArrayList<>();
    private final List<TimedCommand> persistent = new ArrayList<>();

    /** Submit a single-frame draw command. */
    public void submit(DebugDrawCommand command) {
        commands.add(new TimedCommand(command, 0));
    }

    /** Submit a draw command that persists for the given duration. */
    public void submit(DebugDrawCommand command, float durationSeconds) {
        if (durationSeconds <= 0) {
            commands.add(new TimedCommand(command, 0));
        } else {
            persistent.add(new TimedCommand(command, durationSeconds));
        }
    }

    // --- Convenience methods ---

    public void line(float x1, float y1, float z1, float x2, float y2, float z2,
                     float r, float g, float b) {
        submit(DebugLineCommand.of(x1, y1, z1, x2, y2, z2, r, g, b));
    }

    public void box(float cx, float cy, float cz, float hx, float hy, float hz,
                    float r, float g, float b) {
        submit(DebugBoxCommand.of(cx, cy, cz, hx, hy, hz, r, g, b));
    }

    public void sphere(float cx, float cy, float cz, float radius,
                       float r, float g, float b) {
        submit(DebugSphereCommand.of(cx, cy, cz, radius, r, g, b));
    }

    public void text(String text, float x, float y, float r, float g, float b) {
        submit(DebugTextCommand.screen(text, x, y, r, g, b));
    }

    public void worldText(String text, float x, float y, float z,
                           float r, float g, float b) {
        submit(DebugTextCommand.world(text, x, y, z, r, g, b));
    }

    /** Get all active commands for this frame (single-frame + surviving persistent). */
    public List<DebugDrawCommand> activeCommands() {
        List<DebugDrawCommand> result = new ArrayList<>(commands.size() + persistent.size());
        for (var tc : commands) result.add(tc.command);
        for (var tc : persistent) result.add(tc.command);
        return Collections.unmodifiableList(result);
    }

    /** Advance time: clear single-frame commands, decay persistent ones. */
    public void endFrame(float dt) {
        commands.clear();
        persistent.removeIf(tc -> {
            tc.remaining -= dt;
            return tc.remaining <= 0;
        });
    }

    public int commandCount() { return commands.size() + persistent.size(); }

    private static final class TimedCommand {
        final DebugDrawCommand command;
        float remaining;
        TimedCommand(DebugDrawCommand command, float remaining) {
            this.command = command;
            this.remaining = remaining;
        }
    }
}
