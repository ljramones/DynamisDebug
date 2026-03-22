package org.dynamisengine.debug.core;

import org.dynamisengine.debug.api.DebugCategory;
import org.dynamisengine.debug.api.DebugSeverity;
import org.dynamisengine.debug.api.DebugSnapshot;
import org.dynamisengine.debug.api.event.DebugEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DebugAnalyticsTest {

    private DebugSession session;
    private DebugAnalytics analytics;

    @BeforeEach
    void setUp() {
        session = new DebugSession();
        analytics = new DebugAnalytics(session);
    }

    private void recordFrame(long frame, String source, String metric, double value) {
        session.history().record(frame, Map.of(source, new DebugSnapshot(
            frame, frame * 100, source, DebugCategory.ENGINE,
            Map.of(metric, value), Map.of(), ""
        )));
    }

    // --- Spike Analysis ---

    @Test
    void findSpikes_detectsFramesAboveThreshold() {
        recordFrame(1, "engine", "frameTimeMs", 5.0);
        recordFrame(2, "engine", "frameTimeMs", 12.0);
        recordFrame(3, "engine", "frameTimeMs", 8.0);
        recordFrame(4, "engine", "frameTimeMs", 20.0);
        recordFrame(5, "engine", "frameTimeMs", 3.0);

        var result = analytics.findSpikes("engine", "frameTimeMs", 10.0, 100);

        assertEquals(2, result.spikeCount());
        assertEquals(4, result.lastSpikeFrame());
        assertEquals(20.0, result.maxValue(), 0.01);
    }

    @Test
    void findSpikes_emptyHistoryReturnsZero() {
        var result = analytics.findSpikes("engine", "frameTimeMs", 10.0, 100);
        assertEquals(0, result.spikeCount());
        assertEquals(-1, result.lastSpikeFrame());
    }

    // --- Noisy Rules ---

    @Test
    void rankNoisyRules_sortsByFireCount() {
        for (int i = 0; i < 10; i++) {
            session.submit(new DebugEvent(i, i * 100, "test", DebugCategory.ENGINE,
                DebugSeverity.WARNING, "rule.noisy", "noisy"));
        }
        for (int i = 0; i < 3; i++) {
            session.submit(new DebugEvent(i, i * 100, "test", DebugCategory.ENGINE,
                DebugSeverity.WARNING, "rule.quiet", "quiet"));
        }

        var result = analytics.rankNoisyRules(100);

        assertEquals(2, result.size());
        assertEquals("rule.noisy", result.get(0).ruleName());
        assertEquals(10, result.get(0).fireCount());
        assertEquals("rule.quiet", result.get(1).ruleName());
        assertEquals(3, result.get(1).fireCount());
    }

    @Test
    void rankNoisyRules_excludesInfoEvents() {
        session.submit(new DebugEvent(1, 100, "test", DebugCategory.ENGINE,
            DebugSeverity.INFO, "info.event", "info"));
        session.submit(new DebugEvent(2, 200, "test", DebugCategory.ENGINE,
            DebugSeverity.WARNING, "warn.event", "warn"));

        var result = analytics.rankNoisyRules(100);
        assertEquals(1, result.size());
        assertEquals("warn.event", result.get(0).ruleName());
    }

    // --- Threshold Crossings ---

    @Test
    void analyzeThresholdCrossings_countsTransitions() {
        // below, above, above, below, above
        recordFrame(1, "test", "value", 5.0);
        recordFrame(2, "test", "value", 15.0);
        recordFrame(3, "test", "value", 18.0);
        recordFrame(4, "test", "value", 8.0);
        recordFrame(5, "test", "value", 12.0);

        var result = analytics.analyzeThresholdCrossings("test", "value", 10.0, 100);

        assertEquals(2, result.crossings()); // frame 2 and frame 5
        assertEquals(2, result.firstCrossing());
        assertEquals(5, result.lastCrossing());
        assertEquals(3, result.framesAbove()); // frames 2, 3, 5
    }

    @Test
    void analyzeThresholdCrossings_emptyHistory() {
        var result = analytics.analyzeThresholdCrossings("test", "value", 10.0, 100);
        assertEquals(0, result.crossings());
        assertEquals(-1, result.firstCrossing());
    }

    // --- Correlated Window ---

    @Test
    void findCorrelatedFrames_detectsCoOccurrence() {
        for (int i = 1; i <= 5; i++) {
            session.history().record(i, Map.of(
                "ecs", new DebugSnapshot(i, 0, "ecs", DebugCategory.ECS,
                    Map.of("entityCount", i * 200.0), Map.of(), ""),
                "engine", new DebugSnapshot(i, 0, "engine", DebugCategory.ENGINE,
                    Map.of("frameTimeMs", i * 4.0), Map.of(), "")
            ));
        }

        // ecs > 500 AND frameTimeMs > 10: frames 3 (600, 12), 4 (800, 16), 5 (1000, 20)
        var result = analytics.findCorrelatedFrames(
            "ecs", "entityCount", 500.0,
            "engine", "frameTimeMs", 10.0, 100);

        assertEquals(3, result.matches());
        assertEquals(3, result.firstMatch());
        assertEquals(5, result.lastMatch());
    }

    @Test
    void findCorrelatedFrames_noMatchesReturnsEmpty() {
        recordFrame(1, "engine", "frameTimeMs", 5.0);

        var result = analytics.findCorrelatedFrames(
            "ecs", "entityCount", 500.0,
            "engine", "frameTimeMs", 10.0, 100);

        assertEquals(0, result.matches());
        assertEquals(-1, result.firstMatch());
    }

    @Test
    void findSpikes_respectsWindowSize() {
        for (int i = 1; i <= 10; i++) {
            recordFrame(i, "engine", "frameTimeMs", i > 5 ? 20.0 : 5.0);
        }

        // Window of 3 should only see frames 8, 9, 10
        var result = analytics.findSpikes("engine", "frameTimeMs", 10.0, 3);
        assertEquals(3, result.spikeCount());
    }
}
