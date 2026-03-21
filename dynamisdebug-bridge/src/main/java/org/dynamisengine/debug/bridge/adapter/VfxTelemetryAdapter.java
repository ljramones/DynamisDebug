package org.dynamisengine.debug.bridge.adapter;

import org.dynamisengine.debug.api.DebugCategory;
import org.dynamisengine.debug.api.DebugSnapshot;
import org.dynamisengine.debug.bridge.TelemetryAdapter;
import org.dynamisengine.vfx.api.VfxStats;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Adapts {@link VfxStats} into a unified {@link DebugSnapshot}.
 *
 * <p>Captures active effects, particle count, budget utilization,
 * rejection/clamp/eviction events, and GPU memory.
 *
 * <p>Read-only consumer — no debug concepts pushed back into DynamisVFX.
 */
public final class VfxTelemetryAdapter implements TelemetryAdapter<VfxStats> {

    @Override
    public String subsystemName() { return "vfx"; }

    @Override
    public DebugCategory category() { return DebugCategory.RENDERING; }

    @Override
    public DebugSnapshot adapt(VfxStats stats, long frameNumber) {
        Map<String, Double> metrics = extractMetrics(stats);

        var budget = stats.budgetStats();
        Map<String, Boolean> flags = new LinkedHashMap<>();
        flags.put("budgetExhausted", budget != null && budget.remainingBudget() == 0);
        flags.put("hasRejections", budget != null && budget.rejectedThisFrame() > 0);

        String text = String.format(java.util.Locale.ROOT,
                "effects=%d particles=%d budget=%d/%d gpu=%dB",
                stats.activeEffectCount(), stats.activeParticleCount(),
                budget != null ? budget.usedBudget() : 0,
                budget != null ? budget.totalBudget() : 0,
                stats.gpuMemoryBytes());

        return new DebugSnapshot(frameNumber, System.currentTimeMillis(),
                subsystemName(), category(), metrics, flags, text);
    }

    @Override
    public Map<String, Double> extractMetrics(VfxStats stats) {
        Map<String, Double> metrics = new LinkedHashMap<>();
        metrics.put("activeEffectCount", (double) stats.activeEffectCount());
        metrics.put("activeParticleCount", (double) stats.activeParticleCount());
        metrics.put("sleepingEmitterCount", (double) stats.sleepingEmitterCount());
        metrics.put("culledParticleCount", (double) stats.culledParticleCount());
        metrics.put("gpuMemoryBytes", (double) stats.gpuMemoryBytes());

        var budget = stats.budgetStats();
        if (budget != null) {
            metrics.put("budget.totalBudget", (double) budget.totalBudget());
            metrics.put("budget.usedBudget", (double) budget.usedBudget());
            metrics.put("budget.remainingBudget", (double) budget.remainingBudget());
            metrics.put("budget.rejectedThisFrame", (double) budget.rejectedThisFrame());
            metrics.put("budget.clampedThisFrame", (double) budget.clampedThisFrame());
            metrics.put("budget.evictedThisFrame", (double) budget.evictedThisFrame());
        }
        return metrics;
    }
}
