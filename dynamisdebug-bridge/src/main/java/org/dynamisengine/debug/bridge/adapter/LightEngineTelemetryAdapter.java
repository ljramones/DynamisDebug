package org.dynamisengine.debug.bridge.adapter;

import org.dynamisengine.debug.api.DebugCategory;
import org.dynamisengine.debug.api.DebugSnapshot;
import org.dynamisengine.debug.bridge.TelemetryAdapter;
import org.dynamisengine.light.api.runtime.EngineStats;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Adapts {@link EngineStats} into a unified {@link DebugSnapshot}.
 *
 * <p>Captures draw calls, triangles, visible objects, GPU memory,
 * CPU/GPU frame timing, and TAA confidence metrics.
 *
 * <p>Read-only consumer — no debug concepts pushed back into DynamisLightEngine.
 */
public final class LightEngineTelemetryAdapter implements TelemetryAdapter<EngineStats> {

    @Override
    public String subsystemName() { return "lightengine"; }

    @Override
    public DebugCategory category() { return DebugCategory.RENDERING; }

    @Override
    public DebugSnapshot adapt(EngineStats stats, long frameNumber) {
        Map<String, Double> metrics = extractMetrics(stats);

        Map<String, Boolean> flags = Map.of(
                "gpuBound", stats.gpuFrameMs() > stats.cpuFrameMs(),
                "taaConfidenceLow", stats.taaConfidenceMean() < 0.5f,
                "highPipelineSwitches", stats.pipelineSwitches() > 5
        );

        String text = String.format(java.util.Locale.ROOT,
                "fps=%.0f draws=%d (shadow=%d geo=%d post=%d) switches=%d visible=%d/%d cpu=%.1fms gpu=%.1fms",
                stats.fps(), stats.drawCalls(),
                stats.shadowDrawCalls(), stats.geometryDrawCalls(), stats.postDrawCalls(),
                stats.pipelineSwitches(),
                stats.visibleObjects(), stats.submittedObjects(),
                stats.cpuFrameMs(), stats.gpuFrameMs());

        return new DebugSnapshot(frameNumber, System.currentTimeMillis(),
                subsystemName(), category(), metrics, flags, text);
    }

    @Override
    public Map<String, Double> extractMetrics(EngineStats stats) {
        Map<String, Double> metrics = new LinkedHashMap<>();
        metrics.put("fps", (double) stats.fps());
        metrics.put("cpuFrameMs", (double) stats.cpuFrameMs());
        metrics.put("gpuFrameMs", (double) stats.gpuFrameMs());
        metrics.put("drawCalls", (double) stats.drawCalls());
        metrics.put("triangles", (double) stats.triangles());
        metrics.put("visibleObjects", (double) stats.visibleObjects());
        metrics.put("gpuMemoryBytes", (double) stats.gpuMemoryBytes());
        metrics.put("taaHistoryRejectRate", (double) stats.taaHistoryRejectRate());
        metrics.put("taaConfidenceMean", (double) stats.taaConfidenceMean());
        metrics.put("taaConfidenceDropEvents", (double) stats.taaConfidenceDropEvents());
        // Per-pass draw call breakdown
        metrics.put("shadowDrawCalls", (double) stats.shadowDrawCalls());
        metrics.put("geometryDrawCalls", (double) stats.geometryDrawCalls());
        metrics.put("postDrawCalls", (double) stats.postDrawCalls());
        metrics.put("pipelineSwitches", (double) stats.pipelineSwitches());
        metrics.put("submittedObjects", (double) stats.submittedObjects());
        // Per-variant draw counts
        metrics.put("staticDraws", (double) stats.staticDraws());
        metrics.put("morphDraws", (double) stats.morphDraws());
        metrics.put("skinnedDraws", (double) stats.skinnedDraws());
        metrics.put("instancedDraws", (double) stats.instancedDraws());
        return metrics;
    }
}
