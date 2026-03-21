package org.dynamisengine.debug.core;

import org.dynamisengine.debug.api.*;
import org.dynamisengine.debug.api.event.DebugEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DebugWatchdogTest {

    private List<DebugEvent> capturedEvents;
    private DebugWatchdog watchdog;

    @BeforeEach
    void setUp() {
        capturedEvents = new ArrayList<>();
        watchdog = new DebugWatchdog(capturedEvents::add);
    }

    private DebugSnapshot snap(String source, DebugCategory cat, Map<String, Double> metrics) {
        return new DebugSnapshot(1, 0, source, cat, metrics, Map.of(), "");
    }

    // --- Basic rule evaluation ---

    @Test
    void ruleFiresWhenThresholdExceeded() {
        watchdog.addRule(WatchdogRule.above("gpu.backlog", "gpu", "backlogDepth",
                5.0, DebugSeverity.WARNING, "GPU backlog high"));

        var fired = watchdog.evaluate(1, Map.of(
                "gpu", snap("gpu", DebugCategory.RENDERING, Map.of("backlogDepth", 10.0))
        ));

        assertEquals(1, fired.size());
        assertEquals("gpu.backlog", fired.get(0).name());
        assertTrue(fired.get(0).message().contains("backlogDepth = 10.00"));
        assertEquals(1, capturedEvents.size());
    }

    @Test
    void ruleDoesNotFireBelowThreshold() {
        watchdog.addRule(WatchdogRule.above("gpu.backlog", "gpu", "backlogDepth",
                5.0, DebugSeverity.WARNING, "GPU backlog high"));

        var fired = watchdog.evaluate(1, Map.of(
                "gpu", snap("gpu", DebugCategory.RENDERING, Map.of("backlogDepth", 3.0))
        ));

        assertTrue(fired.isEmpty());
        assertTrue(capturedEvents.isEmpty());
    }

    @Test
    void belowRuleFires() {
        watchdog.addRule(WatchdogRule.below("content.hitRate", "content", "cacheHitRate",
                0.5, DebugSeverity.WARNING, "Cache hit rate low"));

        var fired = watchdog.evaluate(1, Map.of(
                "content", snap("content", DebugCategory.CONTENT, Map.of("cacheHitRate", 0.3))
        ));

        assertEquals(1, fired.size());
    }

    // --- Cooldown ---

    @Test
    void cooldownPreventsRepeatedFiring() {
        watchdog.addRule(new WatchdogRule("test", "gpu", "backlog", 0,
                WatchdogRule.Comparison.GREATER_THAN, DebugSeverity.WARNING, "high", 10));

        var snapshots = Map.of("gpu", snap("gpu", DebugCategory.RENDERING, Map.of("backlog", 5.0)));

        // Frame 1: fires
        assertEquals(1, watchdog.evaluate(1, snapshots).size());
        // Frame 5: cooldown (10 frames)
        assertEquals(0, watchdog.evaluate(5, snapshots).size());
        // Frame 12: fires again (past cooldown)
        assertEquals(1, watchdog.evaluate(12, snapshots).size());
    }

    @Test
    void resetCooldownsAllowsImmediateFiring() {
        watchdog.addRule(new WatchdogRule("test", "gpu", "backlog", 0,
                WatchdogRule.Comparison.GREATER_THAN, DebugSeverity.WARNING, "high", 100));

        var snapshots = Map.of("gpu", snap("gpu", DebugCategory.RENDERING, Map.of("backlog", 5.0)));

        watchdog.evaluate(1, snapshots);
        watchdog.resetCooldowns();
        assertEquals(1, watchdog.evaluate(2, snapshots).size());
    }

    // --- Missing data ---

    @Test
    void missingSourceSkipsRule() {
        watchdog.addRule(WatchdogRule.above("test", "missing", "metric",
                0, DebugSeverity.INFO, "msg"));

        var fired = watchdog.evaluate(1, Map.of(
                "gpu", snap("gpu", DebugCategory.RENDERING, Map.of("metric", 10.0))
        ));

        assertTrue(fired.isEmpty());
    }

    @Test
    void missingMetricSkipsRule() {
        watchdog.addRule(WatchdogRule.above("test", "gpu", "nonexistent",
                0, DebugSeverity.INFO, "msg"));

        var fired = watchdog.evaluate(1, Map.of(
                "gpu", snap("gpu", DebugCategory.RENDERING, Map.of("other", 10.0))
        ));

        assertTrue(fired.isEmpty());
    }

    // --- Multiple rules ---

    @Test
    void multipleRulesEvaluatedIndependently() {
        watchdog.addRule(WatchdogRule.above("gpu.backlog", "gpu", "backlog",
                5.0, DebugSeverity.WARNING, "backlog high"));
        watchdog.addRule(WatchdogRule.above("ecs.churn", "ecs", "destroyedCount",
                100.0, DebugSeverity.WARNING, "entity churn high"));

        var fired = watchdog.evaluate(1, Map.of(
                "gpu", snap("gpu", DebugCategory.RENDERING, Map.of("backlog", 10.0)),
                "ecs", snap("ecs", DebugCategory.ECS, Map.of("destroyedCount", 200.0))
        ));

        assertEquals(2, fired.size());
    }

    @Test
    void onlyMatchingRulesFire() {
        watchdog.addRule(WatchdogRule.above("gpu.backlog", "gpu", "backlog",
                5.0, DebugSeverity.WARNING, "backlog high"));
        watchdog.addRule(WatchdogRule.above("ecs.churn", "ecs", "destroyedCount",
                100.0, DebugSeverity.WARNING, "entity churn high"));

        var fired = watchdog.evaluate(1, Map.of(
                "gpu", snap("gpu", DebugCategory.RENDERING, Map.of("backlog", 10.0)),
                "ecs", snap("ecs", DebugCategory.ECS, Map.of("destroyedCount", 5.0))
        ));

        assertEquals(1, fired.size());
        assertEquals("gpu.backlog", fired.get(0).name());
    }

    // --- Rule management ---

    @Test
    void removeRuleStopsEvaluation() {
        watchdog.addRule(WatchdogRule.above("test", "gpu", "backlog",
                0, DebugSeverity.INFO, "msg"));
        watchdog.removeRule("test");

        var fired = watchdog.evaluate(1, Map.of(
                "gpu", snap("gpu", DebugCategory.RENDERING, Map.of("backlog", 10.0))
        ));

        assertTrue(fired.isEmpty());
        assertEquals(0, watchdog.ruleCount());
    }

    @Test
    void rulesListIsDefensiveCopy() {
        watchdog.addRule(WatchdogRule.above("test", "gpu", "x", 0, DebugSeverity.INFO, ""));
        var rules = watchdog.rules();
        assertEquals(1, rules.size());
        assertThrows(UnsupportedOperationException.class, () -> rules.add(null));
    }

    // --- Event detail quality ---

    @Test
    void firedEventContainsUsefulDetail() {
        watchdog.addRule(WatchdogRule.above("gpu.p95", "gpu", "p95TtfuMs",
                5.0, DebugSeverity.ERROR, "Upload latency spike"));

        var fired = watchdog.evaluate(42, Map.of(
                "gpu", snap("gpu", DebugCategory.RENDERING, Map.of("p95TtfuMs", 12.5))
        ));

        var event = fired.get(0);
        assertEquals(42, event.frameNumber());
        assertEquals("gpu", event.source());
        assertEquals(DebugCategory.RENDERING, event.category());
        assertEquals(DebugSeverity.ERROR, event.severity());
        assertEquals("gpu.p95", event.name());
        assertTrue(event.message().contains("Upload latency spike"));
        assertTrue(event.message().contains("12.50"));
        assertTrue(event.message().contains("5.00"));
    }

    // --- Session integration ---

    @Test
    void sessionExposesWatchdog() {
        var session = new DebugSession();
        assertNotNull(session.watchdog());
    }

    @Test
    void watchdogEventsFlowIntoSessionEventBuffer() {
        var session = new DebugSession(64, 100);
        session.watchdog().addRule(WatchdogRule.above("test", "gpu", "backlog",
                0, DebugSeverity.WARNING, "high"));

        // Simulate frame capture with watchdog evaluation
        session.watchdog().evaluate(1, Map.of(
                "gpu", snap("gpu", DebugCategory.RENDERING, Map.of("backlog", 5.0))
        ));

        var events = session.drainEvents();
        assertEquals(1, events.size());
        assertEquals("test", events.get(0).name());
    }
}
