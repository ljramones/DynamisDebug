package org.dynamisengine.debug.bridge.adapter;

import org.dynamisengine.debug.api.DebugCategory;
import org.dynamisengine.terrain.api.state.TerrainStats;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TerrainTelemetryAdapterTest {

    private final TerrainTelemetryAdapter adapter = new TerrainTelemetryAdapter();

    @Test
    void subsystemNameAndCategory() {
        assertEquals("terrain", adapter.subsystemName());
        assertEquals(DebugCategory.RENDERING, adapter.category());
    }

    @Test
    void adaptFullStats() {
        var stats = new TerrainStats(256, 64, 500000, 12000, 2.1f);
        var debug = adapter.adapt(stats, 1);

        assertEquals(256.0, debug.metrics().get("chunkCount"));
        assertEquals(64.0, debug.metrics().get("visibleChunks"));
        assertEquals(500000.0, debug.metrics().get("triangleCount"));
        assertEquals(12000.0, debug.metrics().get("foliageInstanceCount"));
        assertEquals(2.1, debug.metrics().get("gpuTimeMs"), 0.01);
        assertTrue(debug.flags().get("hasFoliage"));
        assertTrue(debug.text().contains("chunks=64/256"));
    }

    @Test
    void noFoliageFlag() {
        var stats = new TerrainStats(100, 50, 200000, 0, 1.0f);
        var debug = adapter.adapt(stats, 1);
        assertFalse(debug.flags().get("hasFoliage"));
    }
}
