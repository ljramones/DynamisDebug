package org.dynamisengine.debug.core;

import org.dynamisengine.debug.api.DebugCategory;
import org.dynamisengine.debug.api.DebugSnapshot;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DebugSnapshotAggregatorTest {

    private DebugSnapshot snap(String source, DebugCategory cat, Map<String, Double> metrics, String text) {
        return new DebugSnapshot(1, 100, source, cat, metrics, Map.of(), text);
    }

    @Test
    void aggregateMergesMetricsWithSourcePrefix() {
        var snapshots = new LinkedHashMap<String, DebugSnapshot>();
        snapshots.put("physics", snap("physics", DebugCategory.PHYSICS, Map.of("stepMs", 2.5), ""));
        snapshots.put("audio", snap("audio", DebugCategory.AUDIO, Map.of("voices", 32.0), ""));

        var agg = DebugSnapshotAggregator.aggregate(1, 100, snapshots, DebugCategory.ENGINE);

        assertEquals(2.5, agg.metrics().get("physics.stepMs"));
        assertEquals(32.0, agg.metrics().get("audio.voices"));
        assertEquals("aggregate", agg.source());
    }

    @Test
    void aggregateMergesFlags() {
        var s1 = new DebugSnapshot(1, 100, "a", DebugCategory.ENGINE,
                Map.of(), Map.of("vsync", true), "");
        var s2 = new DebugSnapshot(1, 100, "b", DebugCategory.ENGINE,
                Map.of(), Map.of("wireframe", false), "");

        var snapshots = new LinkedHashMap<String, DebugSnapshot>();
        snapshots.put("a", s1);
        snapshots.put("b", s2);

        var agg = DebugSnapshotAggregator.aggregate(1, 100, snapshots, DebugCategory.ENGINE);
        assertTrue(agg.flags().get("a.vsync"));
        assertFalse(agg.flags().get("b.wireframe"));
    }

    @Test
    void aggregateConcatenatesText() {
        var snapshots = new LinkedHashMap<String, DebugSnapshot>();
        snapshots.put("phys", snap("phys", DebugCategory.PHYSICS, Map.of(), "5 bodies"));
        snapshots.put("audio", snap("audio", DebugCategory.AUDIO, Map.of(), "32 voices"));

        var agg = DebugSnapshotAggregator.aggregate(1, 100, snapshots, DebugCategory.ENGINE);
        assertTrue(agg.text().contains("phys: 5 bodies"));
        assertTrue(agg.text().contains("audio: 32 voices"));
    }

    @Test
    void aggregateEmptySnapshotsProducesEmptyAggregate() {
        var agg = DebugSnapshotAggregator.aggregate(1, 100, Map.of(), DebugCategory.ENGINE);
        assertTrue(agg.metrics().isEmpty());
        assertTrue(agg.flags().isEmpty());
        assertNull(agg.text());
    }

    @Test
    void aggregateByCategoryFilters() {
        var snapshots = new LinkedHashMap<String, DebugSnapshot>();
        snapshots.put("phys", snap("phys", DebugCategory.PHYSICS, Map.of("step", 1.0), ""));
        snapshots.put("audio", snap("audio", DebugCategory.AUDIO, Map.of("lat", 5.0), ""));

        var agg = DebugSnapshotAggregator.aggregateByCategory(
                1, 100, snapshots, DebugCategory.PHYSICS);

        assertTrue(agg.metrics().containsKey("phys.step"));
        assertFalse(agg.metrics().containsKey("audio.lat"));
    }
}
