package org.dynamisengine.debug.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DebugMarkerTest {

    @Test
    void convenienceFactories() {
        assertEquals(DebugCategory.ENGINE, DebugMarker.engine("tick").category());
        assertEquals(DebugCategory.RENDERING, DebugMarker.rendering("geometry").category());
        assertEquals(DebugCategory.PHYSICS, DebugMarker.physics("step").category());
        assertEquals(DebugCategory.AUDIO, DebugMarker.audio("mix").category());
    }

    @Test
    void ofFactory() {
        var m = DebugMarker.of("custom", DebugCategory.AI);
        assertEquals("custom", m.name());
        assertEquals(DebugCategory.AI, m.category());
    }
}
