package org.dynamisengine.debug.bridge.adapter;

import org.dynamisengine.debug.api.DebugCategory;
import org.dynamisengine.debug.api.DebugSnapshot;
import org.dynamisengine.debug.bridge.TelemetryAdapter;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Adapts UI system state into a unified {@link DebugSnapshot}.
 *
 * <p>DynamisUI is both a telemetry producer (widget tree state, layout passes,
 * overlay visibility) and a consumer (it surfaces debug data through DebugOverlay).
 * This adapter captures the producer side.
 *
 * <p>Read-only consumer — no debug concepts pushed back into DynamisUI.
 */
public final class UiTelemetryAdapter implements TelemetryAdapter<UiTelemetryAdapter.UiTelemetrySnapshot> {

    @Override
    public String subsystemName() { return "ui"; }

    @Override
    public DebugCategory category() { return DebugCategory.UI; }

    @Override
    public DebugSnapshot adapt(UiTelemetrySnapshot t, long frameNumber) {
        Map<String, Double> metrics = extractMetrics(t);

        Map<String, Boolean> flags = Map.of(
                "debugOverlayEnabled", t.debugOverlayEnabled(),
                "hasWidgets", t.widgetCount() > 0
        );

        String text = String.format(java.util.Locale.ROOT,
                "layers=%d widgets=%d fps=%.0f avgFrame=%.1fms overlay=%s panels=%d",
                t.layerCount(), t.widgetCount(),
                t.fps(), t.avgFrameMs(),
                t.debugOverlayEnabled() ? "on" : "off", t.debugPanelCount());

        return new DebugSnapshot(frameNumber, System.currentTimeMillis(),
                subsystemName(), category(), metrics, flags, text);
    }

    @Override
    public Map<String, Double> extractMetrics(UiTelemetrySnapshot t) {
        Map<String, Double> metrics = new LinkedHashMap<>();
        metrics.put("layerCount", (double) t.layerCount());
        metrics.put("widgetCount", (double) t.widgetCount());
        metrics.put("fps", (double) t.fps());
        metrics.put("avgFrameMs", (double) t.avgFrameMs());
        metrics.put("debugPanelCount", (double) t.debugPanelCount());
        metrics.put("visibleWidgetCount", (double) t.visibleWidgetCount());
        return metrics;
    }

    /**
     * Point-in-time UI state.
     *
     * @param layerCount          UIStage layer count
     * @param widgetCount         total widget count (recursive)
     * @param visibleWidgetCount  visible widgets
     * @param fps                 from PerformanceOverlay
     * @param avgFrameMs          from PerformanceOverlay (60-sample rolling)
     * @param debugOverlayEnabled whether debug overlay is active
     * @param debugPanelCount     panels registered this frame
     */
    public record UiTelemetrySnapshot(
            int layerCount, int widgetCount,
            int visibleWidgetCount,
            float fps, float avgFrameMs,
            boolean debugOverlayEnabled, int debugPanelCount
    ) {}
}
