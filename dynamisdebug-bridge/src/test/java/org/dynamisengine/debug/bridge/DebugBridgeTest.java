package org.dynamisengine.debug.bridge;

import org.dynamisengine.debug.api.DebugCategory;
import org.dynamisengine.debug.api.DebugSnapshot;
import org.dynamisengine.debug.core.DebugSession;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DebugBridgeTest {

    @Test
    void registerAndCountAdapters() {
        var bridge = new DebugBridge(new DebugSession());

        bridge.registerAdapter(new StubAdapter("physics"));
        bridge.registerAdapter(new StubAdapter("audio"));
        assertEquals(2, bridge.adapterCount());
    }

    @Test
    void unregisterRemovesAdapter() {
        var bridge = new DebugBridge(new DebugSession());
        bridge.registerAdapter(new StubAdapter("physics"));
        bridge.unregisterAdapter("physics");
        assertEquals(0, bridge.adapterCount());
    }

    @Test
    void sessionAccessible() {
        var session = new DebugSession();
        var bridge = new DebugBridge(session);
        assertSame(session, bridge.session());
    }

    @Test
    void duplicateNameOverwrites() {
        var bridge = new DebugBridge(new DebugSession());
        bridge.registerAdapter(new StubAdapter("audio"));
        bridge.registerAdapter(new StubAdapter("audio"));
        assertEquals(1, bridge.adapterCount());
    }

    private static class StubAdapter implements TelemetryAdapter<String> {
        private final String name;
        StubAdapter(String name) { this.name = name; }
        @Override public String subsystemName() { return name; }
        @Override public DebugCategory category() { return DebugCategory.ENGINE; }
        @Override public DebugSnapshot adapt(String telemetry, long frameNumber) {
            return new DebugSnapshot(frameNumber, 0, name, DebugCategory.ENGINE, Map.of(), Map.of(), "");
        }
        @Override public Map<String, Double> extractMetrics(String telemetry) { return Map.of(); }
    }
}
