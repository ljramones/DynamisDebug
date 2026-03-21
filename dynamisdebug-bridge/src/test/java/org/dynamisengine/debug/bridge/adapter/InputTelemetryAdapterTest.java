package org.dynamisengine.debug.bridge.adapter;

import org.dynamisengine.debug.api.DebugCategory;
import org.dynamisengine.input.api.device.InputDeviceId;
import org.dynamisengine.input.api.device.InputDeviceInfo;
import org.dynamisengine.input.api.device.PlayerId;
import org.dynamisengine.input.core.InputTelemetry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class InputTelemetryAdapterTest {

    private final InputTelemetryAdapter adapter = new InputTelemetryAdapter();

    @Test
    void subsystemNameAndCategory() {
        assertEquals("input", adapter.subsystemName());
        assertEquals(DebugCategory.INPUT, adapter.category());
    }

    @Test
    void adaptBasicState() {
        var devices = List.of(
                new InputDeviceInfo(InputDeviceId.KEYBOARD, "Keyboard", true),
                new InputDeviceInfo(InputDeviceId.MOUSE, "Mouse", true)
        );
        var telemetry = new InputTelemetry(devices, Map.of(), 500, 12, 100, 2, 5);
        var debug = adapter.adapt(telemetry, 1);

        assertEquals(2.0, debug.metrics().get("connectedDeviceCount"));
        assertEquals(500.0, debug.metrics().get("totalEventsProcessed"));
        assertEquals(12.0, debug.metrics().get("eventsThisTick"));
        assertEquals(2.0, debug.metrics().get("activeContextCount"));
        assertFalse(debug.flags().get("hasGamepads"));
        assertFalse(debug.flags().get("highEventRate"));
        assertTrue(debug.text().contains("devices=2"));
    }

    @Test
    void adaptWithGamepad() {
        var devices = List.of(
                new InputDeviceInfo(InputDeviceId.KEYBOARD, "Keyboard", true),
                new InputDeviceInfo(InputDeviceId.gamepad(0), "Xbox Controller", true)
        );
        var players = Map.of(PlayerId.PLAYER_1, List.of(InputDeviceId.gamepad(0)));
        var telemetry = new InputTelemetry(devices, players, 1000, 8, 200, 1, 3);
        var debug = adapter.adapt(telemetry, 1);

        assertTrue(debug.flags().get("hasGamepads"));
        assertEquals(1.0, debug.metrics().get("playerCount"));
    }

    @Test
    void highEventRate() {
        var telemetry = new InputTelemetry(List.of(), Map.of(), 0, 60, 0, 0, 0);
        var debug = adapter.adapt(telemetry, 1);
        assertTrue(debug.flags().get("highEventRate"));
    }
}
