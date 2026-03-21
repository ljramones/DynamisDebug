package org.dynamisengine.debug.core;

import org.dynamisengine.debug.api.DebugCommand;
import org.dynamisengine.debug.api.DebugCommandRegistry;
import org.dynamisengine.debug.api.DebugCommandResult;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default command registry implementation.
 */
public final class DefaultCommandRegistry implements DebugCommandRegistry {

    private final Map<String, DebugCommand> commands = new ConcurrentHashMap<>();

    @Override
    public void register(DebugCommand command) {
        commands.put(command.name(), command);
    }

    @Override
    public Optional<DebugCommand> find(String name) {
        return Optional.ofNullable(commands.get(name));
    }

    @Override
    public Collection<DebugCommand> all() {
        return Collections.unmodifiableCollection(commands.values());
    }

    @Override
    public DebugCommandResult execute(String name, String... args) {
        DebugCommand cmd = commands.get(name);
        if (cmd == null) return DebugCommandResult.error("Unknown command: " + name);
        try {
            return cmd.handler().execute(args);
        } catch (Exception e) {
            return DebugCommandResult.error("Command failed: " + e.getMessage());
        }
    }
}
