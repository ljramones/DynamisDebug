package org.dynamisengine.debug.core;

import org.dynamisengine.debug.api.*;
import org.dynamisengine.debug.api.event.DebugEvent;
import org.dynamisengine.debug.api.event.DebugEventSink;

import org.dynamisengine.debug.api.DebugQueryService;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central debug session. Coordinates snapshot providers, events, counters,
 * flags, and commands for one engine runtime.
 *
 * Thread-safe: subsystems may submit events from any thread.
 */
public final class DebugSession implements DebugEventSink {

    private final Map<String, DebugSnapshotProvider> providers = new ConcurrentHashMap<>();
    private final Map<String, DebugCounter> counters = new ConcurrentHashMap<>();
    private final Map<String, DebugFlag> flags = new ConcurrentHashMap<>();
    private final DefaultCommandRegistry commandRegistry = new DefaultCommandRegistry();
    private final DebugEventBuffer eventBuffer;
    private final DebugHistory history;
    private final DefaultDebugQueryService queryService;
    private final DebugTimeline timeline;
    private volatile boolean enabled = true;

    public DebugSession() {
        this(256, 300); // 256 events, 300 frames history
    }

    public DebugSession(int maxEvents, int maxHistoryFrames) {
        this.eventBuffer = new DebugEventBuffer(maxEvents);
        this.history = new DebugHistory(maxHistoryFrames);
        this.queryService = new DefaultDebugQueryService(history, eventBuffer);
        this.timeline = new DebugTimeline(history);
    }

    // --- Providers ---

    public void registerProvider(DebugSnapshotProvider provider) {
        providers.put(provider.debugSourceName(), provider);
    }

    public void unregisterProvider(String name) {
        providers.remove(name);
    }

    // --- Frame capture ---

    /** Capture snapshots from all providers and store in history. */
    public Map<String, DebugSnapshot> captureFrame(long frameNumber) {
        if (!enabled) return Map.of();
        Map<String, DebugSnapshot> frame = new LinkedHashMap<>();
        for (var entry : providers.entrySet()) {
            try {
                frame.put(entry.getKey(), entry.getValue().captureSnapshot(frameNumber));
            } catch (Exception e) {
                // Don't let debug failure crash the engine
            }
        }
        history.record(frameNumber, frame);
        return frame;
    }

    // --- Events ---

    @Override
    public void submit(DebugEvent event) {
        if (enabled) eventBuffer.submit(event);
    }

    public List<DebugEvent> drainEvents() {
        return eventBuffer.drain();
    }

    public List<DebugEvent> recentEvents(int max) {
        return eventBuffer.recent(max);
    }

    // --- Counters ---

    public void setCounter(String name, DebugCategory category, long value) {
        counters.put(name, new DebugCounter(name, category, value));
    }

    public void incrementCounter(String name) {
        counters.computeIfPresent(name, (k, c) -> c.incremented());
    }

    public Optional<DebugCounter> counter(String name) {
        return Optional.ofNullable(counters.get(name));
    }

    public Collection<DebugCounter> allCounters() {
        return Collections.unmodifiableCollection(counters.values());
    }

    // --- Flags ---

    public void setFlag(String name, boolean value) {
        flags.put(name, new DebugFlag(name, value));
    }

    public void toggleFlag(String name) {
        flags.computeIfPresent(name, (k, f) -> f.toggled());
    }

    public boolean flag(String name) {
        DebugFlag f = flags.get(name);
        return f != null && f.enabled();
    }

    public Collection<DebugFlag> allFlags() {
        return Collections.unmodifiableCollection(flags.values());
    }

    // --- Commands ---

    public DebugCommandRegistry commands() { return commandRegistry; }

    // --- History & queries ---

    public DebugHistory history() { return history; }

    public DebugQueryService queries() { return queryService; }

    public DebugTimeline timeline() { return timeline; }

    // --- Enable/disable ---

    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isEnabled() { return enabled; }
}
