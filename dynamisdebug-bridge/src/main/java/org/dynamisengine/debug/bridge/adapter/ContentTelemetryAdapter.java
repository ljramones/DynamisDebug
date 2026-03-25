package org.dynamisengine.debug.bridge.adapter;

import org.dynamisengine.debug.api.DebugCategory;
import org.dynamisengine.debug.api.DebugSnapshot;
import org.dynamisengine.debug.bridge.TelemetryAdapter;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Adapts content system state into a unified {@link DebugSnapshot}.
 *
 * <p>Extracts manifest scope, cache state, and load/resolution metrics
 * from caller-provided telemetry snapshots.
 *
 * <p>DynamisContent provides built-in telemetry via DefaultAssetCache
 * (hit/miss counters) and DefaultAssetManager (load count, timing, failures).
 *
 * <p>Read-only consumer — no debug concepts pushed back into DynamisContent.
 */
public final class ContentTelemetryAdapter implements TelemetryAdapter<ContentTelemetryAdapter.ContentTelemetrySnapshot> {

    @Override
    public String subsystemName() { return "content"; }

    @Override
    public DebugCategory category() { return DebugCategory.CONTENT; }

    @Override
    public DebugSnapshot adapt(ContentTelemetrySnapshot telemetry, long frameNumber) {
        Map<String, Double> metrics = extractMetrics(telemetry);

        long totalAccess = telemetry.cacheHits() + telemetry.cacheMisses();
        Map<String, Boolean> flags = new LinkedHashMap<>();
        flags.put("hasFailures", telemetry.failedResolutions() > 0);
        flags.put("cacheMissHigh", totalAccess > 0 && (double) telemetry.cacheMisses() / totalAccess > 0.5);
        flags.put("loadSlow", telemetry.lastLoadNanos() > 50_000_000L);

        double lastLoadMs = telemetry.lastLoadNanos() / 1_000_000.0;
        String text = String.format(java.util.Locale.ROOT,
                "manifest=%d cached=%d loads=%d fails=%d hits=%d misses=%d lastLoad=%.1fms",
                telemetry.manifestEntryCount(), telemetry.cachedAssetCount(),
                telemetry.totalLoads(), telemetry.failedResolutions(),
                telemetry.cacheHits(), telemetry.cacheMisses(), lastLoadMs);

        return new DebugSnapshot(frameNumber, System.currentTimeMillis(),
                subsystemName(), category(), metrics, flags, text);
    }

    @Override
    public Map<String, Double> extractMetrics(ContentTelemetrySnapshot telemetry) {
        Map<String, Double> metrics = new LinkedHashMap<>();
        metrics.put("manifestEntryCount", (double) telemetry.manifestEntryCount());
        metrics.put("cachedAssetCount", (double) telemetry.cachedAssetCount());
        metrics.put("totalLoads", (double) telemetry.totalLoads());
        metrics.put("cacheHits", (double) telemetry.cacheHits());
        metrics.put("cacheMisses", (double) telemetry.cacheMisses());
        metrics.put("failedResolutions", (double) telemetry.failedResolutions());
        metrics.put("registeredLoaderCount", (double) telemetry.registeredLoaderCount());

        long total = telemetry.cacheHits() + telemetry.cacheMisses();
        if (total > 0) {
            metrics.put("cacheHitRate", (double) telemetry.cacheHits() / total);
        }

        // Load timing
        metrics.put("lastLoadMs", telemetry.lastLoadNanos() / 1_000_000.0);

        return metrics;
    }

    /**
     * Point-in-time content system state for telemetry extraction.
     *
     * <p>Since DynamisContent has no built-in counters, callers must
     * instrument AssetManager/AssetCache or maintain external counters.
     *
     * @param manifestEntryCount   entries in the asset manifest
     * @param cachedAssetCount     assets currently in cache
     * @param totalLoads           cumulative asset load attempts
     * @param cacheHits            cumulative cache hits
     * @param cacheMisses          cumulative cache misses (triggered loads)
     * @param failedResolutions    cumulative resolution failures
     * @param registeredLoaderCount number of registered asset loaders
     */
    public record ContentTelemetrySnapshot(
            int manifestEntryCount,
            int cachedAssetCount,
            long totalLoads,
            long cacheHits,
            long cacheMisses,
            long failedResolutions,
            int registeredLoaderCount,
            long lastLoadNanos,
            String lastLoadedAssetId
    ) {
        /** Backwards-compatible constructor. */
        public ContentTelemetrySnapshot(
                int manifestEntryCount, int cachedAssetCount,
                long totalLoads, long cacheHits, long cacheMisses,
                long failedResolutions, int registeredLoaderCount) {
            this(manifestEntryCount, cachedAssetCount, totalLoads, cacheHits, cacheMisses,
                    failedResolutions, registeredLoaderCount, 0, "");
        }
    }
}
