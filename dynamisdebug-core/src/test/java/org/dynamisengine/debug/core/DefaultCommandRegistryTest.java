package org.dynamisengine.debug.core;

import org.dynamisengine.debug.api.DebugCommand;
import org.dynamisengine.debug.api.DebugCommandResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DefaultCommandRegistryTest {

    private DefaultCommandRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new DefaultCommandRegistry();
    }

    @Test
    void registerAndFind() {
        var cmd = new DebugCommand("gc", "Run GC", args -> DebugCommandResult.ok("done"));
        registry.register(cmd);

        assertTrue(registry.find("gc").isPresent());
        assertEquals("gc", registry.find("gc").orElseThrow().name());
    }

    @Test
    void findUnknownReturnsEmpty() {
        assertTrue(registry.find("nope").isEmpty());
    }

    @Test
    void executeSuccess() {
        registry.register(new DebugCommand("echo", "Echo args", args ->
                DebugCommandResult.ok(String.join(" ", args))));

        var result = registry.execute("echo", "hello", "world");
        assertTrue(result.success());
        assertEquals("hello world", result.message());
    }

    @Test
    void executeUnknownCommandReturnsError() {
        var result = registry.execute("missing");
        assertFalse(result.success());
        assertTrue(result.message().contains("Unknown"));
    }

    @Test
    void executeHandlesHandlerException() {
        registry.register(new DebugCommand("boom", "Throws", args -> {
            throw new RuntimeException("oops");
        }));

        var result = registry.execute("boom");
        assertFalse(result.success());
        assertTrue(result.message().contains("oops"));
    }

    @Test
    void allReturnsAllRegistered() {
        registry.register(new DebugCommand("a", "A", args -> DebugCommandResult.ok("")));
        registry.register(new DebugCommand("b", "B", args -> DebugCommandResult.ok("")));
        assertEquals(2, registry.all().size());
    }
}
