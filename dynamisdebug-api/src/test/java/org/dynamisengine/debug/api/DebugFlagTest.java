package org.dynamisengine.debug.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DebugFlagTest {

    @Test
    void toggleFlipsState() {
        var flag = DebugFlag.of("wireframe", false);
        var toggled = flag.toggled();
        assertTrue(toggled.enabled());
        assertFalse(toggled.toggled().enabled());
    }

    @Test
    void factoryCreatesCorrectly() {
        var flag = DebugFlag.of("showColliders", true);
        assertEquals("showColliders", flag.name());
        assertTrue(flag.enabled());
    }
}
