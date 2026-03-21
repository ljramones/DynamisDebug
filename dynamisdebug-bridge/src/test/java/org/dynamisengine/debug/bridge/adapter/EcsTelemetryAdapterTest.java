package org.dynamisengine.debug.bridge.adapter;

import org.dynamisengine.debug.api.DebugCategory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EcsTelemetryAdapterTest {

    private final EcsTelemetryAdapter adapter = new EcsTelemetryAdapter();

    @Test
    void subsystemNameAndCategory() {
        assertEquals("ecs", adapter.subsystemName());
        assertEquals(DebugCategory.ECS, adapter.category());
    }

    @Test
    void adaptStableState() {
        var snapshot = new EcsTelemetryAdapter.EcsTelemetrySnapshot(100, 0, 0, 42);
        var debug = adapter.adapt(snapshot, 1);

        assertEquals(100.0, debug.metrics().get("entityCount"));
        assertEquals(0.0, debug.metrics().get("createdCount"));
        assertEquals(0.0, debug.metrics().get("destroyedCount"));
        assertEquals(42.0, debug.metrics().get("tickNumber"));
        assertFalse(debug.flags().get("hasChurn"));
        assertTrue(debug.text().contains("entities=100"));
    }

    @Test
    void adaptWithChurn() {
        var snapshot = new EcsTelemetryAdapter.EcsTelemetrySnapshot(50, 5, 3, 10);
        var debug = adapter.adapt(snapshot, 1);

        assertEquals(50.0, debug.metrics().get("entityCount"));
        assertEquals(5.0, debug.metrics().get("createdCount"));
        assertEquals(3.0, debug.metrics().get("destroyedCount"));
        assertTrue(debug.flags().get("hasChurn"));
        assertTrue(debug.text().contains("created=5"));
        assertTrue(debug.text().contains("destroyed=3"));
    }

    @Test
    void extractMetricsConsistent() {
        var snapshot = new EcsTelemetryAdapter.EcsTelemetrySnapshot(200, 10, 5, 99);
        var metrics = adapter.extractMetrics(snapshot);
        assertEquals(200.0, metrics.get("entityCount"));
        assertEquals(10.0, metrics.get("createdCount"));
        assertEquals(5.0, metrics.get("destroyedCount"));
        assertEquals(99.0, metrics.get("tickNumber"));
    }

    @Test
    void emptyWorldSnapshot() {
        var snapshot = new EcsTelemetryAdapter.EcsTelemetrySnapshot(0, 0, 0, 0);
        var debug = adapter.adapt(snapshot, 1);
        assertEquals(0.0, debug.metrics().get("entityCount"));
        assertFalse(debug.flags().get("hasChurn"));
    }
}
