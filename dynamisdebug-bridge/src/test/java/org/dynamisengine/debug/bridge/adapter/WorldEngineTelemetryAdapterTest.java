package org.dynamisengine.debug.bridge.adapter;

import org.dynamisengine.debug.api.DebugCategory;
import org.dynamisengine.worldengine.api.WorldEngineState;
import org.dynamisengine.worldengine.api.telemetry.EngineTelemetry;
import org.dynamisengine.worldengine.api.telemetry.SubsystemHealth;
import org.dynamisengine.worldengine.api.telemetry.SubsystemTelemetry;
import org.dynamisengine.worldengine.api.telemetry.WorldTelemetrySnapshot;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WorldEngineTelemetryAdapterTest {

    private final WorldEngineTelemetryAdapter adapter = new WorldEngineTelemetryAdapter();

    private WorldTelemetrySnapshot healthy() {
        return new WorldTelemetrySnapshot(
                new EngineTelemetry(WorldEngineState.RUNNING, 1000, 30.5,
                        16.5, 16.3, 18.2, 16.67, 60),
                Map.of(
                        "Audio", SubsystemTelemetry.healthOnly(SubsystemHealth.healthy("Audio", 1000)),
                        "Input", SubsystemTelemetry.healthOnly(SubsystemHealth.healthy("Input", 1000)),
                        "ECS", SubsystemTelemetry.healthOnly(SubsystemHealth.healthy("ECS", 1000))
                ),
                null);
    }

    @Test
    void subsystemNameAndCategory() {
        assertEquals("worldengine", adapter.subsystemName());
        assertEquals(DebugCategory.ENGINE, adapter.category());
    }

    @Test
    void adaptHealthyEngine() {
        var debug = adapter.adapt(healthy(), 1);

        assertEquals(1000.0, debug.metrics().get("tick"));
        assertEquals(30.5, debug.metrics().get("uptimeSeconds"));
        assertEquals(16.5, debug.metrics().get("lastTickDurationMs"));
        assertEquals(16.3, debug.metrics().get("avgTickDurationMs"));
        assertEquals(18.2, debug.metrics().get("maxTickDurationMs"));
        assertEquals(60.0, debug.metrics().get("tickRate"));
        assertEquals(3.0, debug.metrics().get("healthySubsystemCount"));
        assertEquals(0.0, debug.metrics().get("degradedSubsystemCount"));
        assertEquals(0.0, debug.metrics().get("faultedSubsystemCount"));

        assertTrue(debug.flags().get("running"));
        assertFalse(debug.flags().get("hasIssues"));
        assertFalse(debug.flags().get("overBudget"));
        assertTrue(debug.text().contains("RUNNING"));
        assertTrue(debug.text().contains("tick=1000"));
    }

    @Test
    void adaptWithDegradedSubsystem() {
        var snapshot = new WorldTelemetrySnapshot(
                new EngineTelemetry(WorldEngineState.RUNNING, 500, 10.0,
                        15.0, 15.2, 20.0, 16.67, 60),
                Map.of(
                        "Audio", SubsystemTelemetry.healthOnly(
                                SubsystemHealth.degraded("Audio", "Device fallback", 500)),
                        "Input", SubsystemTelemetry.healthOnly(SubsystemHealth.healthy("Input", 500))
                ),
                null);

        var debug = adapter.adapt(snapshot, 1);
        assertTrue(debug.flags().get("hasIssues"));
        assertEquals(1.0, debug.metrics().get("degradedSubsystemCount"));
        assertEquals(0.5, debug.metrics().get("subsystem.Audio.health"));
        assertEquals(1.0, debug.metrics().get("subsystem.Input.health"));
        assertTrue(debug.text().contains("1 degraded"));
    }

    @Test
    void adaptFaultedEngine() {
        var snapshot = new WorldTelemetrySnapshot(
                new EngineTelemetry(WorldEngineState.FAULTED, 100, 5.0,
                        0, 0, 0, 16.67, 60),
                Map.of(
                        "Renderer", SubsystemTelemetry.healthOnly(
                                SubsystemHealth.faulted("Renderer", "GPU lost"))
                ),
                "Fatal: GPU device lost");

        var debug = adapter.adapt(snapshot, 1);
        assertTrue(debug.flags().get("faulted"));
        assertTrue(debug.flags().get("hasIssues"));
        assertEquals(0.0, debug.metrics().get("subsystem.Renderer.health"));
        assertEquals(1.0, debug.metrics().get("faultedSubsystemCount"));
        assertTrue(debug.text().contains("error: Fatal"));
    }

    @Test
    void adaptOverBudget() {
        var snapshot = new WorldTelemetrySnapshot(
                new EngineTelemetry(WorldEngineState.RUNNING, 200, 5.0,
                        20.0, 19.0, 25.0, 16.67, 60),
                Map.of(), null);

        var debug = adapter.adapt(snapshot, 1);
        assertTrue(debug.flags().get("overBudget"));
        assertTrue(debug.metrics().get("budgetPercent") > 100.0);
    }

    @Test
    void adaptPausedState() {
        var snapshot = new WorldTelemetrySnapshot(
                new EngineTelemetry(WorldEngineState.PAUSED, 300, 10.0,
                        0, 16.0, 18.0, 16.67, 60),
                Map.of(), null);

        var debug = adapter.adapt(snapshot, 1);
        assertTrue(debug.flags().get("paused"));
        assertFalse(debug.flags().get("running"));
    }

    @Test
    void perSubsystemHealthMetrics() {
        var metrics = adapter.extractMetrics(healthy());
        assertEquals(1.0, metrics.get("subsystem.Audio.health"));
        assertEquals(1.0, metrics.get("subsystem.Input.health"));
        assertEquals(1.0, metrics.get("subsystem.ECS.health"));
        assertEquals(3.0, metrics.get("totalSubsystemCount"));
    }
}
