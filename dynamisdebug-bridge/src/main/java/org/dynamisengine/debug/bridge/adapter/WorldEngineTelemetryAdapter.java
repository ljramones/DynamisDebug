package org.dynamisengine.debug.bridge.adapter;

import org.dynamisengine.debug.api.DebugCategory;
import org.dynamisengine.debug.api.DebugSnapshot;
import org.dynamisengine.debug.bridge.TelemetryAdapter;
import org.dynamisengine.worldengine.api.WorldEngineState;
import org.dynamisengine.worldengine.api.telemetry.EngineTelemetry;
import org.dynamisengine.worldengine.api.telemetry.SubsystemHealth;
import org.dynamisengine.worldengine.api.telemetry.WorldTelemetrySnapshot;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Adapts {@link WorldTelemetrySnapshot} into a unified {@link DebugSnapshot}.
 *
 * <p>This is the executive summary adapter — the top-level view of the
 * entire engine. It captures:
 *
 * <ol>
 *   <li><b>Engine timing</b> — tick rate, budget utilization, last/avg/max tick duration</li>
 *   <li><b>Lifecycle state</b> — RUNNING, PAUSED, FAULTED, etc.</li>
 *   <li><b>Subsystem health rollup</b> — healthy/degraded/faulted counts</li>
 *   <li><b>Error state</b> — latest engine error, per-subsystem errors</li>
 * </ol>
 *
 * <p>Read-only consumer — no debug concepts pushed back into DynamisWorldEngine.
 */
public final class WorldEngineTelemetryAdapter implements TelemetryAdapter<WorldTelemetrySnapshot> {

    @Override
    public String subsystemName() { return "worldengine"; }

    @Override
    public DebugCategory category() { return DebugCategory.ENGINE; }

    @Override
    public DebugSnapshot adapt(WorldTelemetrySnapshot t, long frameNumber) {
        Map<String, Double> metrics = extractMetrics(t);

        EngineTelemetry engine = t.engine();
        Map<String, Boolean> flags = new LinkedHashMap<>();
        flags.put("running", engine.state() == WorldEngineState.RUNNING);
        flags.put("paused", engine.state() == WorldEngineState.PAUSED);
        flags.put("faulted", engine.state() == WorldEngineState.FAULTED);
        flags.put("hasIssues", t.hasIssues());
        flags.put("overBudget", engine.budgetPercent() > 100.0);

        String text = String.format(java.util.Locale.ROOT,
                "%s | tick=%d | %.0fHz | dt=%.1fms (avg=%.1f max=%.1f) | budget=%.0f%% | " +
                "subsystems: %d healthy %d degraded %d faulted",
                engine.state(), engine.tick(), (double) engine.tickRate(),
                engine.lastTickDurationMs(), engine.avgTickDurationMs(), engine.maxTickDurationMs(),
                engine.budgetPercent(),
                t.healthyCount(), t.degradedCount(), t.faultedCount());

        if (t.lastError() != null) {
            text += " | error: " + t.lastError();
        }

        return new DebugSnapshot(frameNumber, System.currentTimeMillis(),
                subsystemName(), category(), metrics, flags, text);
    }

    @Override
    public Map<String, Double> extractMetrics(WorldTelemetrySnapshot t) {
        Map<String, Double> metrics = new LinkedHashMap<>();
        EngineTelemetry e = t.engine();

        // Engine timing
        metrics.put("tick", (double) e.tick());
        metrics.put("uptimeSeconds", e.uptimeSeconds());
        metrics.put("lastTickDurationMs", e.lastTickDurationMs());
        metrics.put("avgTickDurationMs", e.avgTickDurationMs());
        metrics.put("maxTickDurationMs", e.maxTickDurationMs());
        metrics.put("targetTickMs", e.targetTickMs());
        metrics.put("tickRate", (double) e.tickRate());
        metrics.put("budgetPercent", e.budgetPercent());

        // Subsystem health rollup
        metrics.put("healthySubsystemCount", (double) t.healthyCount());
        metrics.put("degradedSubsystemCount", (double) t.degradedCount());
        metrics.put("faultedSubsystemCount", (double) t.faultedCount());
        metrics.put("totalSubsystemCount", (double) t.subsystems().size());

        // Per-subsystem health state (1.0=healthy, 0.5=degraded, 0.0=faulted, -1.0=absent)
        for (var entry : t.subsystems().entrySet()) {
            SubsystemHealth health = entry.getValue().health();
            double healthValue = switch (health.state()) {
                case HEALTHY -> 1.0;
                case DEGRADED -> 0.5;
                case FAULTED -> 0.0;
                case ABSENT -> -1.0;
            };
            metrics.put("subsystem." + entry.getKey() + ".health", healthValue);
        }

        return metrics;
    }
}
