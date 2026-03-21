package org.dynamisengine.debug.bridge.adapter;

import org.dynamisengine.debug.api.DebugCategory;
import org.dynamisengine.debug.api.DebugSnapshot;
import org.dynamisengine.debug.bridge.TelemetryAdapter;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Adapts scene graph state into a unified {@link DebugSnapshot}.
 *
 * <p>Extracts node counts, renderable bindings, batch statistics,
 * and visibility data from the public DefaultSceneGraph API.
 *
 * <p>Read-only consumer — no debug concepts pushed back into DynamisSceneGraph.
 */
public final class SceneGraphTelemetryAdapter implements TelemetryAdapter<SceneGraphTelemetryAdapter.SceneGraphTelemetrySnapshot> {

    @Override
    public String subsystemName() { return "scenegraph"; }

    @Override
    public DebugCategory category() { return DebugCategory.RENDERING; }

    @Override
    public DebugSnapshot adapt(SceneGraphTelemetrySnapshot telemetry, long frameNumber) {
        Map<String, Double> metrics = extractMetrics(telemetry);

        Map<String, Boolean> flags = Map.of(
                "hasRenderables", telemetry.renderableNodeCount() > 0
        );

        String text = "nodes=" + telemetry.totalNodeCount()
                + " renderable=" + telemetry.renderableNodeCount()
                + " visible=" + telemetry.visibleNodeCount()
                + " batches=" + telemetry.batchCount();

        return new DebugSnapshot(frameNumber, System.currentTimeMillis(),
                subsystemName(), category(), metrics, flags, text);
    }

    @Override
    public Map<String, Double> extractMetrics(SceneGraphTelemetrySnapshot telemetry) {
        Map<String, Double> metrics = new LinkedHashMap<>();
        metrics.put("totalNodeCount", (double) telemetry.totalNodeCount());
        metrics.put("renderableNodeCount", (double) telemetry.renderableNodeCount());
        metrics.put("visibleNodeCount", (double) telemetry.visibleNodeCount());
        metrics.put("batchCount", (double) telemetry.batchCount());
        metrics.put("totalInstances", (double) telemetry.totalInstances());
        return metrics;
    }

    /**
     * Point-in-time scene graph state for telemetry extraction.
     *
     * <p>Callers construct this from {@code DefaultSceneGraph.viewsInStorageOrder()}
     * and extraction results at frame boundaries.
     *
     * @param totalNodeCount      total nodes in the graph
     * @param renderableNodeCount nodes with full renderable bindings
     * @param visibleNodeCount    visible nodes (after extraction filtering)
     * @param batchCount          number of instance batches after extraction
     * @param totalInstances      total instances across all batches
     */
    public record SceneGraphTelemetrySnapshot(
            int totalNodeCount,
            int renderableNodeCount,
            int visibleNodeCount,
            int batchCount,
            int totalInstances
    ) {
        /** Convenience factory for non-batched scenes. */
        public static SceneGraphTelemetrySnapshot flat(
                int totalNodeCount, int renderableNodeCount, int visibleNodeCount
        ) {
            return new SceneGraphTelemetrySnapshot(
                    totalNodeCount, renderableNodeCount, visibleNodeCount, 0, visibleNodeCount);
        }
    }
}
