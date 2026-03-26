package org.dynamisengine.debug.bridge.adapter;

import org.dynamisengine.debug.api.DebugSeverity;
import org.dynamisengine.debug.api.WatchdogRule;

import java.util.List;

/**
 * Factory for high-value watchdog rules across all runtime subsystems.
 *
 * <p>Rules are organized by subsystem and tuned with severity escalation
 * (WARNING for early detection, ERROR for severe degradation) and per-rule
 * cooldowns (shorter for severe rules, longer for noisy ones).
 *
 * <p>Threshold rationale:
 * <ul>
 *   <li>Frame budget: 16.67ms at 60fps. Individual subsystems get ~8ms WARNING, ~12ms ERROR.</li>
 *   <li>GPU: separate budget from CPU; same 8/12ms tiers for the GPU-bound case.</li>
 *   <li>Counts: per-frame thresholds only — cumulative metrics are not watchdog-suitable.</li>
 *   <li>Cooldowns: 30 frames for ERROR, 60 for WARNING, 120 for INFO/noisy rules.</li>
 * </ul>
 */
public final class SimulationWatchdogRules {

    private SimulationWatchdogRules() {}

    /** All watchdog rules (25 rules across 7 subsystem categories). */
    public static List<WatchdogRule> all() {
        return List.of(
                // --- GPU (4 rules) ---
                gpuFrameOverBudgetWarning(),
                gpuFrameOverBudgetError(),
                gpuPassSpike(),
                gpuBound(),

                // --- Physics (3 rules) ---
                physicsStepWarning(),
                physicsStepError(),
                physicsSleepingRatioHigh(),

                // --- Scripting / Canon (6 rules) ---
                canonCommitStall(),
                oracleRejectSpike(),
                chroniclerBacklogSpike(),
                perceptStalenessHigh(),
                degradationTier3Spike(),
                scriptingFrameOverBudget(),

                // --- Content (2 rules) ---
                contentLoadFailures(),
                contentCacheMissHigh(),

                // --- Threading (3 rules) ---
                threadingCognitionQueueBacklog(),
                threadingGpuUploadBacklog(),
                threadingEventBusDeadLetters(),

                // --- AI (5 rules) ---
                aiBudgetExceeded(),
                aiDegradeSpike(),
                aiFrameOverBudgetWarning(),
                aiFrameOverBudgetError(),
                aiTimeoutBurst()
        );
    }

    // ========================================================================
    // GPU rules — hardest to diagnose, highest frame-budget impact
    // ========================================================================

    /** GPU frame time exceeds half the frame budget. */
    public static WatchdogRule gpuFrameOverBudgetWarning() {
        return new WatchdogRule("gpu.frameOverBudget", "gpu", "gpu.frameTimeMs",
                8.0, WatchdogRule.Comparison.GREATER_THAN,
                DebugSeverity.WARNING, "GPU frame > 8ms", 60);
    }

    /** GPU frame time exceeds most of the frame budget — near GPU-bound. */
    public static WatchdogRule gpuFrameOverBudgetError() {
        return new WatchdogRule("gpu.frameCritical", "gpu", "gpu.frameTimeMs",
                12.0, WatchdogRule.Comparison.GREATER_THAN,
                DebugSeverity.ERROR, "GPU frame > 12ms (near GPU-bound)", 30);
    }

    /** Any single GPU pass exceeds 6ms — likely dominating the frame. */
    public static WatchdogRule gpuPassSpike() {
        return new WatchdogRule("gpu.passSpike", "gpu", "gpu.geometryPassMs",
                6.0, WatchdogRule.Comparison.GREATER_THAN,
                DebugSeverity.WARNING, "GPU geometry pass > 6ms", 60);
    }

    /** GPU frame time exceeds CPU frame time — rendering is the bottleneck. */
    public static WatchdogRule gpuBound() {
        return new WatchdogRule("gpu.bound", "lightengine", "gpuFrameMs",
                8.0, WatchdogRule.Comparison.GREATER_THAN,
                DebugSeverity.WARNING, "GPU-bound rendering detected", 120);
    }

    // ========================================================================
    // Physics rules — direct frame budget consumers
    // ========================================================================

    /** Physics step approaching half the frame budget. */
    public static WatchdogRule physicsStepWarning() {
        return new WatchdogRule("physics.stepTimeHigh", "physics", "stepTimeMs",
                8.0, WatchdogRule.Comparison.GREATER_THAN,
                DebugSeverity.WARNING, "Physics step > 8ms", 60);
    }

    /** Physics step consuming most of the frame budget. */
    public static WatchdogRule physicsStepError() {
        return new WatchdogRule("physics.stepCritical", "physics", "stepTimeMs",
                12.0, WatchdogRule.Comparison.GREATER_THAN,
                DebugSeverity.ERROR, "Physics step > 12ms (critical)", 30);
    }

    /** Large number of sleeping bodies — potential memory/broadphase waste. */
    public static WatchdogRule physicsSleepingRatioHigh() {
        return new WatchdogRule("physics.sleepingRatioHigh", "physics", "sleepingBodyCount",
                500.0, WatchdogRule.Comparison.GREATER_THAN,
                DebugSeverity.INFO, "Large sleeping body count", 120);
    }

    // ========================================================================
    // Scripting / Canon rules — world mutation health
    // ========================================================================

    /** No commits when activity is expected. */
    public static WatchdogRule canonCommitStall() {
        return new WatchdogRule("canon.commitStall", "scripting", "canon.eventsCommitted",
                1.0, WatchdogRule.Comparison.LESS_THAN,
                DebugSeverity.WARNING, "No canon commits this tick", 120);
    }

    /** Oracle validation failures spiking. */
    public static WatchdogRule oracleRejectSpike() {
        return new WatchdogRule("oracle.rejectSpike", "scripting", "oracle.validateFailures",
                5.0, WatchdogRule.Comparison.GREATER_THAN,
                DebugSeverity.WARNING, "Oracle rejecting many intents", 60);
    }

    /** Chronicler has too many pending world events. */
    public static WatchdogRule chroniclerBacklogSpike() {
        return new WatchdogRule("chronicler.backlogSpike", "scripting", "chronicler.pendingWorldEvents",
                20.0, WatchdogRule.Comparison.GREATER_THAN,
                DebugSeverity.WARNING, "Chronicler world event backlog high", 60);
    }

    /** Stale percepts accumulating — agents getting outdated world view. */
    public static WatchdogRule perceptStalenessHigh() {
        return new WatchdogRule("percept.stalenessHigh", "scripting", "percept.stalePerceptCount",
                10.0, WatchdogRule.Comparison.GREATER_THAN,
                DebugSeverity.WARNING, "Stale percepts accumulating", 60);
    }

    /** Too many agents in must-not-act-wrong tier. */
    public static WatchdogRule degradationTier3Spike() {
        return new WatchdogRule("degradation.tier3Spike", "scripting", "degradation.tier3Count",
                3.0, WatchdogRule.Comparison.GREATER_THAN,
                DebugSeverity.ERROR, "Agents in must-not-act-wrong tier", 30);
    }

    /** Scripting tick duration over budget. */
    public static WatchdogRule scriptingFrameOverBudget() {
        return new WatchdogRule("scripting.frameOverBudget", "scripting", "canon.tickDurationMs",
                4.0, WatchdogRule.Comparison.GREATER_THAN,
                DebugSeverity.WARNING, "Scripting tick > 4ms", 60);
    }

    // ========================================================================
    // Content rules — asset loading health
    // ========================================================================

    /** Asset load failures accumulating. */
    public static WatchdogRule contentLoadFailures() {
        return new WatchdogRule("content.loadFailures", "content", "failedResolutions",
                3.0, WatchdogRule.Comparison.GREATER_THAN,
                DebugSeverity.WARNING, "Asset load failures accumulating", 60);
    }

    /** Content cache miss rate high. */
    public static WatchdogRule contentCacheMissHigh() {
        return new WatchdogRule("content.cacheMissHigh", "content", "cacheMisses",
                20.0, WatchdogRule.Comparison.GREATER_THAN,
                DebugSeverity.WARNING, "Content cache misses high", 120);
    }

    // ========================================================================
    // Threading rules — executor health
    // ========================================================================

    /** AI cognition inference queue backing up. */
    public static WatchdogRule threadingCognitionQueueBacklog() {
        return new WatchdogRule("threading.cognitionQueueBacklog", "threading",
                "threading.cognition.queueDepth",
                8.0, WatchdogRule.Comparison.GREATER_THAN,
                DebugSeverity.WARNING, "AI cognition queue depth high", 60);
    }

    /** GPU upload backlog — mesh uploads not keeping up. */
    public static WatchdogRule threadingGpuUploadBacklog() {
        return new WatchdogRule("threading.gpuUploadBacklog", "threading",
                "threading.gpuUpload.backlog",
                5.0, WatchdogRule.Comparison.GREATER_THAN,
                DebugSeverity.WARNING, "GPU upload backlog growing", 60);
    }

    /** Event bus dead letters — events with no listeners. */
    public static WatchdogRule threadingEventBusDeadLetters() {
        return new WatchdogRule("threading.eventBusDeadLetters", "threading",
                "threading.eventBus.deadLetters",
                10.0, WatchdogRule.Comparison.GREATER_THAN,
                DebugSeverity.WARNING, "Event bus dead letters accumulating", 120);
    }

    // ========================================================================
    // AI rules — agent runtime health
    // ========================================================================

    /** AI budget exceeded — tasks being degraded/skipped. */
    public static WatchdogRule aiBudgetExceeded() {
        return new WatchdogRule("ai.budgetExceeded", "ai", "budget.budgetUsedPercent",
                100.0, WatchdogRule.Comparison.GREATER_THAN,
                DebugSeverity.WARNING, "AI frame budget exceeded", 60);
    }

    /** Too many agents running degraded. */
    public static WatchdogRule aiDegradeSpike() {
        return new WatchdogRule("ai.degradeSpike", "ai", "budget.degradedTaskCount",
                10.0, WatchdogRule.Comparison.GREATER_THAN,
                DebugSeverity.WARNING, "Many AI tasks degraded", 60);
    }

    /** AI frame total exceeds budget. */
    public static WatchdogRule aiFrameOverBudgetWarning() {
        return new WatchdogRule("ai.frameOverBudget", "ai", "execution.frameTotalMs",
                8.0, WatchdogRule.Comparison.GREATER_THAN,
                DebugSeverity.WARNING, "AI frame > 8ms budget", 60);
    }

    /** AI frame total severely over budget. */
    public static WatchdogRule aiFrameOverBudgetError() {
        return new WatchdogRule("ai.frameCritical", "ai", "execution.frameTotalMs",
                12.0, WatchdogRule.Comparison.GREATER_THAN,
                DebugSeverity.ERROR, "AI frame > 12ms (critical)", 30);
    }

    /** AI inference timeouts spiking. */
    public static WatchdogRule aiTimeoutBurst() {
        return new WatchdogRule("ai.timeoutBurst", "ai", "execution.timeoutInferences",
                3.0, WatchdogRule.Comparison.GREATER_THAN,
                DebugSeverity.WARNING, "AI inference timeouts spiking", 60);
    }
}
