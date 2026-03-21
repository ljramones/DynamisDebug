package org.dynamisengine.debug.bridge.adapter;

import org.dynamisengine.debug.api.DebugCategory;
import org.dynamisengine.light.api.runtime.EngineStats;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LightEngineTelemetryAdapterTest {

    private final LightEngineTelemetryAdapter adapter = new LightEngineTelemetryAdapter();

    @Test
    void subsystemNameAndCategory() {
        assertEquals("lightengine", adapter.subsystemName());
        assertEquals(DebugCategory.RENDERING, adapter.category());
    }

    @Test
    void adaptFullStats() {
        var stats = new EngineStats(60, 8.5f, 12.3f, 500, 150000, 200, 512_000_000, 0.05f, 0.85f, 2);
        var debug = adapter.adapt(stats, 1);

        assertEquals(60.0, debug.metrics().get("fps"));
        assertEquals(8.5, debug.metrics().get("cpuFrameMs"), 0.01);
        assertEquals(12.3, debug.metrics().get("gpuFrameMs"), 0.01);
        assertEquals(500.0, debug.metrics().get("drawCalls"));
        assertEquals(150000.0, debug.metrics().get("triangles"));
        assertEquals(200.0, debug.metrics().get("visibleObjects"));
        assertTrue(debug.flags().get("gpuBound"));
        assertTrue(debug.text().contains("fps=60"));
        assertTrue(debug.text().contains("draws=500"));
    }

    @Test
    void cpuBoundDetected() {
        var stats = new EngineStats(30, 20.0f, 5.0f, 100, 10000, 50, 0, 0, 0.9f, 0);
        var debug = adapter.adapt(stats, 1);
        assertFalse(debug.flags().get("gpuBound"));
    }

    @Test
    void taaConfidenceLow() {
        var stats = new EngineStats(60, 8.0f, 10.0f, 100, 10000, 50, 0, 0.1f, 0.3f, 5);
        var debug = adapter.adapt(stats, 1);
        assertTrue(debug.flags().get("taaConfidenceLow"));
    }
}
