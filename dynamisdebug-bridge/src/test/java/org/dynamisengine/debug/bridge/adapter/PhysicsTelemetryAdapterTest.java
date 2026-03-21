package org.dynamisengine.debug.bridge.adapter;

import org.dynamisengine.debug.api.DebugCategory;
import org.dynamisengine.physics.api.world.PhysicsStats;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PhysicsTelemetryAdapterTest {

    private final PhysicsTelemetryAdapter adapter = new PhysicsTelemetryAdapter();

    @Test
    void subsystemNameAndCategory() {
        assertEquals("physics", adapter.subsystemName());
        assertEquals(DebugCategory.PHYSICS, adapter.category());
    }

    @Test
    void adaptFullStats() {
        var stats = new PhysicsStats(4.2f, 500, 300, 200, 50, 0, 0, 0, 0, 0);
        var debug = adapter.adapt(stats, 1);

        assertEquals(500.0, debug.metrics().get("bodyCount"));
        assertEquals(300.0, debug.metrics().get("activeBodyCount"));
        assertEquals(200.0, debug.metrics().get("sleepingBodyCount"));
        assertEquals(50.0, debug.metrics().get("constraintCount"));
        assertEquals(4.2, debug.metrics().get("stepTimeMs"), 0.01);
        assertTrue(debug.flags().get("hasSleepingBodies"));
        assertFalse(debug.flags().get("stepTimeHigh"));
        assertTrue(debug.text().contains("bodies=500"));
        assertTrue(debug.text().contains("active=300"));
    }

    @Test
    void highStepTimeFlag() {
        var stats = new PhysicsStats(20.0f, 100, 80, 20, 10, 0, 0, 0, 0, 0);
        var debug = adapter.adapt(stats, 1);
        assertTrue(debug.flags().get("stepTimeHigh"));
    }

    @Test
    void noSleepingBodiesFlag() {
        var stats = new PhysicsStats(2.0f, 50, 50, 0, 5, 0, 0, 0, 0, 0);
        var debug = adapter.adapt(stats, 1);
        assertFalse(debug.flags().get("hasSleepingBodies"));
    }
}
