package org.dynamisengine.debug.bridge.telemetry;

import org.dynamisengine.debug.api.DebugCategory;
import org.dynamisengine.debug.api.DebugSnapshot;
import org.dynamisengine.debug.bridge.TelemetryAdapter;

import java.util.Map;

/**
 * Adapts GPU timing metrics (from LightEngine/Vulkan timestamp queries)
 * into the debug telemetry pipeline.
 *
 * <p>Input: a simple {@code Map<String, Double>} of gpu.* metrics
 * from the render backend (e.g. {@code VulkanContext.gpuTimingMetrics()}).
 *
 * <p>The map is expected to contain:
 * <ul>
 *   <li>{@code gpu.frameTimeMs}</li>
 *   <li>{@code gpu.shadowPassMs}</li>
 *   <li>{@code gpu.geometryPassMs}</li>
 *   <li>{@code gpu.postProcessMs}</li>
 *   <li>{@code gpu.uiPassMs}</li>
 *   <li>{@code gpu.timingAvailable} (1.0 = valid, 0.0 = unavailable)</li>
 * </ul>
 */
public final class GpuTimingTelemetryAdapter implements TelemetryAdapter<Map<String, Double>> {

    @Override
    public String subsystemName() { return "gpu"; }

    @Override
    public DebugCategory category() { return DebugCategory.RENDERING; }

    @Override
    public DebugSnapshot adapt(Map<String, Double> metrics, long frameNumber) {
        if (metrics == null || metrics.isEmpty()) {
            return new DebugSnapshot(frameNumber, System.currentTimeMillis(),
                "gpu", DebugCategory.RENDERING, Map.of("gpu.timingAvailable", 0.0),
                Map.of(), "");
        }

        boolean available = metrics.getOrDefault("gpu.timingAvailable", 0.0) > 0.5;
        double frameTimeMs = metrics.getOrDefault("gpu.frameTimeMs", 0.0);
        double cpuFrameTimeMs = 0; // not available here; comparison done by consumers

        return new DebugSnapshot(
            frameNumber,
            System.currentTimeMillis(),
            "gpu",
            DebugCategory.RENDERING,
            Map.copyOf(metrics),
            Map.of(
                "timingAvailable", available,
                "gpuBound", frameTimeMs > 8.0 // rough heuristic
            ),
            available ? "active" : "unavailable"
        );
    }

    @Override
    public Map<String, Double> extractMetrics(Map<String, Double> metrics) {
        return metrics != null ? metrics : Map.of();
    }
}
