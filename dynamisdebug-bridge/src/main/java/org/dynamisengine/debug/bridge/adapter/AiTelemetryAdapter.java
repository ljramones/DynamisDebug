package org.dynamisengine.debug.bridge.adapter;

import org.dynamisengine.debug.api.DebugCategory;
import org.dynamisengine.debug.api.DebugSnapshot;
import org.dynamisengine.debug.bridge.TelemetryAdapter;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Adapts AI engine telemetry into a unified {@link DebugSnapshot}.
 *
 * <p>Consumes data from DynamisAiEngine's AIOutputFrame and FrameBudgetReport.
 * Uses a plain-value DTO since the AI module has complex internal types.
 *
 * <p>Read-only consumer — no debug concepts pushed back into DynamisAI.
 */
public final class AiTelemetryAdapter implements TelemetryAdapter<AiTelemetryAdapter.AiTelemetrySnapshot> {

    @Override
    public String subsystemName() { return "ai"; }

    @Override
    public DebugCategory category() { return DebugCategory.AI; }

    @Override
    public DebugSnapshot adapt(AiTelemetrySnapshot telemetry, long frameNumber) {
        Map<String, Double> metrics = extractMetrics(telemetry);

        Map<String, Boolean> flags = new LinkedHashMap<>();
        flags.put("hasDegradedTasks", telemetry.degradedTaskCount() > 0);
        flags.put("hasSkippedTasks", telemetry.skippedTaskCount() > 0);
        flags.put("inferenceQueuePressure", telemetry.inferenceQueueDepth() > 5);

        String text = String.format(java.util.Locale.ROOT,
                "agents=%d tick=%.1fms tasks=%d degraded=%d skipped=%d inference=%d/%d",
                telemetry.agentCount(), telemetry.tickElapsedMs(),
                telemetry.taskCount(), telemetry.degradedTaskCount(), telemetry.skippedTaskCount(),
                telemetry.inferenceQueueDepth(), telemetry.totalInferenceCalls());

        return new DebugSnapshot(frameNumber, System.currentTimeMillis(),
                subsystemName(), category(), metrics, flags, text);
    }

    @Override
    public Map<String, Double> extractMetrics(AiTelemetrySnapshot telemetry) {
        Map<String, Double> metrics = new LinkedHashMap<>();
        metrics.put("agentCount", (double) telemetry.agentCount());
        metrics.put("tickElapsedMs", telemetry.tickElapsedMs());
        metrics.put("taskCount", (double) telemetry.taskCount());
        metrics.put("degradedTaskCount", (double) telemetry.degradedTaskCount());
        metrics.put("skippedTaskCount", (double) telemetry.skippedTaskCount());
        metrics.put("inferenceQueueDepth", (double) telemetry.inferenceQueueDepth());
        metrics.put("totalInferenceCalls", (double) telemetry.totalInferenceCalls());
        metrics.put("failedInferenceCalls", (double) telemetry.failedInferenceCalls());
        metrics.put("inferenceLatencyMs", telemetry.inferenceLatencyMs());
        return metrics;
    }

    /**
     * Point-in-time AI engine state for telemetry extraction.
     *
     * <p>Callers construct this from AIOutputFrame + FrameBudgetReport +
     * InferenceBackendMetrics at frame boundaries.
     *
     * @param agentCount          active AI agents
     * @param tickElapsedMs       total AI processing time this tick
     * @param taskCount           tasks executed this tick
     * @param degradedTaskCount   tasks that ran degraded
     * @param skippedTaskCount    tasks skipped due to budget
     * @param inferenceQueueDepth pending inference requests
     * @param totalInferenceCalls cumulative inference calls
     * @param failedInferenceCalls cumulative inference failures
     * @param inferenceLatencyMs  last inference call latency
     */
    public record AiTelemetrySnapshot(
            int agentCount,
            double tickElapsedMs,
            int taskCount,
            int degradedTaskCount,
            int skippedTaskCount,
            int inferenceQueueDepth,
            long totalInferenceCalls,
            long failedInferenceCalls,
            double inferenceLatencyMs
    ) {}
}
