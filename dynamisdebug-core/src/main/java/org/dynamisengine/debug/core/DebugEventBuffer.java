package org.dynamisengine.debug.core;

import org.dynamisengine.debug.api.event.DebugEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Thread-safe ring buffer of debug events.
 */
public final class DebugEventBuffer {

    private final int maxEvents;
    private final ConcurrentLinkedDeque<DebugEvent> events = new ConcurrentLinkedDeque<>();

    public DebugEventBuffer(int maxEvents) {
        this.maxEvents = maxEvents;
    }

    public void submit(DebugEvent event) {
        events.addLast(event);
        while (events.size() > maxEvents) events.removeFirst();
    }

    /** Drain all buffered events (returns snapshot, clears buffer). */
    public List<DebugEvent> drain() {
        List<DebugEvent> result = new ArrayList<>(events.size());
        DebugEvent e;
        while ((e = events.pollFirst()) != null) result.add(e);
        return result;
    }

    /** View the most recent events without draining. */
    public List<DebugEvent> recent(int max) {
        List<DebugEvent> all = new ArrayList<>(events);
        int start = Math.max(0, all.size() - max);
        return Collections.unmodifiableList(all.subList(start, all.size()));
    }

    public int size() { return events.size(); }
}
