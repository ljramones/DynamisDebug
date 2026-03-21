package org.dynamisengine.debug.bridge.overlay;

import org.dynamisengine.debug.api.DebugCategory;
import org.dynamisengine.debug.api.DebugQuery;
import org.dynamisengine.debug.api.DebugSnapshot;
import org.dynamisengine.debug.api.DebugSeverity;
import org.dynamisengine.debug.api.event.DebugEvent;
import org.dynamisengine.debug.core.DebugSession;
import org.dynamisengine.debug.core.DebugTimeline;
import org.dynamisengine.ui.debug.DebugOverlay;
import org.dynamisengine.ui.debug.DebugPanel;

import java.util.*;

/**
 * Builds {@link DebugPanel}s from {@link DebugSession} state for display
 * in a {@link DebugOverlay}.
 *
 * <p>This is the bridge between DynamisDebug (truth) and DynamisUI (face).
 * Call {@link #buildAll} each frame to get panels grouped by category,
 * plus engine summary and active alerts.
 *
 * <p>Usage per frame:
 * <pre>
 *   overlay.beginFrame();
 *   for (DebugPanel panel : overlayBuilder.buildAll(snapshots)) {
 *       overlay.addPanel(panel);
 *   }
 *   overlay.render(renderer);
 * </pre>
 */
public final class DebugOverlayBuilder {

    private final DebugSession session;

    public DebugOverlayBuilder(DebugSession session) {
        this.session = session;
    }

    /**
     * Build all debug panels for the current frame.
     *
     * @param snapshots the current frame's merged snapshot map
     * @return ordered list of panels: engine summary, category panels, alerts
     */
    public List<DebugPanel> buildAll(Map<String, DebugSnapshot> snapshots) {
        List<DebugPanel> panels = new ArrayList<>();

        // 1. Engine summary (always first)
        panels.add(buildEngineSummary(snapshots));

        // 2. Category panels (grouped)
        Map<DebugCategory, List<DebugSnapshot>> byCategory = groupByCategory(snapshots);
        for (var entry : byCategory.entrySet()) {
            panels.add(buildCategoryPanel(entry.getKey(), entry.getValue()));
        }

        // 3. Active alerts (always last)
        DebugPanel alerts = buildAlertPanel();
        if (alerts.rows().size() > 0) {
            panels.add(alerts);
        }

        return panels;
    }

    /**
     * Build a single engine summary panel showing top-level metrics.
     */
    public DebugPanel buildEngineSummary(Map<String, DebugSnapshot> snapshots) {
        var builder = DebugPanel.builder("Engine Summary");

        // WorldEngine metrics if present
        DebugSnapshot we = snapshots.get("worldengine");
        if (we != null) {
            addMetricRow(builder, we, "tick", "Tick", "%.0f", "");
            addMetricRow(builder, we, "budgetPercent", "Budget", "%.0f", "%");
            addMetricRow(builder, we, "lastTickDurationMs", "Tick Time", "%.1f", "ms");
            addMetricRow(builder, we, "healthySubsystemCount", "Healthy", "%.0f", "");
            addMetricRow(builder, we, "degradedSubsystemCount", "Degraded", "%.0f", "");
            addMetricRow(builder, we, "faultedSubsystemCount", "Faulted", "%.0f", "");
        }

        builder.row("Sources", String.valueOf(snapshots.size()));
        builder.row("History", String.valueOf(session.history().size()));

        // Flag summary
        long flagsTrue = snapshots.values().stream()
                .flatMap(s -> s.flags().values().stream())
                .filter(b -> b).count();
        if (flagsTrue > 0) {
            builder.row("Active Flags", String.valueOf(flagsTrue));
        }

        return builder.build();
    }

    /**
     * Build a panel for a single category, showing all sources in that category.
     */
    public DebugPanel buildCategoryPanel(DebugCategory category, List<DebugSnapshot> snapshots) {
        var builder = DebugPanel.builder(category.name());

        for (DebugSnapshot snap : snapshots) {
            // Source header with text summary
            if (snap.text() != null && !snap.text().isEmpty()) {
                builder.row(snap.source(), snap.text());
            } else {
                // Fallback: show top 3 metrics
                int count = 0;
                for (var entry : snap.metrics().entrySet()) {
                    if (count++ >= 3) break;
                    builder.row(snap.source() + "." + entry.getKey(),
                            formatMetric(entry.getValue()));
                }
            }

            // Show active flags
            for (var flag : snap.flags().entrySet()) {
                if (flag.getValue()) {
                    builder.row("  " + flag.getKey(), "ACTIVE");
                }
            }
        }

        return builder.build();
    }

    /**
     * Build an alert panel showing recent watchdog events.
     */
    public DebugPanel buildAlertPanel() {
        var builder = DebugPanel.builder("Alerts");

        List<DebugEvent> recent = session.recentEvents(10);
        for (DebugEvent event : recent) {
            String severity = event.severity().name().substring(0, 1);
            builder.row("[" + severity + "] " + event.name(),
                    truncate(event.message(), 60));
        }

        return builder.build();
    }

    /**
     * Build a panel for a specific source with full metric detail.
     */
    public DebugPanel buildSourceDetailPanel(DebugSnapshot snapshot) {
        var builder = DebugPanel.builder(snapshot.source() + " Detail");

        for (var entry : snapshot.metrics().entrySet()) {
            builder.row(entry.getKey(), formatMetric(entry.getValue()));
        }
        for (var flag : snapshot.flags().entrySet()) {
            builder.row(flag.getKey(), flag.getValue() ? "YES" : "no");
        }
        if (snapshot.text() != null) {
            builder.row("Summary", snapshot.text());
        }

        return builder.build();
    }

    /**
     * Build a timeline sparkline panel for a metric.
     */
    public DebugPanel buildTimelinePanel(String source, String metric, int frames) {
        var builder = DebugPanel.builder(source + "." + metric);

        var stats = session.timeline().stats(source, metric, frames);
        if (stats.sampleCount() > 0) {
            builder.row("Current", formatMetric(
                    session.timeline().extractMetric(source, metric, 1)
                            .stream().findFirst().map(DebugTimeline.DataPoint::value).orElse(0.0)));
            builder.row("Avg", formatMetric(stats.average()));
            builder.row("Min", formatMetric(stats.min()));
            builder.row("Max", formatMetric(stats.max()));
            builder.row("Samples", String.valueOf(stats.sampleCount()));
        } else {
            builder.row("Status", "No data");
        }

        return builder.build();
    }

    // --- Helpers ---

    private Map<DebugCategory, List<DebugSnapshot>> groupByCategory(Map<String, DebugSnapshot> snapshots) {
        Map<DebugCategory, List<DebugSnapshot>> grouped = new LinkedHashMap<>();
        for (DebugSnapshot snap : snapshots.values()) {
            grouped.computeIfAbsent(snap.category(), k -> new ArrayList<>()).add(snap);
        }
        return grouped;
    }

    private void addMetricRow(DebugPanel.Builder builder, DebugSnapshot snap,
                               String metric, String label, String format, String unit) {
        Double value = snap.metrics().get(metric);
        if (value != null) {
            builder.row(label, String.format(java.util.Locale.ROOT, format, value) + unit);
        }
    }

    private static String formatMetric(double value) {
        if (value == (long) value) {
            return String.format(java.util.Locale.ROOT, "%.0f", value);
        }
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 3) + "...";
    }
}
