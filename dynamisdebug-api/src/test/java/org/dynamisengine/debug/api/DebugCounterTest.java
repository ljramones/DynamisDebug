package org.dynamisengine.debug.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DebugCounterTest {

    @Test
    void incrementAndDecrement() {
        var counter = DebugCounter.of("draws", DebugCategory.RENDERING);
        assertEquals(0, counter.value());
        assertEquals(1, counter.incremented().value());
        assertEquals(-1, counter.decremented().value());
    }

    @Test
    void withValueReplacesValue() {
        var counter = DebugCounter.of("fps", DebugCategory.ENGINE);
        assertEquals(42, counter.withValue(42).value());
    }

    @Test
    void incrementPreservesNameAndCategory() {
        var counter = new DebugCounter("entities", DebugCategory.ECS, 10);
        var inc = counter.incremented();
        assertEquals("entities", inc.name());
        assertEquals(DebugCategory.ECS, inc.category());
        assertEquals(11, inc.value());
    }
}
