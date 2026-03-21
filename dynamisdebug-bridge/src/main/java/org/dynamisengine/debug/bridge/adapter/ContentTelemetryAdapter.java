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
 * <p>DynamisContent currently has no built-in telemetry hooks, so callers
 * must instrument or wrap AssetManager/AssetCache to collect these counts.
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

        Map<String, Boolean> flags = Map.of(
                "hasFailures", telemetry.failedResolutions() > 0
        );

        String text = "manifest=" + telemetry.manifestEntryCount()
                + " cached=" + telemetry.cachedAssetCount()
                + " loads=" + telemetry.totalLoads()
                + " hits=" + telemetry.cacheHits()
                + " misses=" + telemetry.cacheMisses();

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
            int registeredLoaderCount
    ) {}
}
