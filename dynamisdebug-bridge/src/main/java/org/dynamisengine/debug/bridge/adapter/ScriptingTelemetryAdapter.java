package org.dynamisengine.debug.bridge.adapter;

import org.dynamisengine.debug.api.DebugCategory;
import org.dynamisengine.debug.api.DebugSnapshot;
import org.dynamisengine.debug.bridge.TelemetryAdapter;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Adapts the DynamisScripting canonical world simulation engine into
 * a unified {@link DebugSnapshot}.
 *
 * <p>DynamisScripting is not "scripting" in the usual sense — it is the
 * canonical world mutation, adjudication, and causality engine. This adapter
 * captures five telemetry planes:
 *
 * <ol>
 *   <li><b>Canon</b> — world truth: commit count/rate, causal links, log size</li>
 *   <li><b>Oracle</b> — law enforcement: validate/shape/commit counts, rejections, latency</li>
 *   <li><b>Chronicler</b> — world pressure: proposed events, triggers, overdue deadlines</li>
 *   <li><b>Percept</b> — agent delivery: percept count, fidelity tiers, stale/dropped</li>
 *   <li><b>Degradation</b> — agent cognitive tiers: tier distribution, max lag, debt</li>
 * </ol>
 *
 * <p>Read-only consumer — no debug concepts pushed back into DynamisScripting.
 */
public final class ScriptingTelemetryAdapter implements TelemetryAdapter<ScriptingTelemetryAdapter.ScriptingTelemetrySnapshot> {

    @Override
    public String subsystemName() { return "scripting"; }

    @Override
    public DebugCategory category() { return DebugCategory.SCRIPTING; }

    @Override
    public DebugSnapshot adapt(ScriptingTelemetrySnapshot t, long frameNumber) {
        Map<String, Double> metrics = extractMetrics(t);

        Map<String, Boolean> flags = new LinkedHashMap<>();
        flags.put("hasDegradedAgents", t.degradation().tier1Count() + t.degradation().tier2Count() + t.degradation().tier3Count() > 0);
        flags.put("hasUncommittedEvents", t.canon().eventsProposed() > t.canon().eventsCommitted());
        flags.put("oracleRejecting", t.oracle().validateFailures() > 0);
        flags.put("chroniclerBacklog", t.chronicler().pendingWorldEvents() > 10);
        flags.put("perceptStaleness", t.percept().stalePerceptCount() > 0);
        flags.put("tier3Active", t.degradation().tier3Count() > 0);
        flags.put("evaluationErrors", t.evaluationErrors() > 0);
        long totalCacheAccess = t.cacheHits() + t.cacheMisses();
        flags.put("cacheMissHigh", totalCacheAccess > 0 && (double) t.cacheMisses() / totalCacheAccess > 0.5);

        double tickMs = t.canon().tickDurationNanos() / 1_000_000.0;
        double chroniclerMs = t.chroniclerNanos() / 1_000_000.0;
        String text = String.format(java.util.Locale.ROOT,
                "canon: proposed=%d committed=%d tick=%.2fms chronicler=%.2fms | " +
                "oracle: validate=%d reject=%d | chronicler: pending=%d triggered=%d | " +
                "percepts=%d stale=%d | tiers: 0=%d 1=%d 2=%d 3=%d | " +
                "cache: %d/%d errors=%d",
                t.canon().eventsProposed(), t.canon().eventsCommitted(), tickMs, chroniclerMs,
                t.oracle().validateCount(), t.oracle().validateFailures(),
                t.chronicler().pendingWorldEvents(), t.chronicler().triggeredThisTick(),
                t.percept().perceptsEmitted(), t.percept().stalePerceptCount(),
                t.degradation().tier0Count(), t.degradation().tier1Count(),
                t.degradation().tier2Count(), t.degradation().tier3Count(),
                t.cacheHits(), t.cacheMisses(), t.evaluationErrors());

        return new DebugSnapshot(frameNumber, System.currentTimeMillis(),
                subsystemName(), category(), metrics, flags, text);
    }

    @Override
    public Map<String, Double> extractMetrics(ScriptingTelemetrySnapshot t) {
        Map<String, Double> metrics = new LinkedHashMap<>();

        // Canon plane
        metrics.put("canon.eventsProposed", (double) t.canon().eventsProposed());
        metrics.put("canon.eventsCommitted", (double) t.canon().eventsCommitted());
        metrics.put("canon.tickDurationMs", t.canon().tickDurationNanos() / 1_000_000.0);
        metrics.put("canon.logSize", (double) t.canon().logSize());
        metrics.put("canon.latestCommitId", (double) t.canon().latestCommitId());
        metrics.put("canon.currentTick", (double) t.canon().currentTick());

        // Oracle plane
        metrics.put("oracle.validateCount", (double) t.oracle().validateCount());
        metrics.put("oracle.validateFailures", (double) t.oracle().validateFailures());
        metrics.put("oracle.shapeCount", (double) t.oracle().shapeCount());
        metrics.put("oracle.commitCount", (double) t.oracle().commitCount());
        metrics.put("oracle.commitFailures", (double) t.oracle().commitFailures());
        metrics.put("oracle.rejectedIntents", (double) t.oracle().rejectedIntents());

        // Chronicler plane
        metrics.put("chronicler.pendingWorldEvents", (double) t.chronicler().pendingWorldEvents());
        metrics.put("chronicler.triggeredThisTick", (double) t.chronicler().triggeredThisTick());
        metrics.put("chronicler.overdueDeadlines", (double) t.chronicler().overdueDeadlines());
        metrics.put("chronicler.graphEvaluations", (double) t.chronicler().graphEvaluations());

        // Percept plane
        metrics.put("percept.perceptsEmitted", (double) t.percept().perceptsEmitted());
        metrics.put("percept.agentsReceiving", (double) t.percept().agentsReceiving());
        metrics.put("percept.stalePerceptCount", (double) t.percept().stalePerceptCount());
        metrics.put("percept.droppedPerceptCount", (double) t.percept().droppedPerceptCount());
        metrics.put("percept.degradedDeliveries", (double) t.percept().degradedDeliveries());

        // Degradation plane
        metrics.put("degradation.tier0Count", (double) t.degradation().tier0Count());
        metrics.put("degradation.tier1Count", (double) t.degradation().tier1Count());
        metrics.put("degradation.tier2Count", (double) t.degradation().tier2Count());
        metrics.put("degradation.tier3Count", (double) t.degradation().tier3Count());
        metrics.put("degradation.maxLagTicks", (double) t.degradation().maxLagTicks());
        metrics.put("degradation.averageLagTicks", t.degradation().averageLagTicks());

        // DSL
        metrics.put("dsl.cacheSize", (double) t.dslCacheSize());
        metrics.put("dsl.cacheHits", (double) t.cacheHits());
        metrics.put("dsl.cacheMisses", (double) t.cacheMisses());
        metrics.put("budget.remaining", (double) t.budgetRemaining());

        // Execution timing
        metrics.put("chronicler.executionMs", t.chroniclerNanos() / 1_000_000.0);
        metrics.put("evaluation.errorCount", (double) t.evaluationErrors());

        return metrics;
    }

    /** Canon truth plane telemetry. */
    public record CanonTelemetry(
            int eventsProposed, int eventsCommitted,
            long tickDurationNanos, long logSize,
            long latestCommitId, long currentTick
    ) {}

    /** Oracle adjudication plane telemetry. */
    public record OracleTelemetry(
            int validateCount, int validateFailures,
            int shapeCount, int commitCount,
            int commitFailures, int rejectedIntents
    ) {}

    /** Chronicler world-pressure plane telemetry. */
    public record ChroniclerTelemetry(
            int pendingWorldEvents, int triggeredThisTick,
            int overdueDeadlines, int graphEvaluations
    ) {}

    /** Percept delivery plane telemetry. */
    public record PerceptTelemetry(
            int perceptsEmitted, int agentsReceiving,
            int stalePerceptCount, int droppedPerceptCount,
            int degradedDeliveries
    ) {}

    /** Agent cognitive degradation plane telemetry. */
    public record DegradationTelemetry(
            int tier0Count, int tier1Count,
            int tier2Count, int tier3Count,
            int maxLagTicks, double averageLagTicks
    ) {}

    /**
     * Complete scripting telemetry snapshot across all five planes.
     */
    public record ScriptingTelemetrySnapshot(
            CanonTelemetry canon,
            OracleTelemetry oracle,
            ChroniclerTelemetry chronicler,
            PerceptTelemetry percept,
            DegradationTelemetry degradation,
            int dslCacheSize,
            long budgetRemaining,
            long chroniclerNanos,
            long evaluationErrors,
            long cacheHits,
            long cacheMisses
    ) {
        /** Backwards-compatible constructor for existing callers. */
        public ScriptingTelemetrySnapshot(
                CanonTelemetry canon, OracleTelemetry oracle,
                ChroniclerTelemetry chronicler, PerceptTelemetry percept,
                DegradationTelemetry degradation, int dslCacheSize, long budgetRemaining) {
            this(canon, oracle, chronicler, percept, degradation, dslCacheSize, budgetRemaining,
                    0, 0, 0, 0);
        }
    }
}
