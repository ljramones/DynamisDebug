package org.dynamisengine.debug.bridge.event;

import org.dynamisengine.core.event.EngineEvent;
import org.dynamisengine.debug.api.event.DebugEvent;

/**
 * Published when a {@link DebugEvent} is forwarded from the debug
 * event buffer onto the engine event bus.
 *
 * <p>This bridges the gap between the debug-internal event model
 * and the engine-wide event dispatch system.
 *
 * @param debugEvent the original debug event
 * @param timestamp  event creation time in nanos
 */
public record DebugEventForwardedEvent(
        DebugEvent debugEvent,
        long timestamp
) implements EngineEvent {

    public static DebugEventForwardedEvent of(DebugEvent debugEvent) {
        return new DebugEventForwardedEvent(debugEvent, System.nanoTime());
    }
}
