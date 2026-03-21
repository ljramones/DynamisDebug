package org.dynamisengine.debug.api.draw;

import java.util.List;

/**
 * SPI contract for renderers that consume debug draw commands.
 *
 * <p>DynamisLightEngine (or any renderer) implements this interface
 * to render world-space debug primitives. The renderer is responsible
 * for translating commands into actual GPU draw calls.
 *
 * <p>Doctrine: DynamisDebug owns draw intent; the renderer owns rendering.
 */
public interface DebugDrawConsumer {

    /**
     * Render the given debug draw commands for this frame.
     *
     * <p>Called once per frame after scene rendering, before UI overlay.
     * The consumer should handle depth mode per-command.
     *
     * @param commands the active draw commands for this frame
     */
    void renderDebugDraw(List<DebugDrawCommand> commands);

    /** Whether this consumer is currently enabled. */
    boolean isDebugDrawEnabled();

    /** Toggle debug draw on/off. */
    void setDebugDrawEnabled(boolean enabled);
}
