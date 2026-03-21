package org.dynamisengine.debug.bridge.adapter;

import org.dynamisengine.debug.api.DebugSeverity;
import org.dynamisengine.debug.api.WatchdogRule;

import java.util.List;

/**
 * Factory for high-value watchdog rules across the simulation layer
 * (Physics, Scripting, AI).
 *
 * <p>These rules detect anomalies that are hard to diagnose without
 * unified cross-subsystem observability. Register them on the
 * {@link org.dynamisengine.debug.core.DebugWatchdog} to enable
 * automatic alerting.
 */
public final class SimulationWatchdogRules {

    private SimulationWatchdogRules() {}

    /** All simulation watchdog rules. */
    public static List<WatchdogRule> all() {
        return List.of(
                // --- Physics ---
                physicsStepTimeHigh(),
                physicsSleepingRatioHigh(),

                // --- Scripting / Canon ---
                canonCommitStall(),
                oracleRejectSpike(),
                chroniclerBacklogSpike(),
                perceptStalenessHigh(),
                degradationTier3Spike(),

                // --- AI ---
                aiBudgetExceeded(),
                aiDegradeSpike(),
                aiInferenceBacklog(),
                aiReplanThrashing(),
                aiPerceptStaleness()
        );
    }

    // --- Physics rules ---

    public static WatchdogRule physicsStepTimeHigh() {
        return WatchdogRule.above("physics.stepTimeHigh", "physics", "stepTimeMs",
                16.0, DebugSeverity.WARNING, "Physics step exceeds frame budget");
    }

    public static WatchdogRule physicsSleepingRatioHigh() {
        return WatchdogRule.above("physics.sleepingRatioHigh", "physics", "sleepingBodyCount",
                500.0, DebugSeverity.INFO, "Large number of sleeping bodies");
    }

    // --- Scripting / Canon rules ---

    /** No commits when activity is expected. */
    public static WatchdogRule canonCommitStall() {
        return WatchdogRule.below("canon.commitStall", "scripting", "canon.eventsCommitted",
                1.0, DebugSeverity.WARNING, "No canon commits this tick");
    }

    /** Oracle validation failures spiking. */
    public static WatchdogRule oracleRejectSpike() {
        return WatchdogRule.above("oracle.rejectSpike", "scripting", "oracle.validateFailures",
                5.0, DebugSeverity.WARNING, "Oracle rejecting many intents");
    }

    /** Chronicler has too many pending world events. */
    public static WatchdogRule chroniclerBacklogSpike() {
        return WatchdogRule.above("chronicler.backlogSpike", "scripting", "chronicler.pendingWorldEvents",
                20.0, DebugSeverity.WARNING, "Chronicler world event backlog high");
    }

    /** Stale percepts accumulating — agents getting outdated world view. */
    public static WatchdogRule perceptStalenessHigh() {
        return WatchdogRule.above("percept.stalenessHigh", "scripting", "percept.stalePerceptCount",
                10.0, DebugSeverity.WARNING, "Stale percepts accumulating");
    }

    /** Too many agents in must-not-act-wrong tier. */
    public static WatchdogRule degradationTier3Spike() {
        return WatchdogRule.above("degradation.tier3Spike", "scripting", "degradation.tier3Count",
                5.0, DebugSeverity.ERROR, "Agents in must-not-act-wrong tier");
    }

    // --- AI rules ---

    /** AI budget exceeded — tasks being degraded/skipped. */
    public static WatchdogRule aiBudgetExceeded() {
        return WatchdogRule.above("ai.budgetExceeded", "ai", "budget.budgetUsedPercent",
                100.0, DebugSeverity.WARNING, "AI frame budget exceeded");
    }

    /** Too many agents running degraded. */
    public static WatchdogRule aiDegradeSpike() {
        return WatchdogRule.above("ai.degradeSpike", "ai", "budget.degradedTaskCount",
                10.0, DebugSeverity.WARNING, "Many AI tasks running degraded");
    }

    /** LLM inference requests backing up. */
    public static WatchdogRule aiInferenceBacklog() {
        return WatchdogRule.above("ai.inferenceBacklog", "ai", "cognition.inferenceQueueDepth",
                10.0, DebugSeverity.WARNING, "AI inference queue backing up");
    }

    /** Agents thrashing plan selection — replans exceed new plans. */
    public static WatchdogRule aiReplanThrashing() {
        return WatchdogRule.above("ai.replanThrashing", "ai", "planning.replanCount",
                20.0, DebugSeverity.WARNING, "AI plan thrashing detected");
    }

    /** AI percepts are stale relative to world state. */
    public static WatchdogRule aiPerceptStaleness() {
        return WatchdogRule.above("ai.perceptStaleness", "ai", "cognition.stalePerceptLoad",
                10.0, DebugSeverity.WARNING, "AI perception running stale");
    }
}
