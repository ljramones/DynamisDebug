package org.dynamisengine.debug.bridge.adapter;

import org.dynamisengine.debug.api.DebugCategory;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ScriptingTelemetryAdapterTest {

    private final ScriptingTelemetryAdapter adapter = new ScriptingTelemetryAdapter();

    @Test
    void subsystemNameAndCategory() {
        assertEquals("scripting", adapter.subsystemName());
        assertEquals(DebugCategory.SCRIPTING, adapter.category());
    }

    @Test
    void adaptHealthyTick() {
        var snapshot = new ScriptingTelemetryAdapter.ScriptingTelemetrySnapshot(
                5, 5, 500_000, 10, 0, 50, 200, 100);
        var debug = adapter.adapt(snapshot, 1);

        assertEquals(5.0, debug.metrics().get("eventsProposed"));
        assertEquals(5.0, debug.metrics().get("eventsCommitted"));
        assertEquals(10.0, debug.metrics().get("agentCount"));
        assertEquals(50.0, debug.metrics().get("dslCacheSize"));
        assertEquals(200.0, debug.metrics().get("canonLogSize"));
        assertFalse(debug.flags().get("hasDegradedAgents"));
        assertFalse(debug.flags().get("hasUncommittedEvents"));
        assertTrue(debug.text().contains("proposed=5"));
        assertTrue(debug.text().contains("committed=5"));
    }

    @Test
    void adaptWithDegradation() {
        var snapshot = new ScriptingTelemetryAdapter.ScriptingTelemetrySnapshot(
                10, 7, 2_000_000, 20, 5, 100, 500, 50);
        var debug = adapter.adapt(snapshot, 1);

        assertTrue(debug.flags().get("hasDegradedAgents"));
        assertTrue(debug.flags().get("hasUncommittedEvents"));
        assertEquals(5.0, debug.metrics().get("degradedAgentCount"));
    }

    @Test
    void tickDurationInMs() {
        var snapshot = new ScriptingTelemetryAdapter.ScriptingTelemetrySnapshot(
                0, 0, 1_500_000, 0, 0, 0, 0, 0);
        var metrics = adapter.extractMetrics(snapshot);
        assertEquals(1.5, metrics.get("tickDurationMs"), 0.01);
    }
}
