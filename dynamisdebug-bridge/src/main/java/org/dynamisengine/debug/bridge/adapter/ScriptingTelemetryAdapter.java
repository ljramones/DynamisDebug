package org.dynamisengine.debug.bridge.adapter;

import org.dynamisengine.debug.api.DebugCategory;
import org.dynamisengine.debug.api.DebugSnapshot;
import org.dynamisengine.debug.bridge.TelemetryAdapter;
import org.dynamisengine.scripting.runtime.RuntimeTickResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Adapts scripting runtime tick results into a unified {@link DebugSnapshot}.
 *
 * <p>Captures canonical event flow (proposed/committed), tick duration,
 * agent degradation, and DSL cache state.
 *
 * <p>Read-only consumer — no debug concepts pushed back into DynamisScripting.
 */
public final class ScriptingTelemetryAdapter implements TelemetryAdapter<ScriptingTelemetryAdapter.ScriptingTelemetrySnapshot> {

    @Override
    public String subsystemName() { return "scripting"; }

    @Override
    public DebugCategory category() { return DebugCategory.SCRIPTING; }

    @Override
    public DebugSnapshot adapt(ScriptingTelemetrySnapshot telemetry, long frameNumber) {
        Map<String, Double> metrics = extractMetrics(telemetry);

        Map<String, Boolean> flags = new LinkedHashMap<>();
        flags.put("hasDegradedAgents", telemetry.degradedAgentCount() > 0);
        flags.put("hasUncommittedEvents", telemetry.eventsProposed() > telemetry.eventsCommitted());

        double tickMs = telemetry.tickDurationNanos() / 1_000_000.0;
        String text = String.format(java.util.Locale.ROOT,
                "proposed=%d committed=%d tick=%.2fms agents=%d degraded=%d dslCache=%d",
                telemetry.eventsProposed(), telemetry.eventsCommitted(), tickMs,
                telemetry.agentCount(), telemetry.degradedAgentCount(), telemetry.dslCacheSize());

        return new DebugSnapshot(frameNumber, System.currentTimeMillis(),
                subsystemName(), category(), metrics, flags, text);
    }

    @Override
    public Map<String, Double> extractMetrics(ScriptingTelemetrySnapshot telemetry) {
        Map<String, Double> metrics = new LinkedHashMap<>();
        metrics.put("eventsProposed", (double) telemetry.eventsProposed());
        metrics.put("eventsCommitted", (double) telemetry.eventsCommitted());
        metrics.put("tickDurationNanos", (double) telemetry.tickDurationNanos());
        metrics.put("tickDurationMs", telemetry.tickDurationNanos() / 1_000_000.0);
        metrics.put("agentCount", (double) telemetry.agentCount());
        metrics.put("degradedAgentCount", (double) telemetry.degradedAgentCount());
        metrics.put("dslCacheSize", (double) telemetry.dslCacheSize());
        metrics.put("canonLogSize", (double) telemetry.canonLogSize());
        metrics.put("budgetRemaining", (double) telemetry.budgetRemaining());
        return metrics;
    }

    /**
     * Bundles scripting runtime tick result with supplementary state.
     *
     * @param eventsProposed     WorldEvents proposed this tick
     * @param eventsCommitted    WorldEvents committed this tick
     * @param tickDurationNanos  wall-clock tick duration
     * @param agentCount         registered agents
     * @param degradedAgentCount agents in degraded cognitive tier
     * @param dslCacheSize       compiled DSL expression cache size
     * @param canonLogSize       canonical event log size
     * @param budgetRemaining    remaining budget capacity
     */
    public record ScriptingTelemetrySnapshot(
            int eventsProposed,
            int eventsCommitted,
            long tickDurationNanos,
            int agentCount,
            int degradedAgentCount,
            int dslCacheSize,
            long canonLogSize,
            long budgetRemaining
    ) {
        /** Build from a RuntimeTickResult plus supplementary state. */
        public static ScriptingTelemetrySnapshot from(
                RuntimeTickResult result, int agentCount, int degradedAgentCount,
                int dslCacheSize, long canonLogSize, long budgetRemaining
        ) {
            return new ScriptingTelemetrySnapshot(
                    result.worldEventsProposed(), result.worldEventsCommitted(),
                    result.tickDurationNanos(), agentCount, degradedAgentCount,
                    dslCacheSize, canonLogSize, budgetRemaining);
        }
    }
}
