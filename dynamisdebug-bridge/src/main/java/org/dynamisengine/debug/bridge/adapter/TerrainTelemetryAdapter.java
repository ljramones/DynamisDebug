package org.dynamisengine.debug.bridge.adapter;

import org.dynamisengine.debug.api.DebugCategory;
import org.dynamisengine.debug.api.DebugSnapshot;
import org.dynamisengine.debug.bridge.TelemetryAdapter;
import org.dynamisengine.terrain.api.state.TerrainStats;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Adapts {@link TerrainStats} into a unified {@link DebugSnapshot}.
 *
 * <p>Captures chunk counts, visible chunks, triangle count, foliage instances,
 * and GPU timing.
 *
 * <p>Read-only consumer — no debug concepts pushed back into DynamisTerrain.
 */
public final class TerrainTelemetryAdapter implements TelemetryAdapter<TerrainStats> {

    @Override
    public String subsystemName() { return "terrain"; }

    @Override
    public DebugCategory category() { return DebugCategory.RENDERING; }

    @Override
    public DebugSnapshot adapt(TerrainStats stats, long frameNumber) {
        Map<String, Double> metrics = extractMetrics(stats);

        Map<String, Boolean> flags = Map.of(
                "hasFoliage", stats.foliageInstanceCount() > 0
        );

        String text = String.format(java.util.Locale.ROOT,
                "chunks=%d/%d tris=%d foliage=%d gpu=%.1fms",
                stats.visibleChunks(), stats.chunkCount(),
                stats.triangleCount(), stats.foliageInstanceCount(), stats.gpuTimeMs());

        return new DebugSnapshot(frameNumber, System.currentTimeMillis(),
                subsystemName(), category(), metrics, flags, text);
    }

    @Override
    public Map<String, Double> extractMetrics(TerrainStats stats) {
        Map<String, Double> metrics = new LinkedHashMap<>();
        metrics.put("chunkCount", (double) stats.chunkCount());
        metrics.put("visibleChunks", (double) stats.visibleChunks());
        metrics.put("triangleCount", (double) stats.triangleCount());
        metrics.put("foliageInstanceCount", (double) stats.foliageInstanceCount());
        metrics.put("gpuTimeMs", (double) stats.gpuTimeMs());
        return metrics;
    }
}
