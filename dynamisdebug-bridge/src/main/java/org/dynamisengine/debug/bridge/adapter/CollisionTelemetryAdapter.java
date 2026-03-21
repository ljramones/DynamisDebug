package org.dynamisengine.debug.bridge.adapter;

import org.dynamisengine.collision.debug.CollisionDebugSnapshot3D;
import org.dynamisengine.collision.events.CollisionEvent;
import org.dynamisengine.collision.events.CollisionEventType;
import org.dynamisengine.debug.api.DebugCategory;
import org.dynamisengine.debug.api.DebugSnapshot;
import org.dynamisengine.debug.bridge.TelemetryAdapter;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Adapts {@link CollisionDebugSnapshot3D} into a unified {@link DebugSnapshot}.
 *
 * <p>Extracts collider counts, contact counts by lifecycle phase
 * (ENTER/STAY/EXIT), manifold cache size, and response-enabled ratio.
 *
 * <p>This adapter is a read-only consumer of the collision system's
 * existing debug snapshot API. It does not push any debug concepts
 * back into DynamisCollision.
 *
 * @param <T> the collision item type
 */
public final class CollisionTelemetryAdapter<T> implements TelemetryAdapter<CollisionDebugSnapshot3D<T>> {

    private final int manifoldCacheSize;

    /**
     * @param manifoldCacheSize current manifold cache size, provided by caller
     *                          since CollisionDebugSnapshot3D does not carry it
     */
    public CollisionTelemetryAdapter(int manifoldCacheSize) {
        this.manifoldCacheSize = manifoldCacheSize;
    }

    public CollisionTelemetryAdapter() {
        this(0);
    }

    /**
     * Create a new adapter with an updated cache size for this frame.
     */
    public CollisionTelemetryAdapter<T> withCacheSize(int cacheSize) {
        return new CollisionTelemetryAdapter<>(cacheSize);
    }

    @Override
    public String subsystemName() { return "collision"; }

    @Override
    public DebugCategory category() { return DebugCategory.PHYSICS; }

    @Override
    public DebugSnapshot adapt(CollisionDebugSnapshot3D<T> snapshot, long frameNumber) {
        Map<String, Double> metrics = extractMetrics(snapshot);
        Map<String, Boolean> flags = Map.of(
                "hasContacts", !snapshot.contacts().isEmpty()
        );

        int total = snapshot.contacts().size();
        String text = "colliders=" + snapshot.items().size()
                + " contacts=" + total
                + " cache=" + manifoldCacheSize;

        return new DebugSnapshot(frameNumber, System.currentTimeMillis(),
                subsystemName(), category(), metrics, flags, text);
    }

    @Override
    public Map<String, Double> extractMetrics(CollisionDebugSnapshot3D<T> snapshot) {
        Map<String, Double> metrics = new LinkedHashMap<>();
        metrics.put("colliderCount", (double) snapshot.items().size());
        metrics.put("contactCount", (double) snapshot.contacts().size());
        metrics.put("manifoldCacheSize", (double) manifoldCacheSize);

        long enterCount = 0, stayCount = 0, exitCount = 0, responseCount = 0;
        for (var contact : snapshot.contacts()) {
            switch (contact.type()) {
                case ENTER -> enterCount++;
                case STAY -> stayCount++;
                case EXIT -> exitCount++;
            }
            if (contact.responseEnabled()) responseCount++;
        }

        metrics.put("contactEnterCount", (double) enterCount);
        metrics.put("contactStayCount", (double) stayCount);
        metrics.put("contactExitCount", (double) exitCount);
        metrics.put("responseEnabledCount", (double) responseCount);

        return metrics;
    }
}
