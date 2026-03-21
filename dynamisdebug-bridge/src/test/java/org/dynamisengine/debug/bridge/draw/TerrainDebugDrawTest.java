package org.dynamisengine.debug.bridge.draw;

import org.dynamisengine.debug.api.draw.DebugBoxCommand;
import org.dynamisengine.debug.draw.DebugDrawQueue;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TerrainDebugDrawTest {

    @Test
    void drawChunkBoundsLod0IsGreen() {
        var queue = new DebugDrawQueue();
        TerrainDebugDraw.drawChunkBounds(queue, 0, 0, 0, 32, 10, 32, 0);

        var cmds = queue.activeCommands();
        assertEquals(1, cmds.size());
        var box = (DebugBoxCommand) cmds.get(0);
        assertEquals(0f, box.r());
        assertEquals(1f, box.g());
        assertEquals(0f, box.b());
    }

    @Test
    void drawChunkBoundsLod4IsRed() {
        var queue = new DebugDrawQueue();
        TerrainDebugDraw.drawChunkBounds(queue, 0, 0, 0, 128, 10, 128, 4);

        var box = (DebugBoxCommand) queue.activeCommands().get(0);
        assertEquals(1f, box.r());
        assertEquals(0f, box.g());
        assertEquals(0f, box.b());
    }

    @Test
    void highLodClampedToMaxColor() {
        var queue = new DebugDrawQueue();
        TerrainDebugDraw.drawChunkBounds(queue, 0, 0, 0, 256, 10, 256, 10);
        // Should not throw, uses LOD 4 color (red)
        assertEquals(1, queue.commandCount());
    }
}
