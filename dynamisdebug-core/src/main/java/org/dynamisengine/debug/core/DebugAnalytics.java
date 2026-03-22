package org.dynamisengine.debug.core;

import org.dynamisengine.debug.api.DebugSeverity;
import org.dynamisengine.debug.api.DebugSnapshot;
import org.dynamisengine.debug.api.event.DebugEvent;

import java.util.*;

/**
 * Higher-level analytical queries over {@link DebugSession} history and events.
 *
 * <p>Provides diagnostic answers such as:
 * <ul>
 *   <li>When did a metric last exceed a threshold?</li>
 *   <li>Which watchdog rules are noisiest?</li>
 *   <li>How many threshold crossings occurred?</li>
 *   <li>When did two conditions co-occur?</li>
 * </ul>
 *
 * <p>All queries are deterministic, history-based, and bounded by window size.
 */
public final class DebugAnalytics {

    private final DebugSession session;

    public DebugAnalytics(DebugSession session) {
        this.session = Objects.requireNonNull(session);
    }

    // --- Spike Analysis ---

    /**
     * Find frames where a metric exceeded a threshold within the recent window.
     */
    public SpikeResult findSpikes(String source, String metricName, double threshold, int maxFrames) {
        var frames = session.history().recent(maxFrames);
        long lastSpikeFrame = -1;
        int spikeCount = 0;
        double maxValue = Double.NEGATIVE_INFINITY;

        for (var frame : frames) {
            DebugSnapshot snap = frame.snapshots().get(source);
            if (snap == null) continue;
            Double value = snap.metrics().get(metricName);
            if (value == null) continue;

            if (value > maxValue) maxValue = value;
            if (value > threshold) {
                spikeCount++;
                lastSpikeFrame = frame.frameNumber();
            }
        }

        return new SpikeResult(lastSpikeFrame, spikeCount, maxValue == Double.NEGATIVE_INFINITY ? 0 : maxValue);
    }

    public record SpikeResult(long lastSpikeFrame, int spikeCount, double maxValue) {}

    // --- Noisy Rules ---

    /**
     * Rank watchdog/event sources by fire count within the recent event window.
     */
    public List<RuleNoise> rankNoisyRules(int maxEvents) {
        List<DebugEvent> events = session.recentEvents(maxEvents);
        Map<String, RuleNoiseAccum> counts = new LinkedHashMap<>();

        for (var event : events) {
            if (event.severity() == DebugSeverity.WARNING
                    || event.severity() == DebugSeverity.ERROR
                    || event.severity() == DebugSeverity.CRITICAL) {
                counts.computeIfAbsent(event.name(), k -> new RuleNoiseAccum(event.name(), event.severity().name()))
                      .count++;
            }
        }

        var result = new ArrayList<>(counts.values().stream()
            .map(a -> new RuleNoise(a.ruleName, a.severity, a.count))
            .toList());
        result.sort((a, b) -> Integer.compare(b.fireCount(), a.fireCount()));
        return result;
    }

    public record RuleNoise(String ruleName, String severity, int fireCount) {}

    private static class RuleNoiseAccum {
        final String ruleName;
        final String severity;
        int count;
        RuleNoiseAccum(String ruleName, String severity) {
            this.ruleName = ruleName;
            this.severity = severity;
        }
    }

    // --- Threshold Crossing Analysis ---

    /**
     * Analyze threshold crossings for a metric within the recent history window.
     */
    public ThresholdResult analyzeThresholdCrossings(String source, String metricName,
                                                      double threshold, int maxFrames) {
        var frames = session.history().recent(maxFrames);
        long firstCrossing = -1;
        long lastCrossing = -1;
        int crossings = 0;
        int framesAbove = 0;
        boolean wasAbove = false;

        for (var frame : frames) {
            DebugSnapshot snap = frame.snapshots().get(source);
            if (snap == null) continue;
            Double value = snap.metrics().get(metricName);
            if (value == null) continue;

            boolean isAbove = value > threshold;
            if (isAbove) framesAbove++;

            // Count transitions from below to above
            if (isAbove && !wasAbove) {
                crossings++;
                if (firstCrossing < 0) firstCrossing = frame.frameNumber();
                lastCrossing = frame.frameNumber();
            }
            wasAbove = isAbove;
        }

        return new ThresholdResult(firstCrossing, lastCrossing, crossings, framesAbove);
    }

    public record ThresholdResult(long firstCrossing, long lastCrossing, int crossings, int framesAbove) {}

    // --- Correlated Window Query ---

    /**
     * Find frames where two conditions are true simultaneously.
     */
    public CorrelationResult findCorrelatedFrames(
            String sourceA, String metricA, double thresholdA,
            String sourceB, String metricB, double thresholdB,
            int maxFrames) {

        var frames = session.history().recent(maxFrames);
        int matches = 0;
        long firstMatch = -1;
        long lastMatch = -1;

        for (var frame : frames) {
            DebugSnapshot snapA = frame.snapshots().get(sourceA);
            DebugSnapshot snapB = frame.snapshots().get(sourceB);
            if (snapA == null || snapB == null) continue;

            Double valA = snapA.metrics().get(metricA);
            Double valB = snapB.metrics().get(metricB);
            if (valA == null || valB == null) continue;

            if (valA > thresholdA && valB > thresholdB) {
                matches++;
                if (firstMatch < 0) firstMatch = frame.frameNumber();
                lastMatch = frame.frameNumber();
            }
        }

        return new CorrelationResult(matches, firstMatch, lastMatch);
    }

    public record CorrelationResult(int matches, long firstMatch, long lastMatch) {}
}
