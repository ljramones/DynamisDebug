package org.dynamisengine.debug.bridge;

import org.dynamisengine.debug.api.DebugCommandResult;
import org.dynamisengine.debug.api.event.DebugEvent;
import org.dynamisengine.debug.api.event.DebugEventSink;
import org.dynamisengine.debug.api.DebugSnapshot;
import org.dynamisengine.debug.bridge.event.DebugCommandExecutedEvent;
import org.dynamisengine.debug.bridge.event.DebugEventForwardedEvent;
import org.dynamisengine.debug.bridge.event.DebugFlagToggledEvent;
import org.dynamisengine.debug.bridge.event.DebugSnapshotCapturedEvent;
import org.dynamisengine.debug.core.DebugSession;
import org.dynamisengine.event.EventBus;

import java.util.Map;

/**
 * Bridges {@link DebugSession} operations onto the engine {@link EventBus}.
 *
 * <p>Wraps a session and publishes engine events for key debug lifecycle
 * moments: frame captures, command executions, flag toggles, and forwarded
 * debug events.
 *
 * <p>This adapter is the single integration point between DynamisDebug's
 * internal model and the engine-wide event dispatch system. Subsystems
 * and UI layers subscribe to these events instead of polling the session.
 *
 * <p>The adapter also implements {@link DebugEventSink} so it can be
 * registered as a listener on the session to automatically forward
 * debug events onto the bus.
 */
public final class DebugEventBusAdapter implements DebugEventSink {

    private final DebugSession session;
    private final EventBus eventBus;
    private volatile boolean forwardEvents = true;

    public DebugEventBusAdapter(DebugSession session, EventBus eventBus) {
        this.session = session;
        this.eventBus = eventBus;
    }

    /**
     * Capture a frame and publish a {@link DebugSnapshotCapturedEvent}.
     *
     * @param frameNumber the frame to capture
     * @return the captured snapshots
     */
    public Map<String, DebugSnapshot> captureFrameAndPublish(long frameNumber) {
        Map<String, DebugSnapshot> snapshots = session.captureFrame(frameNumber);
        if (!snapshots.isEmpty()) {
            eventBus.publish(DebugSnapshotCapturedEvent.of(frameNumber, snapshots));
        }
        return snapshots;
    }

    /**
     * Execute a debug command and publish a {@link DebugCommandExecutedEvent}.
     *
     * @param name command name
     * @param args command arguments
     * @return the command result
     */
    public DebugCommandResult executeCommandAndPublish(String name, String... args) {
        DebugCommandResult result = session.commands().execute(name, args);
        eventBus.publish(DebugCommandExecutedEvent.of(name, result));
        return result;
    }

    /**
     * Toggle a flag and publish a {@link DebugFlagToggledEvent}.
     *
     * @param flagName the flag to toggle
     */
    public void toggleFlagAndPublish(String flagName) {
        session.toggleFlag(flagName);
        boolean newValue = session.flag(flagName);
        eventBus.publish(DebugFlagToggledEvent.of(flagName, newValue));
    }

    /**
     * Forwards a {@link DebugEvent} onto the engine event bus as a
     * {@link DebugEventForwardedEvent}. Called when this adapter is
     * registered as a sink on the session.
     */
    @Override
    public void submit(DebugEvent event) {
        session.submit(event);
        if (forwardEvents) {
            eventBus.publish(DebugEventForwardedEvent.of(event));
        }
    }

    /** Enable or disable forwarding of debug events to the event bus. */
    public void setForwardEvents(boolean forward) {
        this.forwardEvents = forward;
    }

    public boolean isForwardingEvents() { return forwardEvents; }

    public DebugSession session() { return session; }

    public EventBus eventBus() { return eventBus; }
}
