package org.dynamisengine.debug.api;

import java.util.Set;
import java.util.function.Predicate;

/**
 * Immutable filter for selecting debug data by category, severity, source,
 * frame range, and time range.
 *
 * <p>Build queries via the static {@link #builder()} method. All filter
 * criteria are combined with AND semantics — a snapshot or event must
 * satisfy every specified predicate to match.
 *
 * <p>Unset criteria are treated as "match all".
 */
public record DebugQuery(
        Set<DebugCategory> categories,
        Set<DebugSeverity> severities,
        Set<String> sources,
        long minFrame,
        long maxFrame,
        long minTimestampMs,
        long maxTimestampMs
) {

    public DebugQuery {
        categories = categories != null ? Set.copyOf(categories) : Set.of();
        severities = severities != null ? Set.copyOf(severities) : Set.of();
        sources = sources != null ? Set.copyOf(sources) : Set.of();
    }

    /** Returns true if this query matches the given snapshot. */
    public boolean matchesSnapshot(DebugSnapshot snapshot) {
        if (!categories.isEmpty() && !categories.contains(snapshot.category())) return false;
        if (!sources.isEmpty() && !sources.contains(snapshot.source())) return false;
        if (minFrame > 0 && snapshot.frameNumber() < minFrame) return false;
        if (maxFrame > 0 && snapshot.frameNumber() > maxFrame) return false;
        if (minTimestampMs > 0 && snapshot.timestampMs() < minTimestampMs) return false;
        if (maxTimestampMs > 0 && snapshot.timestampMs() > maxTimestampMs) return false;
        return true;
    }

    /** Returns true if this query matches the given event. */
    public boolean matchesEvent(org.dynamisengine.debug.api.event.DebugEvent event) {
        if (!categories.isEmpty() && !categories.contains(event.category())) return false;
        if (!severities.isEmpty() && !severities.contains(event.severity())) return false;
        if (!sources.isEmpty() && !sources.contains(event.source())) return false;
        if (minFrame > 0 && event.frameNumber() < minFrame) return false;
        if (maxFrame > 0 && event.frameNumber() > maxFrame) return false;
        if (minTimestampMs > 0 && event.timestampMs() < minTimestampMs) return false;
        if (maxTimestampMs > 0 && event.timestampMs() > maxTimestampMs) return false;
        return true;
    }

    /** A query that matches everything. */
    public static DebugQuery all() {
        return new DebugQuery(Set.of(), Set.of(), Set.of(), 0, 0, 0, 0);
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private Set<DebugCategory> categories = Set.of();
        private Set<DebugSeverity> severities = Set.of();
        private Set<String> sources = Set.of();
        private long minFrame;
        private long maxFrame;
        private long minTimestampMs;
        private long maxTimestampMs;

        private Builder() {}

        public Builder categories(DebugCategory... cats) {
            this.categories = Set.of(cats);
            return this;
        }

        public Builder severities(DebugSeverity... sevs) {
            this.severities = Set.of(sevs);
            return this;
        }

        public Builder sources(String... srcs) {
            this.sources = Set.of(srcs);
            return this;
        }

        public Builder frameRange(long min, long max) {
            this.minFrame = min;
            this.maxFrame = max;
            return this;
        }

        public Builder timeRange(long minMs, long maxMs) {
            this.minTimestampMs = minMs;
            this.maxTimestampMs = maxMs;
            return this;
        }

        public DebugQuery build() {
            return new DebugQuery(categories, severities, sources,
                    minFrame, maxFrame, minTimestampMs, maxTimestampMs);
        }
    }
}
