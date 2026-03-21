package org.dynamisengine.debug.api;

/**
 * Named counter for tracking occurrences or quantities.
 *
 * @param name     counter name (e.g. "drawCalls", "entitiesActive")
 * @param category domain category
 * @param value    current value
 */
public record DebugCounter(String name, DebugCategory category, long value) {

    public DebugCounter incremented() { return new DebugCounter(name, category, value + 1); }
    public DebugCounter decremented() { return new DebugCounter(name, category, value - 1); }
    public DebugCounter withValue(long v) { return new DebugCounter(name, category, v); }

    public static DebugCounter of(String name, DebugCategory category) {
        return new DebugCounter(name, category, 0);
    }
}
