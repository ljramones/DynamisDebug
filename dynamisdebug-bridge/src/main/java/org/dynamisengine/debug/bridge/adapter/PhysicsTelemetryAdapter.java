package org.dynamisengine.debug.bridge.adapter;

import org.dynamisengine.debug.api.DebugCategory;
import org.dynamisengine.debug.api.DebugSnapshot;
import org.dynamisengine.debug.bridge.TelemetryAdapter;
import org.dynamisengine.physics.api.world.PhysicsStats;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Adapts {@link PhysicsStats} into a unified {@link DebugSnapshot}.
 *
 * <p>Captures body counts (active/sleeping), constraint count, step timing,
 * and phase breakdowns when available.
 *
 * <p>Read-only consumer — no debug concepts pushed back into DynamisPhysics.
 */
public final class PhysicsTelemetryAdapter implements TelemetryAdapter<PhysicsStats> {

    @Override
    public String subsystemName() { return "physics"; }

    @Override
    public DebugCategory category() { return DebugCategory.PHYSICS; }

    @Override
    public DebugSnapshot adapt(PhysicsStats stats, long frameNumber) {
        Map<String, Double> metrics = extractMetrics(stats);

        Map<String, Boolean> flags = Map.of(
                "hasSleepingBodies", stats.sleepingBodyCount() > 0,
                "stepTimeHigh", stats.stepTimeMs() > 16.0f
        );

        String text = String.format(java.util.Locale.ROOT,
                "bodies=%d active=%d sleeping=%d constraints=%d step=%.1fms",
                stats.bodyCount(), stats.activeBodyCount(), stats.sleepingBodyCount(),
                stats.constraintCount(), stats.stepTimeMs());

        return new DebugSnapshot(frameNumber, System.currentTimeMillis(),
                subsystemName(), category(), metrics, flags, text);
    }

    @Override
    public Map<String, Double> extractMetrics(PhysicsStats stats) {
        Map<String, Double> metrics = new LinkedHashMap<>();
        metrics.put("stepTimeMs", (double) stats.stepTimeMs());
        metrics.put("bodyCount", (double) stats.bodyCount());
        metrics.put("activeBodyCount", (double) stats.activeBodyCount());
        metrics.put("sleepingBodyCount", (double) stats.sleepingBodyCount());
        metrics.put("constraintCount", (double) stats.constraintCount());
        metrics.put("islandCount", (double) stats.islandCount());
        metrics.put("broadPhaseMs", (double) stats.broadPhaseMs());
        metrics.put("narrowPhaseMs", (double) stats.narrowPhaseMs());
        metrics.put("constraintSolveMs", (double) stats.constraintSolveMs());
        metrics.put("integrationMs", (double) stats.integrationMs());
        return metrics;
    }
}
