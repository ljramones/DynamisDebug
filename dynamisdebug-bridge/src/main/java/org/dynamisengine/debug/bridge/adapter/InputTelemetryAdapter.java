package org.dynamisengine.debug.bridge.adapter;

import org.dynamisengine.debug.api.DebugCategory;
import org.dynamisengine.debug.api.DebugSnapshot;
import org.dynamisengine.debug.bridge.TelemetryAdapter;
import org.dynamisengine.input.core.InputTelemetry;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Adapts {@link InputTelemetry} into a unified {@link DebugSnapshot}.
 *
 * <p>Captures device counts, event rates, context stack depth, player
 * assignments, and device lifecycle events.
 *
 * <p>Read-only consumer — no debug concepts pushed back into DynamisInput.
 */
public final class InputTelemetryAdapter implements TelemetryAdapter<InputTelemetry> {

    @Override
    public String subsystemName() { return "input"; }

    @Override
    public DebugCategory category() { return DebugCategory.INPUT; }

    @Override
    public DebugSnapshot adapt(InputTelemetry t, long frameNumber) {
        Map<String, Double> metrics = extractMetrics(t);

        int gamepadCount = (int) t.connectedDevices().stream()
                .filter(d -> d.id().type() == org.dynamisengine.input.api.device.InputDeviceType.GAMEPAD)
                .count();

        Map<String, Boolean> flags = Map.of(
                "hasGamepads", gamepadCount > 0,
                "highEventRate", t.eventsThisTick() > 50
        );

        String text = String.format(java.util.Locale.ROOT,
                "devices=%d (gamepads=%d) events=%d/tick total=%d contexts=%d",
                t.connectedDevices().size(), gamepadCount,
                t.eventsThisTick(), t.totalEventsProcessed(), t.activeContextCount());

        return new DebugSnapshot(frameNumber, System.currentTimeMillis(),
                subsystemName(), category(), metrics, flags, text);
    }

    @Override
    public Map<String, Double> extractMetrics(InputTelemetry t) {
        Map<String, Double> metrics = new LinkedHashMap<>();
        metrics.put("connectedDeviceCount", (double) t.connectedDevices().size());
        metrics.put("totalEventsProcessed", (double) t.totalEventsProcessed());
        metrics.put("eventsThisTick", (double) t.eventsThisTick());
        metrics.put("totalSnapshots", (double) t.totalSnapshots());
        metrics.put("activeContextCount", (double) t.activeContextCount());
        metrics.put("deviceEventCount", (double) t.deviceEventCount());
        metrics.put("playerCount", (double) t.playerAssignments().size());
        return metrics;
    }
}
