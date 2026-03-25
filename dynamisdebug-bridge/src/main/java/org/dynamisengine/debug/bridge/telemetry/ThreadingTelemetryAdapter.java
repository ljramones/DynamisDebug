package org.dynamisengine.debug.bridge.telemetry;

import org.dynamisengine.debug.api.DebugCategory;
import org.dynamisengine.debug.api.DebugSnapshot;
import org.dynamisengine.debug.bridge.TelemetryAdapter;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Adapts engine-wide threading and executor pool statistics into the debug
 * telemetry pipeline.
 *
 * <p>The Dynamis engine does not have a central job scheduler. Instead,
 * several independent thread pools service different subsystems:
 * <ul>
 *   <li>Event bus dispatch pool (AsyncEventBus)</li>
 *   <li>AI cognition virtual thread executor</li>
 *   <li>AI navigation virtual thread executor</li>
 *   <li>GPU upload worker pool</li>
 *   <li>Audio DSP worker thread</li>
 * </ul>
 *
 * <p>This adapter aggregates coarse statistics from whichever pools are
 * active, providing a unified view of engine threading health.
 *
 * <p>Input: {@link ThreadingSnapshot} populated by the runtime wiring layer.
 *
 * <p>Output metrics:
 * <ul>
 *   <li>{@code threading.activeWorkers}</li>
 *   <li>{@code threading.totalPools}</li>
 *   <li>{@code threading.eventBus.published}</li>
 *   <li>{@code threading.eventBus.delivered}</li>
 *   <li>{@code threading.eventBus.deadLetters}</li>
 *   <li>{@code threading.eventBus.dispatchMs}</li>
 *   <li>{@code threading.eventBus.subscriptions}</li>
 *   <li>{@code threading.cognition.queueDepth}</li>
 *   <li>{@code threading.cognition.completed}</li>
 *   <li>{@code threading.cognition.timeouts}</li>
 *   <li>{@code threading.navigation.pendingRequests}</li>
 *   <li>{@code threading.navigation.completed}</li>
 *   <li>{@code threading.gpuUpload.inflight}</li>
 *   <li>{@code threading.gpuUpload.backlog}</li>
 *   <li>{@code threading.gpuUpload.throughputGbps}</li>
 * </ul>
 */
public final class ThreadingTelemetryAdapter implements TelemetryAdapter<ThreadingTelemetryAdapter.ThreadingSnapshot> {

    @Override
    public String subsystemName() { return "threading"; }

    @Override
    public DebugCategory category() { return DebugCategory.ENGINE; }

    @Override
    public DebugSnapshot adapt(ThreadingSnapshot data, long frameNumber) {
        Map<String, Double> metrics = extractMetrics(data);

        Map<String, Boolean> flags = new LinkedHashMap<>();
        flags.put("cognitionQueueBacklog", data.cognitionQueueDepth() > 8);
        flags.put("eventBusDeadLetters", data.eventBusDeadLetters() > 0);
        flags.put("gpuUploadBacklog", data.gpuUploadBacklog() > 5);

        String text = String.format(java.util.Locale.ROOT,
                "pools=%d workers=%d | eventBus: pub=%d del=%d dead=%d | " +
                "cognition: queue=%d done=%d timeout=%d | " +
                "nav: pending=%d done=%d | gpu: inflight=%d backlog=%d",
                data.totalPools(), data.activeWorkers(),
                data.eventBusPublished(), data.eventBusDelivered(), data.eventBusDeadLetters(),
                data.cognitionQueueDepth(), data.cognitionCompleted(), data.cognitionTimeouts(),
                data.navigationPending(), data.navigationCompleted(),
                data.gpuUploadInflight(), data.gpuUploadBacklog());

        return new DebugSnapshot(frameNumber, System.currentTimeMillis(),
                subsystemName(), category(), metrics, flags, text);
    }

    @Override
    public Map<String, Double> extractMetrics(ThreadingSnapshot data) {
        Map<String, Double> metrics = new LinkedHashMap<>();

        // Aggregate
        metrics.put("threading.activeWorkers", (double) data.activeWorkers());
        metrics.put("threading.totalPools", (double) data.totalPools());

        // Event bus
        metrics.put("threading.eventBus.published", (double) data.eventBusPublished());
        metrics.put("threading.eventBus.delivered", (double) data.eventBusDelivered());
        metrics.put("threading.eventBus.deadLetters", (double) data.eventBusDeadLetters());
        metrics.put("threading.eventBus.dispatchMs", data.eventBusAvgDispatchNanos() / 1_000_000.0);
        metrics.put("threading.eventBus.subscriptions", (double) data.eventBusSubscriptions());

        // AI cognition executor
        metrics.put("threading.cognition.queueDepth", (double) data.cognitionQueueDepth());
        metrics.put("threading.cognition.completed", (double) data.cognitionCompleted());
        metrics.put("threading.cognition.timeouts", (double) data.cognitionTimeouts());

        // AI navigation executor
        metrics.put("threading.navigation.pendingRequests", (double) data.navigationPending());
        metrics.put("threading.navigation.completed", (double) data.navigationCompleted());

        // GPU upload pool
        metrics.put("threading.gpuUpload.inflight", (double) data.gpuUploadInflight());
        metrics.put("threading.gpuUpload.backlog", (double) data.gpuUploadBacklog());
        metrics.put("threading.gpuUpload.throughputGbps", data.gpuUploadThroughputGbps());

        return metrics;
    }

    /**
     * Aggregated threading snapshot across all engine executor pools.
     *
     * <p>Populated by the runtime wiring layer from whichever pools are active.
     * Fields default to zero for pools that are not present.
     */
    public record ThreadingSnapshot(
            int activeWorkers,
            int totalPools,
            // Event bus (from BusMetrics)
            long eventBusPublished,
            long eventBusDelivered,
            long eventBusDeadLetters,
            double eventBusAvgDispatchNanos,
            int eventBusSubscriptions,
            // AI cognition executor
            int cognitionQueueDepth,
            long cognitionCompleted,
            long cognitionTimeouts,
            // AI navigation executor
            int navigationPending,
            long navigationCompleted,
            // GPU upload pool
            int gpuUploadInflight,
            int gpuUploadBacklog,
            double gpuUploadThroughputGbps
    ) {}
}
