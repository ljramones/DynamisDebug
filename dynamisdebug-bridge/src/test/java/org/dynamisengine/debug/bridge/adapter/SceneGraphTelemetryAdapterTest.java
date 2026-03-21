package org.dynamisengine.debug.bridge.adapter;

import org.dynamisengine.debug.api.DebugCategory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SceneGraphTelemetryAdapterTest {

    private final SceneGraphTelemetryAdapter adapter = new SceneGraphTelemetryAdapter();

    @Test
    void subsystemNameAndCategory() {
        assertEquals("scenegraph", adapter.subsystemName());
        assertEquals(DebugCategory.RENDERING, adapter.category());
    }

    @Test
    void adaptFullScene() {
        var snapshot = new SceneGraphTelemetryAdapter.SceneGraphTelemetrySnapshot(
                100, 80, 60, 5, 60
        );
        var debug = adapter.adapt(snapshot, 1);

        assertEquals(100.0, debug.metrics().get("totalNodeCount"));
        assertEquals(80.0, debug.metrics().get("renderableNodeCount"));
        assertEquals(60.0, debug.metrics().get("visibleNodeCount"));
        assertEquals(5.0, debug.metrics().get("batchCount"));
        assertEquals(60.0, debug.metrics().get("totalInstances"));
        assertTrue(debug.flags().get("hasRenderables"));
        assertTrue(debug.text().contains("nodes=100"));
        assertTrue(debug.text().contains("batches=5"));
    }

    @Test
    void adaptEmptyScene() {
        var snapshot = new SceneGraphTelemetryAdapter.SceneGraphTelemetrySnapshot(0, 0, 0, 0, 0);
        var debug = adapter.adapt(snapshot, 1);

        assertEquals(0.0, debug.metrics().get("totalNodeCount"));
        assertFalse(debug.flags().get("hasRenderables"));
    }

    @Test
    void flatFactory() {
        var snapshot = SceneGraphTelemetryAdapter.SceneGraphTelemetrySnapshot.flat(50, 30, 20);
        assertEquals(50, snapshot.totalNodeCount());
        assertEquals(30, snapshot.renderableNodeCount());
        assertEquals(20, snapshot.visibleNodeCount());
        assertEquals(0, snapshot.batchCount());
        assertEquals(20, snapshot.totalInstances());
    }

    @Test
    void extractMetricsConsistent() {
        var snapshot = new SceneGraphTelemetryAdapter.SceneGraphTelemetrySnapshot(10, 8, 6, 2, 6);
        var metrics = adapter.extractMetrics(snapshot);
        assertEquals(10.0, metrics.get("totalNodeCount"));
        assertEquals(8.0, metrics.get("renderableNodeCount"));
    }
}
