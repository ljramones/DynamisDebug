package org.dynamisengine.debug.bridge.adapter;

import org.dynamisengine.debug.api.DebugCategory;
import org.dynamisengine.debug.api.DebugSnapshot;
import org.dynamisengine.debug.bridge.TelemetryAdapter;
import org.dynamisengine.ecs.api.world.World;
import org.dynamisengine.ecs.api.world.WorldDelta;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Adapts ECS {@link World} state into a unified {@link DebugSnapshot}.
 *
 * <p>Extracts entity count, per-tick create/destroy churn, and tick number
 * from the public {@link World} and {@link WorldDelta} APIs.
 *
 * <p>Read-only consumer — no debug concepts pushed back into DynamisECS.
 */
public final class EcsTelemetryAdapter implements TelemetryAdapter<EcsTelemetryAdapter.EcsTelemetrySnapshot> {

    @Override
    public String subsystemName() { return "ecs"; }

    @Override
    public DebugCategory category() { return DebugCategory.ECS; }

    @Override
    public DebugSnapshot adapt(EcsTelemetrySnapshot telemetry, long frameNumber) {
        Map<String, Double> metrics = extractMetrics(telemetry);

        Map<String, Boolean> flags = Map.of(
                "hasChurn", telemetry.createdCount() > 0 || telemetry.destroyedCount() > 0
        );

        String text = "entities=" + telemetry.entityCount()
                + " created=" + telemetry.createdCount()
                + " destroyed=" + telemetry.destroyedCount()
                + " tick=" + telemetry.tickNumber();

        return new DebugSnapshot(frameNumber, System.currentTimeMillis(),
                subsystemName(), category(), metrics, flags, text);
    }

    @Override
    public Map<String, Double> extractMetrics(EcsTelemetrySnapshot telemetry) {
        Map<String, Double> metrics = new LinkedHashMap<>();
        metrics.put("entityCount", (double) telemetry.entityCount());
        metrics.put("createdCount", (double) telemetry.createdCount());
        metrics.put("destroyedCount", (double) telemetry.destroyedCount());
        metrics.put("tickNumber", (double) telemetry.tickNumber());
        return metrics;
    }

    /**
     * Point-in-time ECS state for telemetry extraction.
     *
     * <p>Callers construct this from {@link World} at frame boundaries.
     *
     * @param entityCount    current live entity count
     * @param createdCount   entities created this tick
     * @param destroyedCount entities destroyed this tick
     * @param tickNumber     current tick number
     */
    public record EcsTelemetrySnapshot(
            int entityCount,
            int createdCount,
            int destroyedCount,
            long tickNumber
    ) {
        /** Build from a World and its delta. */
        public static EcsTelemetrySnapshot from(World world) {
            WorldDelta delta = world.delta();
            return new EcsTelemetrySnapshot(
                    world.entities().size(),
                    delta.createdEntities().size(),
                    delta.destroyedEntities().size(),
                    delta.tick().tick()
            );
        }
    }
}
