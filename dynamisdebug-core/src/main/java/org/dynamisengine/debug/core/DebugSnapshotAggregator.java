package org.dynamisengine.debug.core;

import org.dynamisengine.debug.api.DebugCategory;
import org.dynamisengine.debug.api.DebugSnapshot;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Merges multiple {@link DebugSnapshot}s into a single aggregate snapshot.
 *
 * <p>Use this to combine heterogeneous subsystem snapshots into a unified
 * view — e.g. merge all physics snapshots, or produce a frame-level summary
 * across all subsystems.
 *
 * <p>Metrics are merged by qualified key ({@code source.metricName}).
 * Flags are merged similarly. On key collision, the later snapshot wins.
 */
public final class DebugSnapshotAggregator {

    private DebugSnapshotAggregator() {}

    /**
     * Merge all snapshots from a frame into a single aggregate snapshot.
     *
     * @param frameNumber the frame number for the aggregate
     * @param timestampMs timestamp for the aggregate
     * @param snapshots   source-keyed snapshots to merge
     * @param category    category for the aggregate
     * @return a single snapshot with all metrics and flags, keyed by source prefix
     */
    public static DebugSnapshot aggregate(
            long frameNumber,
            long timestampMs,
            Map<String, DebugSnapshot> snapshots,
            DebugCategory category
    ) {
        Map<String, Double> mergedMetrics = new LinkedHashMap<>();
        Map<String, Boolean> mergedFlags = new LinkedHashMap<>();
        StringBuilder textBuilder = new StringBuilder();

        for (var entry : snapshots.entrySet()) {
            String source = entry.getKey();
            DebugSnapshot snap = entry.getValue();

            for (var m : snap.metrics().entrySet()) {
                mergedMetrics.put(source + "." + m.getKey(), m.getValue());
            }
            for (var f : snap.flags().entrySet()) {
                mergedFlags.put(source + "." + f.getKey(), f.getValue());
            }
            if (snap.text() != null && !snap.text().isEmpty()) {
                if (!textBuilder.isEmpty()) textBuilder.append("; ");
                textBuilder.append(source).append(": ").append(snap.text());
            }
        }

        return new DebugSnapshot(
                frameNumber, timestampMs, "aggregate", category,
                mergedMetrics, mergedFlags,
                textBuilder.isEmpty() ? null : textBuilder.toString()
        );
    }

    /**
     * Filter and merge only snapshots matching a given category.
     */
    public static DebugSnapshot aggregateByCategory(
            long frameNumber,
            long timestampMs,
            Map<String, DebugSnapshot> snapshots,
            DebugCategory category
    ) {
        Map<String, DebugSnapshot> filtered = new LinkedHashMap<>();
        for (var entry : snapshots.entrySet()) {
            if (entry.getValue().category() == category) {
                filtered.put(entry.getKey(), entry.getValue());
            }
        }
        return aggregate(frameNumber, timestampMs, filtered, category);
    }
}
