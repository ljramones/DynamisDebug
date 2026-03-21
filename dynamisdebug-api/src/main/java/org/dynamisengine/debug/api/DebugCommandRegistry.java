package org.dynamisengine.debug.api;

import java.util.Collection;
import java.util.Optional;

/**
 * Registry for runtime debug commands.
 */
public interface DebugCommandRegistry {

    void register(DebugCommand command);

    Optional<DebugCommand> find(String name);

    Collection<DebugCommand> all();

    DebugCommandResult execute(String name, String... args);
}
