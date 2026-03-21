package org.dynamisengine.debug.bridge.adapter;

import org.dynamisengine.debug.api.DebugCategory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ContentTelemetryAdapterTest {

    private final ContentTelemetryAdapter adapter = new ContentTelemetryAdapter();

    @Test
    void subsystemNameAndCategory() {
        assertEquals("content", adapter.subsystemName());
        assertEquals(DebugCategory.CONTENT, adapter.category());
    }

    @Test
    void adaptHealthyCache() {
        var snapshot = new ContentTelemetryAdapter.ContentTelemetrySnapshot(
                50, 30, 100, 80, 20, 0, 3
        );
        var debug = adapter.adapt(snapshot, 1);

        assertEquals(50.0, debug.metrics().get("manifestEntryCount"));
        assertEquals(30.0, debug.metrics().get("cachedAssetCount"));
        assertEquals(100.0, debug.metrics().get("totalLoads"));
        assertEquals(80.0, debug.metrics().get("cacheHits"));
        assertEquals(20.0, debug.metrics().get("cacheMisses"));
        assertEquals(0.0, debug.metrics().get("failedResolutions"));
        assertEquals(3.0, debug.metrics().get("registeredLoaderCount"));
        assertEquals(0.8, debug.metrics().get("cacheHitRate"), 0.01);
        assertFalse(debug.flags().get("hasFailures"));
    }

    @Test
    void adaptWithFailures() {
        var snapshot = new ContentTelemetryAdapter.ContentTelemetrySnapshot(
                10, 5, 20, 15, 5, 3, 2
        );
        var debug = adapter.adapt(snapshot, 1);
        assertTrue(debug.flags().get("hasFailures"));
        assertEquals(3.0, debug.metrics().get("failedResolutions"));
    }

    @Test
    void cacheHitRateNotPresentWhenNoAccesses() {
        var snapshot = new ContentTelemetryAdapter.ContentTelemetrySnapshot(
                10, 0, 0, 0, 0, 0, 1
        );
        var metrics = adapter.extractMetrics(snapshot);
        assertFalse(metrics.containsKey("cacheHitRate"));
    }

    @Test
    void textSummary() {
        var snapshot = new ContentTelemetryAdapter.ContentTelemetrySnapshot(
                20, 10, 50, 40, 10, 0, 2
        );
        var debug = adapter.adapt(snapshot, 1);
        assertTrue(debug.text().contains("manifest=20"));
        assertTrue(debug.text().contains("cached=10"));
        assertTrue(debug.text().contains("hits=40"));
        assertTrue(debug.text().contains("misses=10"));
    }

    @Test
    void emptySystem() {
        var snapshot = new ContentTelemetryAdapter.ContentTelemetrySnapshot(0, 0, 0, 0, 0, 0, 0);
        var debug = adapter.adapt(snapshot, 1);
        assertEquals(0.0, debug.metrics().get("manifestEntryCount"));
        assertFalse(debug.flags().get("hasFailures"));
    }
}
