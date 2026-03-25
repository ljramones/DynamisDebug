package org.dynamisengine.debug.bridge.adapter;

import org.dynamisengine.debug.api.DebugSeverity;
import org.dynamisengine.debug.api.WatchdogRule;

import java.util.List;

/**
 * Factory for ECS system timing watchdog rules.
 *
 * <p>These rules detect ECS timing anomalies: frame budget overruns,
 * individual system spikes, and dominance by a single system.
 *
 * <p>Register on {@link org.dynamisengine.debug.core.DebugWatchdog}
 * to enable automatic alerting.
 */
public final class EcsWatchdogRules {

    private EcsWatchdogRules() {}

    /** All ECS timing watchdog rules. */
    public static List<WatchdogRule> all() {
        return List.of(
            frameOverBudgetWarning(),
            frameOverBudgetError(),
            dominantSystem()
        );
    }

    /** ECS frame total exceeds warning budget (8ms). */
    public static WatchdogRule frameOverBudgetWarning() {
        return new WatchdogRule(
            "ecs.frameOverBudget",
            "ecs",
            "ecs.frameTotalMs",
            8.0,
            WatchdogRule.Comparison.GREATER_THAN,
            DebugSeverity.WARNING,
            "ECS frame total > 8ms",
            60
        );
    }

    /** ECS frame total exceeds error budget (12ms). */
    public static WatchdogRule frameOverBudgetError() {
        return new WatchdogRule(
            "ecs.frameCritical",
            "ecs",
            "ecs.frameTotalMs",
            12.0,
            WatchdogRule.Comparison.GREATER_THAN,
            DebugSeverity.ERROR,
            "ECS frame total > 12ms",
            30
        );
    }

    /** Single system dominates the frame (>50% of total). */
    public static WatchdogRule dominantSystem() {
        return new WatchdogRule(
            "ecs.systemDominance",
            "ecs",
            "ecs.dominantSystemTimeMs",
            4.0, // triggers when dominant system > 4ms (heuristic for >50% of 8ms budget)
            WatchdogRule.Comparison.GREATER_THAN,
            DebugSeverity.WARNING,
            "Single ECS system > 4ms (potential dominance)",
            120
        );
    }
}
