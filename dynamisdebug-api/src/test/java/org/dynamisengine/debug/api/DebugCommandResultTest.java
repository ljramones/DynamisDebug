package org.dynamisengine.debug.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DebugCommandResultTest {

    @Test
    void okFactory() {
        var result = DebugCommandResult.ok("done");
        assertTrue(result.success());
        assertEquals("done", result.message());
    }

    @Test
    void errorFactory() {
        var result = DebugCommandResult.error("bad");
        assertFalse(result.success());
        assertEquals("bad", result.message());
    }
}
