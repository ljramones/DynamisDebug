package org.dynamisengine.debug.api;

/**
 * A registered debug command that can be executed at runtime.
 *
 * @param name        command name (e.g. "gc", "resetCounters", "toggleWireframe")
 * @param description brief help text
 * @param handler     execution handler
 */
public record DebugCommand(String name, String description, DebugCommandHandler handler) {

    @FunctionalInterface
    public interface DebugCommandHandler {
        DebugCommandResult execute(String[] args);
    }
}
