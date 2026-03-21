package org.dynamisengine.debug.bridge.adapter;

import org.dynamisengine.debug.api.DebugCategory;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AiTelemetryAdapterTest {

    private final AiTelemetryAdapter adapter = new AiTelemetryAdapter();

    @Test
    void subsystemNameAndCategory() {
        assertEquals("ai", adapter.subsystemName());
        assertEquals(DebugCategory.AI, adapter.category());
    }

    @Test
    void adaptHealthyState() {
        var snapshot = new AiTelemetryAdapter.AiTelemetrySnapshot(
                20, 3.5, 15, 0, 0, 2, 100, 0, 1.2);
        var debug = adapter.adapt(snapshot, 1);

        assertEquals(20.0, debug.metrics().get("agentCount"));
        assertEquals(3.5, debug.metrics().get("tickElapsedMs"));
        assertEquals(15.0, debug.metrics().get("taskCount"));
        assertEquals(100.0, debug.metrics().get("totalInferenceCalls"));
        assertFalse(debug.flags().get("hasDegradedTasks"));
        assertFalse(debug.flags().get("hasSkippedTasks"));
        assertFalse(debug.flags().get("inferenceQueuePressure"));
        assertTrue(debug.text().contains("agents=20"));
    }

    @Test
    void adaptDegradedState() {
        var snapshot = new AiTelemetryAdapter.AiTelemetrySnapshot(
                50, 12.0, 40, 10, 5, 8, 500, 3, 5.0);
        var debug = adapter.adapt(snapshot, 1);

        assertTrue(debug.flags().get("hasDegradedTasks"));
        assertTrue(debug.flags().get("hasSkippedTasks"));
        assertTrue(debug.flags().get("inferenceQueuePressure"));
        assertEquals(10.0, debug.metrics().get("degradedTaskCount"));
        assertEquals(5.0, debug.metrics().get("skippedTaskCount"));
        assertEquals(3.0, debug.metrics().get("failedInferenceCalls"));
    }

    @Test
    void inferenceQueueThreshold() {
        var low = new AiTelemetryAdapter.AiTelemetrySnapshot(1, 1, 1, 0, 0, 3, 0, 0, 0);
        var high = new AiTelemetryAdapter.AiTelemetrySnapshot(1, 1, 1, 0, 0, 6, 0, 0, 0);
        assertFalse(adapter.adapt(low, 1).flags().get("inferenceQueuePressure"));
        assertTrue(adapter.adapt(high, 1).flags().get("inferenceQueuePressure"));
    }
}
