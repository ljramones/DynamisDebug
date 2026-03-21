package org.dynamisengine.debug.api;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DebugSnapshotTest {

    @Test
    void defensiveCopyOfMetricsAndFlags() {
        var metrics = new java.util.HashMap<String, Double>();
        metrics.put("fps", 60.0);
        var flags = new java.util.HashMap<String, Boolean>();
        flags.put("vsync", true);

        var snap = new DebugSnapshot(1, 100L, "test", DebugCategory.ENGINE, metrics, flags, "ok");

        metrics.put("fps", 30.0);
        flags.put("vsync", false);

        assertEquals(60.0, snap.metrics().get("fps"));
        assertTrue(snap.flags().get("vsync"));
    }

    @Test
    void nullMetricsAndFlagsDefaultToEmpty() {
        var snap = new DebugSnapshot(1, 100L, "test", DebugCategory.ENGINE, null, null, null);
        assertTrue(snap.metrics().isEmpty());
        assertTrue(snap.flags().isEmpty());
    }

    @Test
    void metricsAreUnmodifiable() {
        var snap = new DebugSnapshot(1, 100L, "src", DebugCategory.RENDERING, Map.of("a", 1.0), Map.of(), "");
        assertThrows(UnsupportedOperationException.class, () -> snap.metrics().put("b", 2.0));
    }
}
