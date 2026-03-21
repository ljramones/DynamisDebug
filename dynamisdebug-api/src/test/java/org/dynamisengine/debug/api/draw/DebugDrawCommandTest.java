package org.dynamisengine.debug.api.draw;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DebugDrawCommandTest {

    @Test
    void lineFactory() {
        var cmd = DebugLineCommand.of(0, 0, 0, 1, 2, 3, 1, 0, 0);
        assertEquals(0, cmd.durationSeconds());
        assertEquals(1f, cmd.x2());
        assertEquals(2f, cmd.y2());
        assertEquals(3f, cmd.z2());
        assertInstanceOf(DebugDrawCommand.class, cmd);
    }

    @Test
    void boxFactory() {
        var cmd = DebugBoxCommand.of(1, 2, 3, 0.5f, 0.5f, 0.5f, 0, 1, 0);
        assertEquals(1f, cmd.cx());
        assertEquals(0.5f, cmd.halfX());
        assertEquals(0, cmd.durationSeconds());
    }

    @Test
    void sphereFactory() {
        var cmd = DebugSphereCommand.of(5, 5, 5, 2, 0, 0, 1);
        assertEquals(2f, cmd.radius());
        assertEquals(5f, cmd.cx());
    }

    @Test
    void textScreenSpace() {
        var cmd = DebugTextCommand.screen("FPS", 10, 20, 1, 1, 1);
        assertTrue(cmd.screenSpace());
        assertEquals(0f, cmd.z());
    }

    @Test
    void textWorldSpace() {
        var cmd = DebugTextCommand.world("label", 1, 2, 3, 1, 1, 1);
        assertFalse(cmd.screenSpace());
        assertEquals(3f, cmd.z());
    }
}
