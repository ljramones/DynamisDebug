package org.dynamisengine.debug.bridge.adapter;

import org.dynamisengine.debug.api.DebugCategory;
import org.dynamisengine.gpu.api.BindlessHeapStats;
import org.dynamisengine.gpu.api.upload.UploadTelemetry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GpuTelemetryAdapterTest {

    private final GpuTelemetryAdapter adapter = new GpuTelemetryAdapter();

    private UploadTelemetry sampleUpload() {
        return new UploadTelemetry(
                2,      // inflightCount
                3,      // backlogDepth
                4096,   // inflightBytes
                4,      // maxInflightSubmissions
                8192,   // maxInflightBytes
                5,      // maxBacklogDepth
                100,    // completedUploads
                102400, // completedBytes
                1.5,    // throughputGbps
                0.8,    // averageTtfuMillis
                2.1,    // p95TtfuMillis
                0.5     // averageCompletionLatencyMillis
        );
    }

    private BindlessHeapStats sampleHeap() {
        return new BindlessHeapStats(
                100, 8192,   // joint used/capacity
                50, 4096,    // morphDelta
                25, 4096,    // morphWeight
                200, 4096,   // instance
                500,         // allocations
                10,          // freesQueued
                8,           // freesRetired
                3,           // staleHandleRejects
                42,          // drawMetaCount
                1            // invalidIndexWrites
        );
    }

    @Test
    void subsystemNameAndCategory() {
        assertEquals("gpu", adapter.subsystemName());
        assertEquals(DebugCategory.RENDERING, adapter.category());
    }

    @Test
    void adaptUploadOnly() {
        var snapshot = GpuTelemetryAdapter.GpuTelemetrySnapshot.of(sampleUpload());
        var debug = adapter.adapt(snapshot, 1);

        assertEquals("gpu", debug.source());
        assertEquals(2.0, debug.metrics().get("inflightCount"));
        assertEquals(3.0, debug.metrics().get("backlogDepth"));
        assertEquals(4096.0, debug.metrics().get("inflightBytes"));
        assertEquals(1.5, debug.metrics().get("throughputGbps"));
        assertEquals(2.1, debug.metrics().get("p95TtfuMs"));
        assertEquals(100.0, debug.metrics().get("completedUploads"));
        assertTrue(debug.flags().get("backlogPressure"));
        assertFalse(debug.flags().get("staleHandles"));
        assertFalse(debug.metrics().containsKey("heap.jointUsed"));
    }

    @Test
    void adaptWithHeapStats() {
        var snapshot = GpuTelemetryAdapter.GpuTelemetrySnapshot.of(sampleUpload(), sampleHeap());
        var debug = adapter.adapt(snapshot, 1);

        assertEquals(100.0, debug.metrics().get("heap.jointUsed"));
        assertEquals(8192.0, debug.metrics().get("heap.jointCapacity"));
        assertEquals(200.0, debug.metrics().get("heap.instanceUsed"));
        assertEquals(500.0, debug.metrics().get("heap.allocations"));
        assertEquals(3.0, debug.metrics().get("heap.staleHandleRejects"));
        assertTrue(debug.flags().get("staleHandles"));
    }

    @Test
    void highWaterMarks() {
        var snapshot = GpuTelemetryAdapter.GpuTelemetrySnapshot.of(sampleUpload());
        var metrics = adapter.extractMetrics(snapshot);

        assertEquals(4.0, metrics.get("maxInflightSubmissions"));
        assertEquals(8192.0, metrics.get("maxInflightBytes"));
        assertEquals(5.0, metrics.get("maxBacklogDepth"));
    }

    @Test
    void textContainsSummary() {
        var snapshot = GpuTelemetryAdapter.GpuTelemetrySnapshot.of(sampleUpload());
        var debug = adapter.adapt(snapshot, 1);

        assertTrue(debug.text().contains("inflight=2"));
        assertTrue(debug.text().contains("backlog=3"));
        assertTrue(debug.text().contains("Gbps"));
        assertTrue(debug.text().contains("p95TTFU="));
    }

    @Test
    void snapshotNullUploadThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> new GpuTelemetryAdapter.GpuTelemetrySnapshot(null, null));
    }

    @Test
    void zeroBacklogNoFlag() {
        var upload = new UploadTelemetry(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        var snapshot = GpuTelemetryAdapter.GpuTelemetrySnapshot.of(upload);
        var debug = adapter.adapt(snapshot, 1);
        assertFalse(debug.flags().get("backlogPressure"));
    }
}
