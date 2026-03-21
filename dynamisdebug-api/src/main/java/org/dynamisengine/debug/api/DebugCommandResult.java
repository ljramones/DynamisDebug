package org.dynamisengine.debug.api;

/**
 * Result of executing a debug command.
 *
 * @param success whether the command succeeded
 * @param message output/result text
 */
public record DebugCommandResult(boolean success, String message) {

    public static DebugCommandResult ok(String message) { return new DebugCommandResult(true, message); }
    public static DebugCommandResult error(String message) { return new DebugCommandResult(false, message); }
}
