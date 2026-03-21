package org.dynamisengine.debug.core;

import org.dynamisengine.debug.api.DebugSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Time-series view over {@link DebugHistory}.
 *
 * <p>Extracts named metrics across frames for trending, graphing, and
 * threshold detection. Works on top of the existing history ring buffer
 * without duplicating storage.
 */
public final class DebugTimeline {

    private final DebugHistory history;

    public DebugTimeline(DebugHistory history) {
        this.history = history;
    }

    /**
     * Extract a named metric from a specific source across recent frames.
     *
     * @param source     the snapshot source name (e.g. "physics")
     * @param metricName the metric key within that source's snapshot
     * @param maxFrames  maximum number of frames to look back
     * @return time-series data points in chronological order
     */
    public List<DataPoint> extractMetric(String source, String metricName, int maxFrames) {
        List<DataPoint> points = new ArrayList<>();
        for (var frame : history.recent(maxFrames)) {
            DebugSnapshot snap = frame.snapshots().get(source);
            if (snap != null) {
                Double value = snap.metrics().get(metricName);
                if (value != null) {
                    points.add(new DataPoint(frame.frameNumber(), snap.timestampMs(), value));
                }
            }
        }
        return points;
    }

    /**
     * Extract a metric from any source that contains it, across recent frames.
     * If multiple sources have the same metric name, all contribute points
     * (tagged by source in the returned records).
     *
     * @param metricName the metric key to search for
     * @param maxFrames  maximum number of frames to look back
     * @return tagged data points in chronological order
     */
    public List<TaggedDataPoint> extractMetricAcrossSources(String metricName, int maxFrames) {
        List<TaggedDataPoint> points = new ArrayList<>();
        for (var frame : history.recent(maxFrames)) {
            for (var entry : frame.snapshots().entrySet()) {
                Double value = entry.getValue().metrics().get(metricName);
                if (value != null) {
                    points.add(new TaggedDataPoint(
                            frame.frameNumber(), entry.getValue().timestampMs(),
                            entry.getKey(), value));
                }
            }
        }
        return points;
    }

    /**
     * Compute min/max/average of a metric over recent frames.
     *
     * @param source     the snapshot source name
     * @param metricName the metric key
     * @param maxFrames  maximum number of frames to look back
     * @return statistics, or empty stats if no data points found
     */
    public MetricStats stats(String source, String metricName, int maxFrames) {
        var points = extractMetric(source, metricName, maxFrames);
        if (points.isEmpty()) {
            return new MetricStats(0, 0, 0, 0);
        }
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        double sum = 0;
        for (var p : points) {
            if (p.value() < min) min = p.value();
            if (p.value() > max) max = p.value();
            sum += p.value();
        }
        return new MetricStats(min, max, sum / points.size(), points.size());
    }

    /** A single metric value at a point in time. */
    public record DataPoint(long frameNumber, long timestampMs, double value) {}

    /** A metric value tagged with its source. */
    public record TaggedDataPoint(long frameNumber, long timestampMs, String source, double value) {}

    /** Summary statistics for a metric over a time range. */
    public record MetricStats(double min, double max, double average, int sampleCount) {}
}
