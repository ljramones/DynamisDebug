package org.dynamisengine.debug.bridge;

import org.dynamisengine.animis.runtime.api.RootMotionDelta;
import org.dynamisengine.collision.bounds.Aabb;
import org.dynamisengine.collision.debug.CollisionDebugSnapshot3D;
import org.dynamisengine.collision.pipeline.CollisionPair;
import org.dynamisengine.debug.api.DebugCategory;
import org.dynamisengine.debug.api.DebugQuery;
import org.dynamisengine.debug.api.DebugSeverity;
import org.dynamisengine.debug.api.DebugSnapshot;
import org.dynamisengine.debug.api.WatchdogRule;
import org.dynamisengine.debug.bridge.adapter.*;
import org.dynamisengine.debug.core.DebugSession;
import org.dynamisengine.debug.core.DebugTimeline;
import org.dynamisengine.gpu.api.BindlessHeapStats;
import org.dynamisengine.gpu.api.upload.UploadTelemetry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test proving the complete data flow:
 * subsystem telemetry → adapter → bridge → session → history → timeline → query.
 *
 * Uses all six real adapters with realistic telemetry data.
 */
class FullPipelineIntegrationTest {

    private DebugSession session;
    private DebugBridge bridge;

    @BeforeEach
    void setUp() {
        session = new DebugSession(256, 300);
        bridge = new DebugBridge(session);

        // Register all six adapters
        bridge.registerAdapter(new CollisionTelemetryAdapter<String>(0));
        bridge.registerAdapter(new GpuTelemetryAdapter());
        bridge.registerAdapter(new AnimisTelemetryAdapter());
        bridge.registerAdapter(new EcsTelemetryAdapter());
        bridge.registerAdapter(new SceneGraphTelemetryAdapter());
        bridge.registerAdapter(new ContentTelemetryAdapter());
    }

    @Test
    void allSixAdaptersFlowThroughSingleFrame() {
        submitAllTelemetry();
        var frame = bridge.captureFrame(1);

        assertEquals(6, frame.size());
        assertTrue(frame.containsKey("collision"));
        assertTrue(frame.containsKey("gpu"));
        assertTrue(frame.containsKey("animation"));
        assertTrue(frame.containsKey("ecs"));
        assertTrue(frame.containsKey("scenegraph"));
        assertTrue(frame.containsKey("content"));

        // Verify each has real metrics
        assertTrue(frame.get("collision").metrics().containsKey("colliderCount"));
        assertTrue(frame.get("gpu").metrics().containsKey("throughputGbps"));
        assertTrue(frame.get("animation").metrics().containsKey("jointCount"));
        assertTrue(frame.get("ecs").metrics().containsKey("entityCount"));
        assertTrue(frame.get("scenegraph").metrics().containsKey("totalNodeCount"));
        assertTrue(frame.get("content").metrics().containsKey("cachedAssetCount"));
    }

    @Test
    void historyAccumulatesAcrossFrames() {
        for (int i = 1; i <= 10; i++) {
            submitAllTelemetry();
            bridge.captureFrame(i);
        }

        assertEquals(10, session.history().size());
        var latest = session.history().latest().orElseThrow();
        assertEquals(10, latest.frameNumber());
        assertEquals(6, latest.snapshots().size());
    }

    @Test
    void timelineExtractsMetricTrends() {
        for (int i = 1; i <= 5; i++) {
            submitAllTelemetry();
            bridge.captureFrame(i);
        }

        // Extract a GPU metric across frames
        var points = session.timeline().extractMetric("gpu", "throughputGbps", 10);
        assertEquals(5, points.size());
        assertEquals(1.5, points.get(0).value());

        // Extract an ECS metric
        var ecsPoints = session.timeline().extractMetric("ecs", "entityCount", 10);
        assertEquals(5, ecsPoints.size());
        assertEquals(200.0, ecsPoints.get(0).value());
    }

    @Test
    void timelineStatsWork() {
        for (int i = 1; i <= 5; i++) {
            submitAllTelemetry();
            bridge.captureFrame(i);
        }

        var stats = session.timeline().stats("ecs", "entityCount", 10);
        assertEquals(200.0, stats.min());
        assertEquals(200.0, stats.max());
        assertEquals(200.0, stats.average());
        assertEquals(5, stats.sampleCount());
    }

    @Test
    void queryByCategory() {
        submitAllTelemetry();
        bridge.captureFrame(1);

        // Query PHYSICS category — should find collision
        var physics = session.queries().querySnapshots(
                DebugQuery.builder().categories(DebugCategory.PHYSICS).build()
        );
        assertEquals(1, physics.size());
        assertEquals("collision", physics.get(0).source());

        // Query RENDERING — should find gpu + scenegraph
        var rendering = session.queries().querySnapshots(
                DebugQuery.builder().categories(DebugCategory.RENDERING).build()
        );
        assertEquals(2, rendering.size());

        // Query ECS
        var ecs = session.queries().querySnapshots(
                DebugQuery.builder().categories(DebugCategory.ECS).build()
        );
        assertEquals(1, ecs.size());
        assertEquals("ecs", ecs.get(0).source());

        // Query CONTENT
        var content = session.queries().querySnapshots(
                DebugQuery.builder().categories(DebugCategory.CONTENT).build()
        );
        assertEquals(1, content.size());
    }

    @Test
    void queryBySource() {
        submitAllTelemetry();
        bridge.captureFrame(1);

        var result = session.queries().querySnapshots(
                DebugQuery.builder().sources("animation").build()
        );
        assertEquals(1, result.size());
        assertTrue(result.get(0).text().contains("state=idle"));
    }

    @Test
    void queryByFrameRange() {
        for (int i = 1; i <= 10; i++) {
            submitAllTelemetry();
            bridge.captureFrame(i);
        }

        var result = session.queries().querySnapshots(
                DebugQuery.builder().sources("ecs").frameRange(5, 7).build()
        );
        assertEquals(3, result.size());
    }

    @Test
    void snapshotsForSpecificFrame() {
        submitAllTelemetry();
        bridge.captureFrame(42);

        var frame = session.queries().snapshotsForFrame(42);
        assertEquals(6, frame.size());
    }

    @Test
    void crossSourceMetricSearch() {
        submitAllTelemetry();
        bridge.captureFrame(1);

        // Search for a metric that only exists in one source
        var points = session.timeline().extractMetricAcrossSources("throughputGbps", 10);
        assertEquals(1, points.size());
        assertEquals("gpu", points.get(0).source());
    }

    @Test
    void flagsFlowCorrectly() {
        submitAllTelemetry();
        var frame = bridge.captureFrame(1);

        // Collision should have hasContacts=true
        assertTrue(frame.get("collision").flags().get("hasContacts"));
        // GPU should have backlogPressure flag
        assertFalse(frame.get("gpu").flags().get("backlogPressure"));
        // Animation should not be in transition
        assertFalse(frame.get("animation").flags().get("inTransition"));
        // Content should have no failures
        assertFalse(frame.get("content").flags().get("hasFailures"));
    }

    @Test
    void textSummariesPresent() {
        submitAllTelemetry();
        var frame = bridge.captureFrame(1);

        for (var entry : frame.entrySet()) {
            assertNotNull(entry.getValue().text(),
                    entry.getKey() + " should have text summary");
            assertFalse(entry.getValue().text().isEmpty(),
                    entry.getKey() + " text should not be empty");
        }
    }

    // --- Watchdog integration ---

    @Test
    void watchdogFiresOnThresholdBreach() {
        session.watchdog().addRule(WatchdogRule.above(
                "collision.contactSpike", "collision", "contactCount",
                50.0, DebugSeverity.WARNING, "Contact count spike"));

        // Submit telemetry with high contact count
        bridge.submitTelemetry("collision",
                new CollisionDebugSnapshot3D<>(List.of(), List.of()));
        bridge.submitTelemetry("ecs",
                new EcsTelemetryAdapter.EcsTelemetrySnapshot(200, 0, 0, 1));

        bridge.captureFrame(1);

        // Contact count is 0, so rule should NOT fire
        var events = session.drainEvents();
        assertTrue(events.isEmpty());
    }

    @Test
    void watchdogDetectsCrossSubsystemCorrelation() {
        // Rule: GPU backlog spikes when ECS churn is high
        session.watchdog().addRule(WatchdogRule.above(
                "ecs.massDestroy", "ecs", "destroyedCount",
                50.0, DebugSeverity.WARNING, "Mass entity destruction"));
        session.watchdog().addRule(WatchdogRule.above(
                "gpu.backlogSpike", "gpu", "backlogDepth",
                3.0, DebugSeverity.WARNING, "GPU backlog under pressure"));

        // Simulate a spike frame: ECS churn + GPU backlog
        bridge.registerAdapter(new GpuTelemetryAdapter());
        bridge.registerAdapter(new EcsTelemetryAdapter());

        bridge.submitTelemetry("gpu",
                GpuTelemetryAdapter.GpuTelemetrySnapshot.of(
                        new UploadTelemetry(2, 5, 8192, 4, 16384, 5, 100, 102400, 0.5, 2.0, 8.0, 1.5)));
        bridge.submitTelemetry("ecs",
                new EcsTelemetryAdapter.EcsTelemetrySnapshot(500, 0, 100, 42));

        bridge.captureFrame(1);

        // Both rules should fire — this is the cross-subsystem correlation
        var events = session.drainEvents();
        assertEquals(2, events.size());

        var names = events.stream().map(e -> e.name()).toList();
        assertTrue(names.contains("ecs.massDestroy"));
        assertTrue(names.contains("gpu.backlogSpike"));
    }

    @Test
    void watchdogEventsAppearInEventQueries() {
        session.watchdog().addRule(WatchdogRule.above(
                "ecs.churn", "ecs", "destroyedCount",
                10.0, DebugSeverity.ERROR, "High entity churn"));

        bridge.registerAdapter(new EcsTelemetryAdapter());
        bridge.submitTelemetry("ecs",
                new EcsTelemetryAdapter.EcsTelemetrySnapshot(100, 0, 50, 1));
        bridge.captureFrame(1);

        var events = session.queries().queryEvents(
                DebugQuery.builder().severities(DebugSeverity.ERROR).build()
        );
        assertEquals(1, events.size());
        assertEquals("ecs.churn", events.get(0).name());
    }

    // --- Telemetry submission helpers ---

    private void submitAllTelemetry() {
        // Collision: 2 colliders, 1 ENTER contact
        var collisionItems = List.of(
                new CollisionDebugSnapshot3D.ItemBounds<>("a", new Aabb(0, 0, 0, 1, 1, 1)),
                new CollisionDebugSnapshot3D.ItemBounds<>("b", new Aabb(2, 2, 2, 3, 3, 3))
        );
        var collisionContacts = List.of(
                new CollisionDebugSnapshot3D.Contact<>(new CollisionPair<>("a", "b"),
                        org.dynamisengine.collision.events.CollisionEventType.ENTER, true,
                        new org.dynamisengine.collision.narrowphase.CollisionManifold3D(0, 1, 0, 0.1),
                        new org.dynamisengine.collision.contact.ContactPoint3D(1, 1, 1))
        );
        bridge.submitTelemetry("collision",
                new CollisionDebugSnapshot3D<>(collisionItems, collisionContacts));

        // GPU: healthy upload state
        bridge.submitTelemetry("gpu",
                GpuTelemetryAdapter.GpuTelemetrySnapshot.of(
                        new UploadTelemetry(1, 0, 2048, 2, 4096, 1, 50, 51200, 1.5, 0.5, 1.2, 0.3),
                        new BindlessHeapStats(80, 8192, 40, 4096, 20, 4096, 150, 4096,
                                400, 5, 4, 0, 30, 0)));

        // Animation: idle state, no transition
        bridge.submitTelemetry("animation",
                AnimisTelemetryAdapter.AnimisTelemetrySnapshot.stable(
                        "idle", RootMotionDelta.ZERO, 65, List.of()));

        // ECS: 200 entities, no churn
        bridge.submitTelemetry("ecs",
                new EcsTelemetryAdapter.EcsTelemetrySnapshot(200, 0, 0, 1));

        // SceneGraph: 50 nodes, 40 renderable, 35 visible, 3 batches
        bridge.submitTelemetry("scenegraph",
                new SceneGraphTelemetryAdapter.SceneGraphTelemetrySnapshot(50, 40, 35, 3, 35));

        // Content: healthy cache
        bridge.submitTelemetry("content",
                new ContentTelemetryAdapter.ContentTelemetrySnapshot(25, 20, 80, 60, 20, 0, 3));
    }
}
