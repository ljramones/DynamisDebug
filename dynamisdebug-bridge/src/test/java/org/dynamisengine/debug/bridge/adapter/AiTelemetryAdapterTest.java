package org.dynamisengine.debug.bridge.adapter;

import org.dynamisengine.debug.api.DebugCategory;
import org.dynamisengine.debug.bridge.adapter.AiTelemetryAdapter.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AiTelemetryAdapterTest {

    private final AiTelemetryAdapter adapter = new AiTelemetryAdapter();

    private AiTelemetrySnapshot healthy() {
        return new AiTelemetrySnapshot(
                new SimulationTelemetry(20, 3.5, 20, 1, 5),
                new CognitionTelemetry(2, 100, 0, 1.2, 50.0, 10, 5, 20, 0),
                new PlanningTelemetry(15, 2, 14, 1, 0),
                new BudgetTelemetry(65.0, 15, 0, 0, 10, 5, 3, 2));
    }

    @Test
    void subsystemNameAndCategory() {
        assertEquals("ai", adapter.subsystemName());
        assertEquals(DebugCategory.AI, adapter.category());
    }

    @Test
    void adaptHealthyState() {
        var debug = adapter.adapt(healthy(), 1);
        assertEquals(20.0, debug.metrics().get("sim.agentCount"));
        assertEquals(3.5, debug.metrics().get("sim.tickElapsedMs"));
        assertEquals(100.0, debug.metrics().get("cognition.totalInferenceCalls"));
        assertFalse(debug.flags().get("hasDegradedTasks"));
        assertFalse(debug.flags().get("budgetExceeded"));
        assertFalse(debug.flags().get("planningThrashing"));
    }

    @Test
    void adaptDegradedState() {
        var snapshot = new AiTelemetrySnapshot(
                new SimulationTelemetry(50, 12.0, 35, 1, 10),
                new CognitionTelemetry(8, 500, 3, 5.0, 20.0, 30, 15, 50, 5),
                new PlanningTelemetry(10, 15, 5, 5, 3),
                new BudgetTelemetry(120.0, 40, 10, 5, 15, 10, 10, 5));

        var debug = adapter.adapt(snapshot, 1);
        assertTrue(debug.flags().get("hasDegradedTasks"));
        assertTrue(debug.flags().get("hasSkippedTasks"));
        assertTrue(debug.flags().get("inferenceQueuePressure"));
        assertTrue(debug.flags().get("budgetExceeded"));
        assertTrue(debug.flags().get("planningThrashing"));
        assertTrue(debug.flags().get("perceptStaleness"));
    }

    @Test
    void metricsHaveAllPlanes() {
        var metrics = adapter.extractMetrics(healthy());
        assertTrue(metrics.containsKey("sim.agentCount"));
        assertTrue(metrics.containsKey("sim.navigationRequests"));
        assertTrue(metrics.containsKey("cognition.inferenceQueueDepth"));
        assertTrue(metrics.containsKey("cognition.tokensPerSecond"));
        assertTrue(metrics.containsKey("cognition.memoryRetrievalCount"));
        assertTrue(metrics.containsKey("planning.planCount"));
        assertTrue(metrics.containsKey("planning.fallbackCount"));
        assertTrue(metrics.containsKey("budget.budgetUsedPercent"));
        assertTrue(metrics.containsKey("budget.realtimeAgents"));
        assertTrue(metrics.containsKey("budget.skippedAgents"));
    }

    @Test
    void inferenceQueueThreshold() {
        var low = new AiTelemetrySnapshot(
                new SimulationTelemetry(1, 1, 1, 0, 0),
                new CognitionTelemetry(3, 0, 0, 0, 0, 0, 0, 0, 0),
                new PlanningTelemetry(0, 0, 0, 0, 0),
                new BudgetTelemetry(0, 0, 0, 0, 0, 0, 0, 0));
        var high = new AiTelemetrySnapshot(
                new SimulationTelemetry(1, 1, 1, 0, 0),
                new CognitionTelemetry(6, 0, 0, 0, 0, 0, 0, 0, 0),
                new PlanningTelemetry(0, 0, 0, 0, 0),
                new BudgetTelemetry(0, 0, 0, 0, 0, 0, 0, 0));
        assertFalse(adapter.adapt(low, 1).flags().get("inferenceQueuePressure"));
        assertTrue(adapter.adapt(high, 1).flags().get("inferenceQueuePressure"));
    }
}
