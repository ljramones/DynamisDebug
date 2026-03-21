package org.dynamisengine.debug.bridge;

import org.dynamisengine.debug.api.*;
import org.dynamisengine.debug.core.DebugSession;
import org.dynamisengine.debug.core.DebugTimeline;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DebugBridgeTest {

    private DebugSession session;
    private DebugBridge bridge;

    @BeforeEach
    void setUp() {
        session = new DebugSession(64, 100);
        bridge = new DebugBridge(session);
    }

    // --- Registration ---

    @Test
    void registerAndCountAdapters() {
        bridge.registerAdapter(new StubAdapter("physics"));
        bridge.registerAdapter(new StubAdapter("audio"));
        assertEquals(2, bridge.adapterCount());
    }

    @Test
    void unregisterRemovesAdapter() {
        bridge.registerAdapter(new StubAdapter("physics"));
        bridge.unregisterAdapter("physics");
        assertEquals(0, bridge.adapterCount());
    }

    @Test
    void sessionAccessible() {
        assertSame(session, bridge.session());
    }

    @Test
    void duplicateNameOverwrites() {
        bridge.registerAdapter(new StubAdapter("audio"));
        bridge.registerAdapter(new StubAdapter("audio"));
        assertEquals(1, bridge.adapterCount());
    }

    // --- Telemetry submission ---

    @Test
    void submitTelemetryBuffered() {
        bridge.registerAdapter(new StubAdapter("physics"));
        bridge.submitTelemetry("physics", "data");
        assertEquals(1, bridge.pendingTelemetryCount());
    }

    @Test
    void submitTelemetryIgnoredForUnregisteredAdapter() {
        bridge.submitTelemetry("unknown", "data");
        assertEquals(0, bridge.pendingTelemetryCount());
    }

    @Test
    void unregisterClearsPendingTelemetry() {
        bridge.registerAdapter(new StubAdapter("physics"));
        bridge.submitTelemetry("physics", "data");
        bridge.unregisterAdapter("physics");
        assertEquals(0, bridge.pendingTelemetryCount());
    }

    // --- Frame capture ---

    @Test
    void captureFrameAdaptsTelemetry() {
        bridge.registerAdapter(new MetricAdapter("gpu", Map.of("inflight", 2.0)));
        bridge.submitTelemetry("gpu", "raw");

        var frame = bridge.captureFrame(1);

        assertTrue(frame.containsKey("gpu"));
        assertEquals(2.0, frame.get("gpu").metrics().get("inflight"));
    }

    @Test
    void captureFrameClearsPendingTelemetry() {
        bridge.registerAdapter(new StubAdapter("ecs"));
        bridge.submitTelemetry("ecs", "data");

        bridge.captureFrame(1);
        assertEquals(0, bridge.pendingTelemetryCount());
    }

    @Test
    void captureFrameMergesProvidersAndAdapters() {
        // Register a provider on the session (pull model)
        session.registerProvider(new DebugSnapshotProvider() {
            @Override public String debugSourceName() { return "provider"; }
            @Override public DebugCategory debugCategory() { return DebugCategory.ENGINE; }
            @Override public DebugSnapshot captureSnapshot(long frameNumber) {
                return new DebugSnapshot(frameNumber, 0, "provider", DebugCategory.ENGINE,
                        Map.of("tick", 1.0), Map.of(), "from provider");
            }
        });

        // Register an adapter on the bridge (push model)
        bridge.registerAdapter(new MetricAdapter("gpu", Map.of("inflight", 5.0)));
        bridge.submitTelemetry("gpu", "raw");

        var frame = bridge.captureFrame(1);

        // Both sources present in merged frame
        assertEquals(2, frame.size());
        assertTrue(frame.containsKey("provider"));
        assertTrue(frame.containsKey("gpu"));
        assertEquals(1.0, frame.get("provider").metrics().get("tick"));
        assertEquals(5.0, frame.get("gpu").metrics().get("inflight"));
    }

    @Test
    void captureFrameStoresInHistory() {
        bridge.registerAdapter(new MetricAdapter("ecs", Map.of("entities", 100.0)));
        bridge.submitTelemetry("ecs", "raw");

        bridge.captureFrame(1);

        var latest = session.history().latest().orElseThrow();
        assertEquals(1, latest.frameNumber());
        assertTrue(latest.snapshots().containsKey("ecs"));
    }

    @Test
    void captureFrameFlowsToTimeline() {
        bridge.registerAdapter(new MetricAdapter("ecs", Map.of("entities", 50.0)));

        // Capture 3 frames with same adapter
        for (int i = 1; i <= 3; i++) {
            bridge.submitTelemetry("ecs", "raw");
            bridge.captureFrame(i);
        }

        var points = session.timeline().extractMetric("ecs", "entities", 10);
        assertEquals(3, points.size());
        assertEquals(50.0, points.get(0).value());
    }

    @Test
    void captureFrameFlowsToQueries() {
        bridge.registerAdapter(new MetricAdapter("gpu", Map.of("inflight", 2.0)));
        bridge.submitTelemetry("gpu", "raw");
        bridge.captureFrame(1);

        var results = session.queries().querySnapshots(
                DebugQuery.builder().categories(DebugCategory.RENDERING).build()
        );
        assertEquals(1, results.size());
        assertEquals("gpu", results.get(0).source());
    }

    @Test
    void disabledSessionSkipsCapture() {
        bridge.registerAdapter(new StubAdapter("ecs"));
        bridge.submitTelemetry("ecs", "data");
        session.setEnabled(false);

        var frame = bridge.captureFrame(1);
        assertTrue(frame.isEmpty());
    }

    @Test
    void adapterExceptionDoesNotCrash() {
        bridge.registerAdapter(new TelemetryAdapter<String>() {
            @Override public String subsystemName() { return "broken"; }
            @Override public DebugCategory category() { return DebugCategory.ENGINE; }
            @Override public DebugSnapshot adapt(String t, long f) { throw new RuntimeException("boom"); }
            @Override public Map<String, Double> extractMetrics(String t) { return Map.of(); }
        });
        bridge.submitTelemetry("broken", "data");

        // Should not throw
        var frame = bridge.captureFrame(1);
        assertFalse(frame.containsKey("broken"));
    }

    @Test
    void multipleAdaptersInSingleFrame() {
        bridge.registerAdapter(new MetricAdapter("collision", Map.of("contacts", 10.0)));
        bridge.registerAdapter(new MetricAdapter("gpu", Map.of("inflight", 2.0)));
        bridge.registerAdapter(new MetricAdapter("ecs", Map.of("entities", 500.0)));

        bridge.submitTelemetry("collision", "raw");
        bridge.submitTelemetry("gpu", "raw");
        bridge.submitTelemetry("ecs", "raw");

        var frame = bridge.captureFrame(1);
        assertEquals(3, frame.size());
        assertEquals(10.0, frame.get("collision").metrics().get("contacts"));
        assertEquals(2.0, frame.get("gpu").metrics().get("inflight"));
        assertEquals(500.0, frame.get("ecs").metrics().get("entities"));
    }

    @Test
    void consecutiveFramesBuildHistory() {
        bridge.registerAdapter(new MetricAdapter("ecs", Map.of("entities", 0.0)));

        for (int i = 1; i <= 5; i++) {
            bridge.submitTelemetry("ecs", "raw");
            bridge.captureFrame(i);
        }

        assertEquals(5, session.history().size());
        var recent = session.history().recent(3);
        assertEquals(3, recent.get(0).frameNumber());
        assertEquals(5, recent.get(2).frameNumber());
    }

    // --- Helpers ---

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

    private static class MetricAdapter implements TelemetryAdapter<String> {
        private final String name;
        private final Map<String, Double> metrics;
        MetricAdapter(String name, Map<String, Double> metrics) {
            this.name = name;
            this.metrics = metrics;
        }
        @Override public String subsystemName() { return name; }
        @Override public DebugCategory category() { return DebugCategory.RENDERING; }
        @Override public DebugSnapshot adapt(String telemetry, long frameNumber) {
            return new DebugSnapshot(frameNumber, System.currentTimeMillis(), name,
                    DebugCategory.RENDERING, metrics, Map.of(), "");
        }
        @Override public Map<String, Double> extractMetrics(String telemetry) { return metrics; }
    }
}
