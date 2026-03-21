package org.dynamisengine.debug.bridge.adapter;

import org.dynamisengine.debug.api.DebugCategory;
import org.dynamisengine.debug.api.DebugSnapshot;
import org.dynamisengine.debug.bridge.TelemetryAdapter;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Adapts localization system state into a unified {@link DebugSnapshot}.
 *
 * <p>Captures active locale, missing key counts, fallback hits, and
 * namespace availability. Light integration — localization is not a
 * high-frequency telemetry source.
 *
 * <p>Read-only consumer — no debug concepts pushed back into DynamisLocalization.
 */
public final class LocalizationTelemetryAdapter implements TelemetryAdapter<LocalizationTelemetryAdapter.LocalizationTelemetrySnapshot> {

    @Override
    public String subsystemName() { return "localization"; }

    @Override
    public DebugCategory category() { return DebugCategory.UI; }

    @Override
    public DebugSnapshot adapt(LocalizationTelemetrySnapshot t, long frameNumber) {
        Map<String, Double> metrics = extractMetrics(t);

        Map<String, Boolean> flags = Map.of(
                "hasMissingKeys", t.missingKeyCount() > 0,
                "hasFallbacks", t.fallbackHitCount() > 0
        );

        String text = String.format(java.util.Locale.ROOT,
                "locale=%s namespaces=%d missing=%d fallbacks=%d switches=%d",
                t.activeLocale(), t.namespaceCount(),
                t.missingKeyCount(), t.fallbackHitCount(), t.localeSwitchCount());

        return new DebugSnapshot(frameNumber, System.currentTimeMillis(),
                subsystemName(), category(), metrics, flags, text);
    }

    @Override
    public Map<String, Double> extractMetrics(LocalizationTelemetrySnapshot t) {
        Map<String, Double> metrics = new LinkedHashMap<>();
        metrics.put("namespaceCount", (double) t.namespaceCount());
        metrics.put("missingKeyCount", (double) t.missingKeyCount());
        metrics.put("fallbackHitCount", (double) t.fallbackHitCount());
        metrics.put("localeSwitchCount", (double) t.localeSwitchCount());
        metrics.put("bundleLoadFailures", (double) t.bundleLoadFailures());
        return metrics;
    }

    /**
     * Point-in-time localization state.
     *
     * <p>Since DynamisLocalization has no built-in counters, callers must
     * instrument or wrap the service to collect missing key and fallback counts.
     *
     * @param activeLocale      BCP47 tag of current locale
     * @param namespaceCount    loaded namespace count
     * @param missingKeyCount   cumulative missing key lookups
     * @param fallbackHitCount  cumulative fallback language hits
     * @param bundleLoadFailures cumulative bundle load failures
     * @param localeSwitchCount cumulative locale switch events
     */
    public record LocalizationTelemetrySnapshot(
            String activeLocale, int namespaceCount,
            long missingKeyCount, long fallbackHitCount,
            long bundleLoadFailures, int localeSwitchCount
    ) {}
}
