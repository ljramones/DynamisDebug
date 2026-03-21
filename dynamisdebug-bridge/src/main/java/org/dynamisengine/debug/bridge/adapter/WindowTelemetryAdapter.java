package org.dynamisengine.debug.bridge.adapter;

import org.dynamisengine.debug.api.DebugCategory;
import org.dynamisengine.debug.api.DebugSnapshot;
import org.dynamisengine.debug.bridge.TelemetryAdapter;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Adapts window system state into a unified {@link DebugSnapshot}.
 *
 * <p>Since DynamisWindow has no built-in telemetry record, callers provide
 * a plain-value snapshot DTO constructed from Window queries and event counting.
 *
 * <p>Read-only consumer — no debug concepts pushed back into DynamisWindow.
 */
public final class WindowTelemetryAdapter implements TelemetryAdapter<WindowTelemetryAdapter.WindowTelemetrySnapshot> {

    @Override
    public String subsystemName() { return "window"; }

    @Override
    public DebugCategory category() { return DebugCategory.ENGINE; }

    @Override
    public DebugSnapshot adapt(WindowTelemetrySnapshot t, long frameNumber) {
        Map<String, Double> metrics = extractMetrics(t);

        Map<String, Boolean> flags = Map.of(
                "focused", t.focused(),
                "minimized", t.windowWidth() == 0 || t.windowHeight() == 0
        );

        String text = String.format(java.util.Locale.ROOT,
                "window=%dx%d fb=%dx%d focused=%s resizes=%d backend=%s",
                t.windowWidth(), t.windowHeight(),
                t.framebufferWidth(), t.framebufferHeight(),
                t.focused(), t.resizeCount(), t.backendHint());

        return new DebugSnapshot(frameNumber, System.currentTimeMillis(),
                subsystemName(), category(), metrics, flags, text);
    }

    @Override
    public Map<String, Double> extractMetrics(WindowTelemetrySnapshot t) {
        Map<String, Double> metrics = new LinkedHashMap<>();
        metrics.put("windowWidth", (double) t.windowWidth());
        metrics.put("windowHeight", (double) t.windowHeight());
        metrics.put("framebufferWidth", (double) t.framebufferWidth());
        metrics.put("framebufferHeight", (double) t.framebufferHeight());
        metrics.put("resizeCount", (double) t.resizeCount());
        metrics.put("focusChangeCount", (double) t.focusChangeCount());
        if (t.windowWidth() > 0 && t.framebufferWidth() > 0) {
            metrics.put("dpiScale", (double) t.framebufferWidth() / t.windowWidth());
        }
        return metrics;
    }

    /**
     * Point-in-time window state.
     *
     * @param windowWidth       logical window width
     * @param windowHeight      logical window height
     * @param framebufferWidth  framebuffer width (may differ on HiDPI)
     * @param framebufferHeight framebuffer height
     * @param focused           whether the window has focus
     * @param resizeCount       cumulative resize events
     * @param focusChangeCount  cumulative focus change events
     * @param backendHint       the backend hint used at creation
     */
    public record WindowTelemetrySnapshot(
            int windowWidth, int windowHeight,
            int framebufferWidth, int framebufferHeight,
            boolean focused, int resizeCount,
            int focusChangeCount, String backendHint
    ) {}
}
