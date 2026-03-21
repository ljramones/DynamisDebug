package org.dynamisengine.debug.core;

import org.dynamisengine.debug.api.DebugQuery;
import org.dynamisengine.debug.api.DebugQueryService;
import org.dynamisengine.debug.api.DebugSnapshot;
import org.dynamisengine.debug.api.event.DebugEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Default query service that executes {@link DebugQuery} filters against
 * a {@link DebugHistory} and {@link DebugEventBuffer}.
 */
public final class DefaultDebugQueryService implements DebugQueryService {

    private final DebugHistory history;
    private final DebugEventBuffer eventBuffer;

    public DefaultDebugQueryService(DebugHistory history, DebugEventBuffer eventBuffer) {
        this.history = history;
        this.eventBuffer = eventBuffer;
    }

    @Override
    public List<DebugSnapshot> querySnapshots(DebugQuery query) {
        List<DebugSnapshot> result = new ArrayList<>();
        for (var frame : history.recent(history.size())) {
            for (var snapshot : frame.snapshots().values()) {
                if (query.matchesSnapshot(snapshot)) {
                    result.add(snapshot);
                }
            }
        }
        return result;
    }

    @Override
    public List<DebugEvent> queryEvents(DebugQuery query) {
        List<DebugEvent> result = new ArrayList<>();
        for (var event : eventBuffer.recent(eventBuffer.size())) {
            if (query.matchesEvent(event)) {
                result.add(event);
            }
        }
        return result;
    }

    @Override
    public Map<String, DebugSnapshot> snapshotsForFrame(long frameNumber) {
        for (var frame : history.recent(history.size())) {
            if (frame.frameNumber() == frameNumber) {
                return frame.snapshots();
            }
        }
        return Map.of();
    }
}
