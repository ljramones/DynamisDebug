package org.dynamisengine.debug.bridge.adapter;

import org.dynamisengine.debug.api.DebugCategory;
import org.dynamisengine.debug.api.DebugSnapshot;
import org.dynamisengine.debug.bridge.TelemetryAdapter;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Adapts DynamisAI's multi-plane simulation stack into a unified {@link DebugSnapshot}.
 *
 * <p>DynamisAI is a deterministic, budget-governed AI simulation with optional
 * async cognition. This adapter captures four telemetry planes:
 *
 * <ol>
 *   <li><b>Simulation</b> — agents, tick timing, outputs, deterministic state</li>
 *   <li><b>Cognition</b> — inference queue, LLM calls, memory retrieval, belief updates</li>
 *   <li><b>Planning</b> — plan count, replans, success/failure/fallback</li>
 *   <li><b>LOD/Budget</b> — tier distribution, budget usage, skipped/degraded work</li>
 * </ol>
 *
 * <p>Read-only consumer — no debug concepts pushed back into DynamisAI.
 */
public final class AiTelemetryAdapter implements TelemetryAdapter<AiTelemetryAdapter.AiTelemetrySnapshot> {

    @Override
    public String subsystemName() { return "ai"; }

    @Override
    public DebugCategory category() { return DebugCategory.AI; }

    @Override
    public DebugSnapshot adapt(AiTelemetrySnapshot t, long frameNumber) {
        Map<String, Double> metrics = extractMetrics(t);

        Map<String, Boolean> flags = new LinkedHashMap<>();
        flags.put("hasDegradedTasks", t.budget().degradedTaskCount() > 0);
        flags.put("hasSkippedTasks", t.budget().skippedTaskCount() > 0);
        flags.put("inferenceQueuePressure", t.cognition().inferenceQueueDepth() > 5);
        flags.put("budgetExceeded", t.budget().budgetUsedPercent() > 100.0);
        flags.put("planningThrashing", t.planning().replanCount() > t.planning().planCount());
        flags.put("perceptStaleness", t.cognition().stalePerceptLoad() > 0);
        flags.put("frameOverBudget", t.execution().frameTotalMs() > 8.0);
        flags.put("timeoutBurst", t.execution().timeoutInferences() > 3);

        String hottestInfo = t.execution().hottestTaskId().isEmpty()
                ? "" : " hottest=" + t.execution().hottestTaskId();
        String text = String.format(java.util.Locale.ROOT,
                "agents=%d frame=%.1fms (tick=%.1fms) budget=%.0f%%%s | " +
                "inference: queue=%d done=%d timeout=%d | " +
                "plans=%d replans=%d | LOD: 0=%d 1=%d 2=%d skip=%d",
                t.simulation().agentCount(), t.execution().frameTotalMs(),
                t.simulation().tickElapsedMs(), t.budget().budgetUsedPercent(), hottestInfo,
                t.cognition().inferenceQueueDepth(), t.execution().completedInferences(),
                t.execution().timeoutInferences(),
                t.planning().planCount(), t.planning().replanCount(),
                t.budget().realtimeAgents(), t.budget().highQAgents(),
                t.budget().cachedAgents(), t.budget().skippedTaskCount());

        return new DebugSnapshot(frameNumber, System.currentTimeMillis(),
                subsystemName(), category(), metrics, flags, text);
    }

    @Override
    public Map<String, Double> extractMetrics(AiTelemetrySnapshot t) {
        Map<String, Double> metrics = new LinkedHashMap<>();

        // Simulation plane
        metrics.put("sim.agentCount", (double) t.simulation().agentCount());
        metrics.put("sim.tickElapsedMs", t.simulation().tickElapsedMs());
        metrics.put("sim.agentsUpdatedThisTick", (double) t.simulation().agentsUpdatedThisTick());
        metrics.put("sim.outputFrameCount", (double) t.simulation().outputFrameCount());
        metrics.put("sim.navigationRequests", (double) t.simulation().navigationRequests());

        // Cognition plane
        metrics.put("cognition.inferenceQueueDepth", (double) t.cognition().inferenceQueueDepth());
        metrics.put("cognition.totalInferenceCalls", (double) t.cognition().totalInferenceCalls());
        metrics.put("cognition.failedInferenceCalls", (double) t.cognition().failedInferenceCalls());
        metrics.put("cognition.inferenceLatencyMs", t.cognition().inferenceLatencyMs());
        metrics.put("cognition.tokensPerSecond", t.cognition().tokensPerSecond());
        metrics.put("cognition.memoryRetrievalCount", (double) t.cognition().memoryRetrievalCount());
        metrics.put("cognition.beliefUpdateCount", (double) t.cognition().beliefUpdateCount());
        metrics.put("cognition.perceptionSnapshotCount", (double) t.cognition().perceptionSnapshotCount());
        metrics.put("cognition.stalePerceptLoad", (double) t.cognition().stalePerceptLoad());

        // Planning plane
        metrics.put("planning.planCount", (double) t.planning().planCount());
        metrics.put("planning.replanCount", (double) t.planning().replanCount());
        metrics.put("planning.successCount", (double) t.planning().successCount());
        metrics.put("planning.failureCount", (double) t.planning().failureCount());
        metrics.put("planning.fallbackCount", (double) t.planning().fallbackCount());

        // LOD/Budget plane
        metrics.put("budget.budgetUsedPercent", t.budget().budgetUsedPercent());
        metrics.put("budget.totalTasks", (double) t.budget().totalTasks());
        metrics.put("budget.degradedTaskCount", (double) t.budget().degradedTaskCount());
        metrics.put("budget.skippedTaskCount", (double) t.budget().skippedTaskCount());
        metrics.put("budget.realtimeAgents", (double) t.budget().realtimeAgents());
        metrics.put("budget.highQAgents", (double) t.budget().highQAgents());
        metrics.put("budget.cachedAgents", (double) t.budget().cachedAgents());
        metrics.put("budget.skippedAgents", (double) t.budget().skippedAgents());

        // Execution timing plane (from FrameBudgetReport)
        metrics.put("execution.frameTotalMs", t.execution().frameTotalMs());
        metrics.put("execution.hottestTaskMs", t.execution().hottestTaskMs());
        metrics.put("execution.taskCount", (double) t.execution().taskCount());
        metrics.put("execution.completedInferences", (double) t.execution().completedInferences());
        metrics.put("execution.timeoutInferences", (double) t.execution().timeoutInferences());

        return metrics;
    }

    /** Simulation plane: core agent/tick/output state. */
    public record SimulationTelemetry(
            int agentCount, double tickElapsedMs,
            int agentsUpdatedThisTick, int outputFrameCount,
            int navigationRequests
    ) {}

    /** Cognition plane: inference, memory, perception, belief state. */
    public record CognitionTelemetry(
            int inferenceQueueDepth, long totalInferenceCalls,
            long failedInferenceCalls, double inferenceLatencyMs,
            double tokensPerSecond, int memoryRetrievalCount,
            int beliefUpdateCount, int perceptionSnapshotCount,
            int stalePerceptLoad
    ) {}

    /** Planning plane: plan selection and execution outcomes. */
    public record PlanningTelemetry(
            int planCount, int replanCount,
            int successCount, int failureCount, int fallbackCount
    ) {}

    /** LOD/Budget plane: resource governance and tier distribution. */
    public record BudgetTelemetry(
            double budgetUsedPercent, int totalTasks,
            int degradedTaskCount, int skippedTaskCount,
            int realtimeAgents, int highQAgents,
            int cachedAgents, int skippedAgents
    ) {}

    /** Per-task execution timing from BudgetGovernor FrameBudgetReport. */
    public record ExecutionTelemetry(
            double frameTotalMs,
            double hottestTaskMs,
            String hottestTaskId,
            int taskCount,
            long completedInferences,
            long timeoutInferences
    ) {}

    /**
     * Complete AI telemetry snapshot across all four planes plus execution timing.
     */
    public record AiTelemetrySnapshot(
            SimulationTelemetry simulation,
            CognitionTelemetry cognition,
            PlanningTelemetry planning,
            BudgetTelemetry budget,
            ExecutionTelemetry execution
    ) {
        /** Backwards-compatible constructor without execution telemetry. */
        public AiTelemetrySnapshot(
                SimulationTelemetry simulation, CognitionTelemetry cognition,
                PlanningTelemetry planning, BudgetTelemetry budget) {
            this(simulation, cognition, planning, budget,
                    new ExecutionTelemetry(0, 0, "", 0, 0, 0));
        }
    }
}
