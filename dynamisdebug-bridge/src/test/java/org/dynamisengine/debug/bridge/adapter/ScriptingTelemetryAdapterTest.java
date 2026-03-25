package org.dynamisengine.debug.bridge.adapter;

import org.dynamisengine.debug.api.DebugCategory;
import org.dynamisengine.debug.bridge.adapter.ScriptingTelemetryAdapter.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ScriptingTelemetryAdapterTest {

    private final ScriptingTelemetryAdapter adapter = new ScriptingTelemetryAdapter();

    private ScriptingTelemetrySnapshot healthy() {
        return new ScriptingTelemetrySnapshot(
                new CanonTelemetry(5, 5, 500_000, 200, 42, 100),
                new OracleTelemetry(10, 0, 3, 5, 0, 0),
                new ChroniclerTelemetry(2, 1, 0, 5),
                new PerceptTelemetry(50, 10, 0, 0, 0),
                new DegradationTelemetry(10, 0, 0, 0, 0, 0.0),
                50, 100);
    }

    @Test
    void subsystemNameAndCategory() {
        assertEquals("scripting", adapter.subsystemName());
        assertEquals(DebugCategory.SCRIPTING, adapter.category());
    }

    @Test
    void adaptHealthyState() {
        var debug = adapter.adapt(healthy(), 1);
        assertEquals(5.0, debug.metrics().get("canon.eventsCommitted"));
        assertEquals(0.0, debug.metrics().get("oracle.validateFailures"));
        assertEquals(10.0, debug.metrics().get("degradation.tier0Count"));
        assertFalse(debug.flags().get("hasDegradedAgents"));
        assertFalse(debug.flags().get("oracleRejecting"));
        assertFalse(debug.flags().get("tier3Active"));
    }

    @Test
    void adaptWithOracleRejections() {
        var snapshot = new ScriptingTelemetrySnapshot(
                new CanonTelemetry(10, 7, 1_000_000, 300, 50, 105),
                new OracleTelemetry(15, 3, 5, 7, 1, 3),
                new ChroniclerTelemetry(5, 2, 0, 8),
                new PerceptTelemetry(40, 8, 0, 0, 0),
                new DegradationTelemetry(8, 0, 0, 0, 0, 0.0),
                60, 80);

        var debug = adapter.adapt(snapshot, 1);
        assertTrue(debug.flags().get("oracleRejecting"));
        assertTrue(debug.flags().get("hasUncommittedEvents"));
        assertEquals(3.0, debug.metrics().get("oracle.validateFailures"));
        assertEquals(3.0, debug.metrics().get("oracle.rejectedIntents"));
    }

    @Test
    void adaptWithDegradation() {
        var snapshot = new ScriptingTelemetrySnapshot(
                new CanonTelemetry(5, 5, 500_000, 200, 42, 100),
                new OracleTelemetry(10, 0, 3, 5, 0, 0),
                new ChroniclerTelemetry(2, 1, 0, 5),
                new PerceptTelemetry(50, 10, 3, 1, 5),
                new DegradationTelemetry(5, 3, 1, 1, 15, 4.5),
                50, 100);

        var debug = adapter.adapt(snapshot, 1);
        assertTrue(debug.flags().get("hasDegradedAgents"));
        assertTrue(debug.flags().get("tier3Active"));
        assertTrue(debug.flags().get("perceptStaleness"));
        assertEquals(1.0, debug.metrics().get("degradation.tier3Count"));
        assertEquals(15.0, debug.metrics().get("degradation.maxLagTicks"));
        assertEquals(4.5, debug.metrics().get("degradation.averageLagTicks"));
    }

    @Test
    void adaptWithChroniclerBacklog() {
        var snapshot = new ScriptingTelemetrySnapshot(
                new CanonTelemetry(5, 5, 500_000, 200, 42, 100),
                new OracleTelemetry(10, 0, 3, 5, 0, 0),
                new ChroniclerTelemetry(25, 3, 2, 10),
                new PerceptTelemetry(50, 10, 0, 0, 0),
                new DegradationTelemetry(10, 0, 0, 0, 0, 0.0),
                50, 100);

        var debug = adapter.adapt(snapshot, 1);
        assertTrue(debug.flags().get("chroniclerBacklog"));
        assertEquals(25.0, debug.metrics().get("chronicler.pendingWorldEvents"));
        assertEquals(2.0, debug.metrics().get("chronicler.overdueDeadlines"));
    }

    @Test
    void metricsHaveAllPlanes() {
        var metrics = adapter.extractMetrics(healthy());
        assertTrue(metrics.containsKey("canon.eventsProposed"));
        assertTrue(metrics.containsKey("canon.logSize"));
        assertTrue(metrics.containsKey("oracle.validateCount"));
        assertTrue(metrics.containsKey("oracle.rejectedIntents"));
        assertTrue(metrics.containsKey("chronicler.pendingWorldEvents"));
        assertTrue(metrics.containsKey("percept.perceptsEmitted"));
        assertTrue(metrics.containsKey("percept.degradedDeliveries"));
        assertTrue(metrics.containsKey("degradation.tier0Count"));
        assertTrue(metrics.containsKey("degradation.maxLagTicks"));
        assertTrue(metrics.containsKey("dsl.cacheSize"));
        assertTrue(metrics.containsKey("dsl.cacheHits"));
        assertTrue(metrics.containsKey("dsl.cacheMisses"));
        assertTrue(metrics.containsKey("budget.remaining"));
        assertTrue(metrics.containsKey("chronicler.executionMs"));
        assertTrue(metrics.containsKey("evaluation.errorCount"));
    }

    @Test
    void executionMetricsPopulated() {
        var snapshot = new ScriptingTelemetrySnapshot(
                new CanonTelemetry(5, 5, 2_000_000, 200, 42, 100),
                new OracleTelemetry(10, 0, 3, 5, 0, 0),
                new ChroniclerTelemetry(2, 1, 0, 5),
                new PerceptTelemetry(50, 10, 0, 0, 0),
                new DegradationTelemetry(10, 0, 0, 0, 0, 0.0),
                50, 100,
                1_500_000, 2, 45, 5);

        var debug = adapter.adapt(snapshot, 1);
        assertEquals(45.0, debug.metrics().get("dsl.cacheHits"));
        assertEquals(5.0, debug.metrics().get("dsl.cacheMisses"));
        assertEquals(2.0, debug.metrics().get("evaluation.errorCount"));
        assertTrue(debug.flags().get("evaluationErrors"));
        assertFalse(debug.flags().get("cacheMissHigh"));
        assertTrue(debug.metrics().get("chronicler.executionMs") > 0);
    }
}
