package org.dynamisengine.debug.bridge.adapter;

import org.dynamisengine.collision.bounds.Aabb;
import org.dynamisengine.collision.contact.ContactPoint3D;
import org.dynamisengine.collision.debug.CollisionDebugSnapshot3D;
import org.dynamisengine.collision.events.CollisionEventType;
import org.dynamisengine.collision.narrowphase.CollisionManifold3D;
import org.dynamisengine.collision.pipeline.CollisionPair;
import org.dynamisengine.debug.api.DebugCategory;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CollisionTelemetryAdapterTest {

    private final CollisionTelemetryAdapter<String> adapter = new CollisionTelemetryAdapter<>(5);

    @Test
    void subsystemNameAndCategory() {
        assertEquals("collision", adapter.subsystemName());
        assertEquals(DebugCategory.PHYSICS, adapter.category());
    }

    @Test
    void adaptEmptySnapshot() {
        var snapshot = new CollisionDebugSnapshot3D<>(List.<CollisionDebugSnapshot3D.ItemBounds<String>>of(), List.<CollisionDebugSnapshot3D.Contact<String>>of());
        var debug = adapter.adapt(snapshot, 1);

        assertEquals(1, debug.frameNumber());
        assertEquals("collision", debug.source());
        assertEquals(0.0, debug.metrics().get("colliderCount"));
        assertEquals(0.0, debug.metrics().get("contactCount"));
        assertEquals(5.0, debug.metrics().get("manifoldCacheSize"));
        assertFalse(debug.flags().get("hasContacts"));
    }

    @Test
    void adaptSnapshotWithContacts() {
        var pair = new CollisionPair<>("a", "b");
        var manifold = new CollisionManifold3D(0, 1, 0, 0.5);
        var pointA = new ContactPoint3D(1, 2, 3);
        var pointB = new ContactPoint3D(4, 5, 6);

        var items = List.of(
                new CollisionDebugSnapshot3D.ItemBounds<>("a", new Aabb(0, 0, 0, 1, 1, 1)),
                new CollisionDebugSnapshot3D.ItemBounds<>("b", new Aabb(2, 2, 2, 3, 3, 3))
        );

        var contacts = List.of(
                new CollisionDebugSnapshot3D.Contact<>(pair, CollisionEventType.ENTER, true, manifold, pointA),
                new CollisionDebugSnapshot3D.Contact<>(pair, CollisionEventType.STAY, true, manifold, pointB),
                new CollisionDebugSnapshot3D.Contact<>(pair, CollisionEventType.EXIT, false, manifold, pointA)
        );

        var snapshot = new CollisionDebugSnapshot3D<>(items, contacts);
        var debug = adapter.adapt(snapshot, 42);

        assertEquals(2.0, debug.metrics().get("colliderCount"));
        assertEquals(3.0, debug.metrics().get("contactCount"));
        assertEquals(1.0, debug.metrics().get("contactEnterCount"));
        assertEquals(1.0, debug.metrics().get("contactStayCount"));
        assertEquals(1.0, debug.metrics().get("contactExitCount"));
        assertEquals(2.0, debug.metrics().get("responseEnabledCount"));
        assertTrue(debug.flags().get("hasContacts"));
    }

    @Test
    void extractMetricsMatchesAdapt() {
        var snapshot = new CollisionDebugSnapshot3D<String>(
                List.of(new CollisionDebugSnapshot3D.ItemBounds<>("x", new Aabb(0, 0, 0, 1, 1, 1))),
                List.of()
        );
        var metrics = adapter.extractMetrics(snapshot);
        assertEquals(1.0, metrics.get("colliderCount"));
        assertEquals(0.0, metrics.get("contactCount"));
    }

    @Test
    void withCacheSizeCreatesNewAdapter() {
        var updated = adapter.withCacheSize(10);
        var snapshot = new CollisionDebugSnapshot3D<String>(List.of(), List.of());
        assertEquals(10.0, updated.extractMetrics(snapshot).get("manifoldCacheSize"));
    }

    @Test
    void textSummary() {
        var snapshot = new CollisionDebugSnapshot3D<String>(List.of(), List.of());
        var debug = adapter.adapt(snapshot, 1);
        assertTrue(debug.text().contains("colliders=0"));
        assertTrue(debug.text().contains("contacts=0"));
        assertTrue(debug.text().contains("cache=5"));
    }
}
