package org.dynamisengine.debug.bridge.adapter;

import org.dynamisengine.animis.runtime.api.RootMotionDelta;
import org.dynamisengine.debug.api.DebugCategory;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AnimisTelemetryAdapterTest {

    private final AnimisTelemetryAdapter adapter = new AnimisTelemetryAdapter();

    @Test
    void subsystemNameAndCategory() {
        assertEquals("animation", adapter.subsystemName());
        assertEquals(DebugCategory.ENGINE, adapter.category());
    }

    @Test
    void adaptStableState() {
        var snapshot = AnimisTelemetryAdapter.AnimisTelemetrySnapshot.stable(
                "idle", RootMotionDelta.ZERO, 65, List.of()
        );
        var debug = adapter.adapt(snapshot, 1);

        assertEquals("animation", debug.source());
        assertEquals(65.0, debug.metrics().get("jointCount"));
        assertEquals(0.0, debug.metrics().get("firedEventCount"));
        assertFalse(debug.flags().get("inTransition"));
        assertFalse(debug.flags().get("hasRootMotion"));
        assertTrue(debug.text().contains("state=idle"));
        assertFalse(debug.metrics().containsKey("transitionProgress"));
    }

    @Test
    void adaptWithActiveTransition() {
        var snapshot = AnimisTelemetryAdapter.AnimisTelemetrySnapshot.transitioning(
                "idle", "run", 0.15f, 0.3f, RootMotionDelta.ZERO, 65, List.of()
        );
        var debug = adapter.adapt(snapshot, 1);

        assertTrue(debug.flags().get("inTransition"));
        assertEquals(0.5, debug.metrics().get("transitionProgress"), 0.01);
        assertEquals(0.3, debug.metrics().get("transitionBlendSeconds"), 0.01);
        assertTrue(debug.text().contains("-> run"));
        assertTrue(debug.text().contains("50%"));
    }

    @Test
    void adaptWithRootMotion() {
        var rootMotion = new RootMotionDelta(1.5f, 0f, 0.5f, 0.1f);
        var snapshot = AnimisTelemetryAdapter.AnimisTelemetrySnapshot.stable(
                "walk", rootMotion, 65, List.of()
        );
        var debug = adapter.adapt(snapshot, 1);

        assertTrue(debug.flags().get("hasRootMotion"));
        assertEquals(1.5, debug.metrics().get("rootMotion.dx"), 0.01);
        assertEquals(0.0, debug.metrics().get("rootMotion.dy"), 0.01);
        assertEquals(0.5, debug.metrics().get("rootMotion.dz"), 0.01);
        assertEquals(0.1, debug.metrics().get("rootMotion.dyaw"), 0.01);
    }

    @Test
    void adaptWithFiredEvents() {
        var snapshot = AnimisTelemetryAdapter.AnimisTelemetrySnapshot.stable(
                "attack", RootMotionDelta.ZERO, 65, List.of("footstep", "weaponSwing")
        );
        var debug = adapter.adapt(snapshot, 1);

        assertEquals(2.0, debug.metrics().get("firedEventCount"));
        assertTrue(debug.text().contains("events="));
        assertTrue(debug.text().contains("footstep"));
    }

    @Test
    void extractMetricsConsistentWithAdapt() {
        var snapshot = AnimisTelemetryAdapter.AnimisTelemetrySnapshot.stable(
                "idle", null, 30, List.of()
        );
        var metrics = adapter.extractMetrics(snapshot);
        assertEquals(30.0, metrics.get("jointCount"));
        assertEquals(0.0, metrics.get("firedEventCount"));
    }

    @Test
    void transitionClampedAtOneHundredPercent() {
        var snapshot = AnimisTelemetryAdapter.AnimisTelemetrySnapshot.transitioning(
                "idle", "run", 0.5f, 0.3f, RootMotionDelta.ZERO, 65, List.of()
        );
        var debug = adapter.adapt(snapshot, 1);

        assertEquals(1.0, debug.metrics().get("transitionProgress"), 0.01);
        assertTrue(debug.text().contains("100%"));
    }

    @Test
    void nullRootMotionHandled() {
        var snapshot = AnimisTelemetryAdapter.AnimisTelemetrySnapshot.stable(
                "idle", null, 65, List.of()
        );
        var debug = adapter.adapt(snapshot, 1);
        assertFalse(debug.flags().get("hasRootMotion"));
        assertFalse(debug.metrics().containsKey("rootMotion.dx"));
    }

    @Test
    void nullEventsDefaultsToEmpty() {
        var snapshot = new AnimisTelemetryAdapter.AnimisTelemetrySnapshot(
                "idle", null, 0, 0, null, 65, null
        );
        assertEquals(0, snapshot.firedEvents().size());
    }
}
