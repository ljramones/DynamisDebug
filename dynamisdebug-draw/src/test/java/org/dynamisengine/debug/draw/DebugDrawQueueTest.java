package org.dynamisengine.debug.draw;

import org.dynamisengine.debug.api.draw.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DebugDrawQueueTest {

    private DebugDrawQueue queue;

    @BeforeEach
    void setUp() {
        queue = new DebugDrawQueue();
    }

    @Test
    void singleFrameCommandsClearedOnEndFrame() {
        queue.line(0, 0, 0, 1, 1, 1, 1, 0, 0);
        queue.box(0, 0, 0, 1, 1, 1, 0, 1, 0);
        assertEquals(2, queue.commandCount());

        queue.endFrame(0.016f);
        assertEquals(0, queue.commandCount());
    }

    @Test
    void persistentCommandsSurviveFrames() {
        queue.submit(DebugLineCommand.of(0, 0, 0, 1, 1, 1, 1, 0, 0), 1.0f);
        assertEquals(1, queue.commandCount());

        queue.endFrame(0.5f);
        assertEquals(1, queue.commandCount()); // still alive

        queue.endFrame(0.6f);
        assertEquals(0, queue.commandCount()); // expired
    }

    @Test
    void zeroDurationGoesToSingleFrame() {
        queue.submit(DebugLineCommand.of(0, 0, 0, 1, 1, 1, 1, 0, 0), 0f);
        assertEquals(1, queue.commandCount());
        queue.endFrame(0.016f);
        assertEquals(0, queue.commandCount());
    }

    @Test
    void activeCommandsCombinesBothLists() {
        queue.line(0, 0, 0, 1, 1, 1, 1, 0, 0);
        queue.submit(DebugSphereCommand.of(0, 0, 0, 1, 0, 1, 0), 2.0f);

        var active = queue.activeCommands();
        assertEquals(2, active.size());
        assertInstanceOf(DebugLineCommand.class, active.get(0));
        assertInstanceOf(DebugSphereCommand.class, active.get(1));
    }

    @Test
    void convenienceMethods() {
        queue.line(0, 0, 0, 1, 1, 1, 1, 0, 0);
        queue.box(0, 0, 0, 1, 1, 1, 0, 1, 0);
        queue.sphere(0, 0, 0, 5, 0, 0, 1);
        queue.text("hi", 10, 20, 1, 1, 1);
        queue.worldText("label", 1, 2, 3, 1, 1, 1);

        var cmds = queue.activeCommands();
        assertEquals(5, cmds.size());
        assertInstanceOf(DebugLineCommand.class, cmds.get(0));
        assertInstanceOf(DebugBoxCommand.class, cmds.get(1));
        assertInstanceOf(DebugSphereCommand.class, cmds.get(2));
        assertInstanceOf(DebugTextCommand.class, cmds.get(3));
        assertInstanceOf(DebugTextCommand.class, cmds.get(4));

        assertTrue(((DebugTextCommand) cmds.get(3)).screenSpace());
        assertFalse(((DebugTextCommand) cmds.get(4)).screenSpace());
    }

    @Test
    void activeCommandsIsUnmodifiable() {
        queue.line(0, 0, 0, 1, 1, 1, 1, 0, 0);
        assertThrows(UnsupportedOperationException.class, () ->
                queue.activeCommands().add(DebugLineCommand.of(0, 0, 0, 0, 0, 0, 0, 0, 0)));
    }
}
