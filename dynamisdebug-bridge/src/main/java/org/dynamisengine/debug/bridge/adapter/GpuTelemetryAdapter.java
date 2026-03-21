package org.dynamisengine.debug.bridge.adapter;

import org.dynamisengine.debug.api.DebugCategory;
import org.dynamisengine.debug.api.DebugSnapshot;
import org.dynamisengine.debug.bridge.TelemetryAdapter;
import org.dynamisengine.gpu.api.BindlessHeapStats;
import org.dynamisengine.gpu.api.upload.UploadTelemetry;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Adapts GPU upload and bindless heap telemetry into a unified {@link DebugSnapshot}.
 *
 * <p>Consumes {@link GpuTelemetrySnapshot} which bundles both
 * {@link UploadTelemetry} and optional {@link BindlessHeapStats}.
 *
 * <p>This adapter is a read-only consumer of DynamisGPU's existing
 * telemetry API. It does not push any debug concepts back into DynamisGPU.
 */
public final class GpuTelemetryAdapter implements TelemetryAdapter<GpuTelemetryAdapter.GpuTelemetrySnapshot> {

    @Override
    public String subsystemName() { return "gpu"; }

    @Override
    public DebugCategory category() { return DebugCategory.RENDERING; }

    @Override
    public DebugSnapshot adapt(GpuTelemetrySnapshot telemetry, long frameNumber) {
        Map<String, Double> metrics = extractMetrics(telemetry);

        boolean backlogPressure = telemetry.upload().backlogDepth() > 0;
        boolean staleHandles = telemetry.heap() != null && telemetry.heap().staleHandleRejects() > 0;
        Map<String, Boolean> flags = Map.of(
                "backlogPressure", backlogPressure,
                "staleHandles", staleHandles
        );

        String text = "inflight=" + telemetry.upload().inflightCount()
                + " backlog=" + telemetry.upload().backlogDepth()
                + " throughput=" + String.format(java.util.Locale.ROOT, "%.2f", telemetry.upload().throughputGbps()) + "Gbps"
                + " p95TTFU=" + String.format(java.util.Locale.ROOT, "%.1f", telemetry.upload().p95TtfuMillis()) + "ms";

        return new DebugSnapshot(frameNumber, System.currentTimeMillis(),
                subsystemName(), category(), metrics, flags, text);
    }

    @Override
    public Map<String, Double> extractMetrics(GpuTelemetrySnapshot telemetry) {
        Map<String, Double> metrics = new LinkedHashMap<>();
        UploadTelemetry u = telemetry.upload();

        // Upload metrics
        metrics.put("inflightCount", (double) u.inflightCount());
        metrics.put("backlogDepth", (double) u.backlogDepth());
        metrics.put("inflightBytes", (double) u.inflightBytes());
        metrics.put("completedUploads", (double) u.completedUploads());
        metrics.put("completedBytes", (double) u.completedBytes());
        metrics.put("throughputGbps", u.throughputGbps());
        metrics.put("averageTtfuMs", u.averageTtfuMillis());
        metrics.put("p95TtfuMs", u.p95TtfuMillis());
        metrics.put("averageCompletionLatencyMs", u.averageCompletionLatencyMillis());

        // High-water marks
        metrics.put("maxInflightSubmissions", (double) u.maxInflightSubmissions());
        metrics.put("maxInflightBytes", (double) u.maxInflightBytes());
        metrics.put("maxBacklogDepth", (double) u.maxBacklogDepth());

        // Bindless heap (if available)
        BindlessHeapStats h = telemetry.heap();
        if (h != null) {
            metrics.put("heap.jointUsed", (double) h.jointUsed());
            metrics.put("heap.jointCapacity", (double) h.jointCapacity());
            metrics.put("heap.morphDeltaUsed", (double) h.morphDeltaUsed());
            metrics.put("heap.morphDeltaCapacity", (double) h.morphDeltaCapacity());
            metrics.put("heap.morphWeightUsed", (double) h.morphWeightUsed());
            metrics.put("heap.morphWeightCapacity", (double) h.morphWeightCapacity());
            metrics.put("heap.instanceUsed", (double) h.instanceUsed());
            metrics.put("heap.instanceCapacity", (double) h.instanceCapacity());
            metrics.put("heap.allocations", (double) h.allocations());
            metrics.put("heap.freesQueued", (double) h.freesQueued());
            metrics.put("heap.freesRetired", (double) h.freesRetired());
            metrics.put("heap.staleHandleRejects", (double) h.staleHandleRejects());
            metrics.put("heap.drawMetaCount", (double) h.drawMetaCount());
        }

        return metrics;
    }

    /**
     * Bundles upload telemetry and optional bindless heap stats into a single
     * telemetry snapshot for adaptation.
     *
     * @param upload upload manager telemetry (required)
     * @param heap   bindless heap stats (nullable — not all backends have one)
     */
    public record GpuTelemetrySnapshot(UploadTelemetry upload, BindlessHeapStats heap) {

        public GpuTelemetrySnapshot {
            if (upload == null) throw new IllegalArgumentException("upload must not be null");
        }

        public static GpuTelemetrySnapshot of(UploadTelemetry upload) {
            return new GpuTelemetrySnapshot(upload, null);
        }

        public static GpuTelemetrySnapshot of(UploadTelemetry upload, BindlessHeapStats heap) {
            return new GpuTelemetrySnapshot(upload, heap);
        }
    }
}
