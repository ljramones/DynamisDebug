package org.dynamisengine.debug.api.event;

import org.dynamisengine.debug.api.DebugCategory;
import org.dynamisengine.debug.api.DebugSeverity;

/**
 * A diagnostic event: "what just happened?"
 *
 * Unlike snapshots (point-in-time state), events capture transient
 * occurrences: underruns, fallbacks, spikes, failures, transitions.
 *
 * @param frameNumber  frame when the event occurred
 * @param timestampMs  wall-clock timestamp
 * @param source       originating subsystem
 * @param category     domain category
 * @param severity     importance level
 * @param name         short event name (e.g. "audioUnderrun", "shaderCompileFail")
 * @param message      human-readable detail
 */
public record DebugEvent(
        long frameNumber,
        long timestampMs,
        String source,
        DebugCategory category,
        DebugSeverity severity,
        String name,
        String message
) {}
