package org.dynamisengine.debug.bridge.telemetry;

import org.dynamisengine.debug.api.DebugCategory;
import org.dynamisengine.debug.bridge.telemetry.ThreadingTelemetryAdapter.ThreadingSnapshot;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ThreadingTelemetryAdapterTest {

    private final ThreadingTelemetryAdapter adapter = new ThreadingTelemetryAdapter();

    @Test
    void subsystemNameAndCategory() {
        assertEquals("threading", adapter.subsystemName());
        assertEquals(DebugCategory.ENGINE, adapter.category());
    }

    @Test
    void adaptHealthyState() {
        var snapshot = new ThreadingSnapshot(
                8, 4,
                1000, 990, 0, 50_000.0, 12,
                2, 50, 0,
                1, 30,
                1, 0, 2.5);
        var debug = adapter.adapt(snapshot, 1);

        assertEquals(8.0, debug.metrics().get("threading.activeWorkers"));
        assertEquals(4.0, debug.metrics().get("threading.totalPools"));
        assertEquals(1000.0, debug.metrics().get("threading.eventBus.published"));
        assertEquals(2.0, debug.metrics().get("threading.cognition.queueDepth"));
        assertFalse(debug.flags().get("cognitionQueueBacklog"));
        assertFalse(debug.flags().get("eventBusDeadLetters"));
        assertFalse(debug.flags().get("gpuUploadBacklog"));
    }

    @Test
    void flagsDetectProblems() {
        var snapshot = new ThreadingSnapshot(
                4, 3,
                500, 480, 15, 100_000.0, 8,
                12, 100, 5,
                3, 20,
                2, 8, 1.0);
        var debug = adapter.adapt(snapshot, 1);

        assertTrue(debug.flags().get("cognitionQueueBacklog"));
        assertTrue(debug.flags().get("eventBusDeadLetters"));
        assertTrue(debug.flags().get("gpuUploadBacklog"));
        assertEquals(5.0, debug.metrics().get("threading.cognition.timeouts"));
    }

    @Test
    void metricsHaveAllKeys() {
        var snapshot = new ThreadingSnapshot(4, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        var metrics = adapter.extractMetrics(snapshot);
        assertTrue(metrics.containsKey("threading.activeWorkers"));
        assertTrue(metrics.containsKey("threading.eventBus.published"));
        assertTrue(metrics.containsKey("threading.cognition.queueDepth"));
        assertTrue(metrics.containsKey("threading.navigation.pendingRequests"));
        assertTrue(metrics.containsKey("threading.gpuUpload.inflight"));
    }
}
