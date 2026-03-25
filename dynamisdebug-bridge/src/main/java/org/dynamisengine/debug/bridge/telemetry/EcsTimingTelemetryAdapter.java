package org.dynamisengine.debug.bridge.telemetry;

import org.dynamisengine.debug.api.DebugCategory;
import org.dynamisengine.debug.api.DebugSnapshot;
import org.dynamisengine.debug.bridge.TelemetryAdapter;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Adapts ECS per-system timing into the debug telemetry pipeline.
 *
 * <p>Input: {@link EcsTimingData} containing per-system timings and frame totals
 * from {@code SubsystemCoordinator}.
 *
 * <p>Output metrics:
 * <ul>
 *   <li>{@code ecs.frameTotalMs}</li>
 *   <li>{@code ecs.systemCount}</li>
 *   <li>{@code ecs.dominantSystemTimeMs}</li>
 *   <li>{@code ecs.system.<name>.timeMs} for each system</li>
 * </ul>
 */
public final class EcsTimingTelemetryAdapter implements TelemetryAdapter<EcsTimingTelemetryAdapter.EcsTimingData> {

    /**
     * Input data from the ECS runtime timing seam.
     */
    public record EcsTimingData(
        Map<String, Double> systemTimings,
        double frameTotalMs,
        String dominantSystem,
        double dominantTimeMs,
        int systemCount
    ) {}

    @Override
    public String subsystemName() { return "ecs"; }

    @Override
    public DebugCategory category() { return DebugCategory.ECS; }

    @Override
    public DebugSnapshot adapt(EcsTimingData data, long frameNumber) {
        var metrics = new LinkedHashMap<String, Double>();
        metrics.put("ecs.frameTotalMs", data.frameTotalMs());
        metrics.put("ecs.systemCount", (double) data.systemCount());
        metrics.put("ecs.dominantSystemTimeMs", data.dominantTimeMs());

        // Per-system metrics with sanitized names
        for (var entry : data.systemTimings().entrySet()) {
            String safeName = sanitizeName(entry.getKey());
            metrics.put("ecs.system." + safeName + ".timeMs", entry.getValue());
        }

        boolean overBudget = data.frameTotalMs() > 8.0;
        boolean dominant = data.dominantTimeMs() > data.frameTotalMs() * 0.5;

        return new DebugSnapshot(
            frameNumber,
            System.currentTimeMillis(),
            "ecs",
            DebugCategory.ECS,
            Map.copyOf(metrics),
            Map.of(
                "overBudget", overBudget,
                "dominantSystem", dominant
            ),
            data.dominantSystem()
        );
    }

    @Override
    public Map<String, Double> extractMetrics(EcsTimingData data) {
        var metrics = new LinkedHashMap<String, Double>();
        metrics.put("ecs.frameTotalMs", data.frameTotalMs());
        metrics.put("ecs.systemCount", (double) data.systemCount());
        metrics.put("ecs.dominantSystemTimeMs", data.dominantTimeMs());
        for (var entry : data.systemTimings().entrySet()) {
            metrics.put("ecs.system." + sanitizeName(entry.getKey()) + ".timeMs", entry.getValue());
        }
        return metrics;
    }

    /** Sanitize system name for metric keys (lowercase, dots to underscores). */
    private static String sanitizeName(String name) {
        return name.toLowerCase().replace('.', '_').replace(' ', '_');
    }
}
