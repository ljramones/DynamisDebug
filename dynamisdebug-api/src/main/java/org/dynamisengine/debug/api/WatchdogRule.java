package org.dynamisengine.debug.api;

/**
 * A threshold rule evaluated against debug snapshots each frame.
 *
 * <p>When a rule triggers, the watchdog emits a {@link org.dynamisengine.debug.api.event.DebugEvent}
 * with the rule's severity and message. Rules are stateless predicates —
 * the watchdog tracks cooldown and repeat suppression.
 *
 * @param name           unique rule name (e.g. "gpu.backlogHigh")
 * @param source         the snapshot source to watch (e.g. "gpu", "ecs")
 * @param metricName     the metric key to evaluate, or null for flag-based rules
 * @param threshold      the threshold value (metric > threshold triggers)
 * @param comparison     how to compare the metric against the threshold
 * @param severity       event severity when triggered
 * @param message        human-readable message when triggered
 * @param cooldownFrames minimum frames between consecutive firings (prevents spam)
 */
public record WatchdogRule(
        String name,
        String source,
        String metricName,
        double threshold,
        Comparison comparison,
        DebugSeverity severity,
        String message,
        int cooldownFrames
) {
    public enum Comparison {
        GREATER_THAN,
        LESS_THAN,
        EQUALS,
        NOT_EQUALS
    }

    public boolean evaluate(double value) {
        return switch (comparison) {
            case GREATER_THAN -> value > threshold;
            case LESS_THAN -> value < threshold;
            case EQUALS -> Double.compare(value, threshold) == 0;
            case NOT_EQUALS -> Double.compare(value, threshold) != 0;
        };
    }

    /** Convenience: fire when metric exceeds threshold. */
    public static WatchdogRule above(String name, String source, String metric,
                                      double threshold, DebugSeverity severity, String message) {
        return new WatchdogRule(name, source, metric, threshold,
                Comparison.GREATER_THAN, severity, message, 60);
    }

    /** Convenience: fire when metric drops below threshold. */
    public static WatchdogRule below(String name, String source, String metric,
                                      double threshold, DebugSeverity severity, String message) {
        return new WatchdogRule(name, source, metric, threshold,
                Comparison.LESS_THAN, severity, message, 60);
    }
}
