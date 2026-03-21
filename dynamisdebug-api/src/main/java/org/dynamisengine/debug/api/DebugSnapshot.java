package org.dynamisengine.debug.api;

import java.util.Map;

/**
 * Point-in-time diagnostic snapshot from a subsystem or aggregator.
 *
 * "What is true right now?"
 *
 * @param frameNumber  engine frame/tick number
 * @param timestampMs  wall-clock timestamp
 * @param source       originating subsystem or component name
 * @param category     domain category
 * @param metrics      named numeric values
 * @param flags        named boolean states
 * @param text         optional human-readable summary
 */
public record DebugSnapshot(
        long frameNumber,
        long timestampMs,
        String source,
        DebugCategory category,
        Map<String, Double> metrics,
        Map<String, Boolean> flags,
        String text
) {
    public DebugSnapshot {
        metrics = metrics != null ? Map.copyOf(metrics) : Map.of();
        flags = flags != null ? Map.copyOf(flags) : Map.of();
    }
}
