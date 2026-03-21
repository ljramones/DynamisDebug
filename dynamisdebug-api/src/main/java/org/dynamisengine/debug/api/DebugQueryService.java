package org.dynamisengine.debug.api;

import org.dynamisengine.debug.api.event.DebugEvent;

import java.util.List;
import java.util.Map;

/**
 * Contract for querying debug history and events.
 *
 * <p>Implementations execute {@link DebugQuery} filters against the
 * accumulated debug state (snapshots and events).
 */
public interface DebugQueryService {

    /** Query snapshots from history that match the given filter. */
    List<DebugSnapshot> querySnapshots(DebugQuery query);

    /** Query events that match the given filter. */
    List<DebugEvent> queryEvents(DebugQuery query);

    /** Query snapshots from a single frame. */
    Map<String, DebugSnapshot> snapshotsForFrame(long frameNumber);
}
