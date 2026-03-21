package org.dynamisengine.debug.bridge.event;

import org.dynamisengine.core.event.EngineEvent;

/**
 * Published when a debug flag is toggled through the adapter.
 *
 * @param flagName the flag that was toggled
 * @param newValue the flag's new state after toggling
 * @param timestamp event creation time in nanos
 */
public record DebugFlagToggledEvent(
        String flagName,
        boolean newValue,
        long timestamp
) implements EngineEvent {

    public static DebugFlagToggledEvent of(String flagName, boolean newValue) {
        return new DebugFlagToggledEvent(flagName, newValue, System.nanoTime());
    }
}
