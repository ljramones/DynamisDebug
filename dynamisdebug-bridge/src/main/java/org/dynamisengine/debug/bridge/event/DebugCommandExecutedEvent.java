package org.dynamisengine.debug.bridge.event;

import org.dynamisengine.core.event.EngineEvent;
import org.dynamisengine.debug.api.DebugCommandResult;

/**
 * Published when a debug command is executed through the adapter.
 *
 * @param commandName the command that was executed
 * @param result      the execution result
 * @param timestamp   event creation time in nanos
 */
public record DebugCommandExecutedEvent(
        String commandName,
        DebugCommandResult result,
        long timestamp
) implements EngineEvent {

    public static DebugCommandExecutedEvent of(String commandName, DebugCommandResult result) {
        return new DebugCommandExecutedEvent(commandName, result, System.nanoTime());
    }
}
