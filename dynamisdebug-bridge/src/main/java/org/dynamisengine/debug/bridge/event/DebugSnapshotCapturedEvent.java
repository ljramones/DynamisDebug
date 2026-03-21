package org.dynamisengine.debug.bridge.event;

import org.dynamisengine.core.event.EngineEvent;
import org.dynamisengine.debug.api.DebugSnapshot;

import java.util.Map;

/**
 * Published when a debug frame capture completes.
 *
 * <p>Carries the frame number and all captured snapshots. Subsystems
 * or UI layers can subscribe to this event to react to diagnostic
 * state changes without polling the debug session.
 *
 * @param frameNumber the captured frame number
 * @param snapshots   source-keyed snapshots from this frame
 * @param timestamp   event creation time in nanos
 */
public record DebugSnapshotCapturedEvent(
        long frameNumber,
        Map<String, DebugSnapshot> snapshots,
        long timestamp
) implements EngineEvent {

    public DebugSnapshotCapturedEvent {
        snapshots = Map.copyOf(snapshots);
    }

    public static DebugSnapshotCapturedEvent of(long frameNumber, Map<String, DebugSnapshot> snapshots) {
        return new DebugSnapshotCapturedEvent(frameNumber, snapshots, System.nanoTime());
    }
}
