package org.dynamisengine.debug.core;

import org.dynamisengine.debug.api.DebugCategory;
import org.dynamisengine.debug.api.DebugSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DebugTimelineTest {

    private DebugHistory history;
    private DebugTimeline timeline;

    @BeforeEach
    void setUp() {
        history = new DebugHistory(100);
        timeline = new DebugTimeline(history);
    }

    private void recordFrame(long frame, String source, Map<String, Double> metrics) {
        var snap = new DebugSnapshot(frame, frame * 16, source, DebugCategory.ENGINE, metrics, Map.of(), "");
        history.record(frame, Map.of(source, snap));
    }

    @Test
    void extractMetricFromSingleSource() {
        recordFrame(1, "physics", Map.of("stepMs", 2.0));
        recordFrame(2, "physics", Map.of("stepMs", 3.0));
        recordFrame(3, "physics", Map.of("stepMs", 2.5));

        var points = timeline.extractMetric("physics", "stepMs", 10);
        assertEquals(3, points.size());
        assertEquals(2.0, points.get(0).value());
        assertEquals(3.0, points.get(1).value());
        assertEquals(2.5, points.get(2).value());
    }

    @Test
    void extractMetricSkipsFramesWithoutSource() {
        recordFrame(1, "physics", Map.of("stepMs", 2.0));
        recordFrame(2, "audio", Map.of("latMs", 5.0)); // different source
        recordFrame(3, "physics", Map.of("stepMs", 3.0));

        var points = timeline.extractMetric("physics", "stepMs", 10);
        assertEquals(2, points.size());
        assertEquals(1, points.get(0).frameNumber());
        assertEquals(3, points.get(1).frameNumber());
    }

    @Test
    void extractMetricSkipsFramesWithoutMetric() {
        recordFrame(1, "physics", Map.of("stepMs", 2.0));
        recordFrame(2, "physics", Map.of("bodies", 100.0)); // different metric
        recordFrame(3, "physics", Map.of("stepMs", 3.0));

        var points = timeline.extractMetric("physics", "stepMs", 10);
        assertEquals(2, points.size());
    }

    @Test
    void extractMetricRespectsMaxFrames() {
        for (int i = 1; i <= 10; i++) {
            recordFrame(i, "src", Map.of("val", (double) i));
        }

        var points = timeline.extractMetric("src", "val", 3);
        assertEquals(3, points.size());
        assertEquals(8, points.get(0).frameNumber()); // last 3 frames
    }

    @Test
    void extractMetricAcrossSourcesFindsAll() {
        var snap1 = new DebugSnapshot(1, 16, "physics", DebugCategory.PHYSICS,
                Map.of("fps", 60.0), Map.of(), "");
        var snap2 = new DebugSnapshot(1, 16, "rendering", DebugCategory.RENDERING,
                Map.of("fps", 59.0), Map.of(), "");
        history.record(1, Map.of("physics", snap1, "rendering", snap2));

        var points = timeline.extractMetricAcrossSources("fps", 10);
        assertEquals(2, points.size());
    }

    @Test
    void statsComputesCorrectly() {
        recordFrame(1, "src", Map.of("val", 10.0));
        recordFrame(2, "src", Map.of("val", 20.0));
        recordFrame(3, "src", Map.of("val", 30.0));

        var stats = timeline.stats("src", "val", 10);
        assertEquals(10.0, stats.min());
        assertEquals(30.0, stats.max());
        assertEquals(20.0, stats.average());
        assertEquals(3, stats.sampleCount());
    }

    @Test
    void statsEmptyReturnsZeros() {
        var stats = timeline.stats("missing", "val", 10);
        assertEquals(0, stats.sampleCount());
        assertEquals(0.0, stats.min());
        assertEquals(0.0, stats.max());
        assertEquals(0.0, stats.average());
    }

    @Test
    void sessionExposesTimeline() {
        var session = new DebugSession(64, 100);
        assertNotNull(session.timeline());
    }
}
