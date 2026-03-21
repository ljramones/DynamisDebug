package org.dynamisengine.debug.bridge;

import org.dynamisengine.debug.api.DebugSnapshot;
import org.dynamisengine.debug.core.DebugSession;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central integration point for subsystem telemetry adapters.
 *
 * <p>Subsystems register their {@link TelemetryAdapter} instances here
 * and push raw telemetry each frame via {@link #submitTelemetry}.
 * The bridge adapts and merges that telemetry with provider snapshots
 * during {@link #captureFrame}.
 *
 * <p>This is the "churn zone" where integration-specific code lives,
 * keeping the API and core modules clean.
 *
 * <h3>Frame lifecycle</h3>
 * <ol>
 *   <li>Subsystems call {@code submitTelemetry(name, data)} as they produce telemetry</li>
 *   <li>Orchestrator calls {@code captureFrame(frameNumber)} once per frame</li>
 *   <li>Bridge adapts buffered telemetry, merges with session provider snapshots,
 *       stores everything in history, and clears the buffer</li>
 * </ol>
 */
public final class DebugBridge {

    private final DebugSession session;
    private final Map<String, TelemetryAdapter<?>> adapters = new ConcurrentHashMap<>();
    private final Map<String, Object> pendingTelemetry = new ConcurrentHashMap<>();

    public DebugBridge(DebugSession session) {
        this.session = session;
    }

    // --- Adapter registration ---

    public <T> void registerAdapter(TelemetryAdapter<T> adapter) {
        adapters.put(adapter.subsystemName(), adapter);
    }

    public void unregisterAdapter(String subsystemName) {
        adapters.remove(subsystemName);
        pendingTelemetry.remove(subsystemName);
    }

    // --- Telemetry submission (push model) ---

    /**
     * Submit raw telemetry from a subsystem. The data is buffered until
     * the next {@link #captureFrame} call, when it will be adapted and
     * merged into the debug session's history.
     *
     * @param subsystemName the adapter name (must match a registered adapter)
     * @param telemetry     the raw subsystem telemetry object
     * @param <T>           the telemetry type
     */
    public <T> void submitTelemetry(String subsystemName, T telemetry) {
        if (adapters.containsKey(subsystemName)) {
            pendingTelemetry.put(subsystemName, telemetry);
        }
    }

    // --- Frame capture ---

    /**
     * Capture a complete debug frame. This:
     * <ol>
     *   <li>Captures snapshots from all registered {@link org.dynamisengine.debug.api.DebugSnapshotProvider}s
     *       on the session</li>
     *   <li>Adapts all buffered telemetry through registered adapters</li>
     *   <li>Merges both into a single frame map</li>
     *   <li>Stores the merged frame in history</li>
     *   <li>Clears the telemetry buffer</li>
     * </ol>
     *
     * @param frameNumber the current frame number
     * @return the merged snapshot map for this frame
     */
    public Map<String, DebugSnapshot> captureFrame(long frameNumber) {
        if (!session.isEnabled()) return Map.of();

        // 1. Capture from session providers (pull model, no history write yet)
        Map<String, DebugSnapshot> frame = new LinkedHashMap<>(session.captureProviders(frameNumber));

        // 2. Adapt buffered telemetry (push model)
        for (var entry : pendingTelemetry.entrySet()) {
            String name = entry.getKey();
            Object telemetry = entry.getValue();
            TelemetryAdapter<?> adapter = adapters.get(name);
            if (adapter != null) {
                try {
                    DebugSnapshot snapshot = adaptUnchecked(adapter, telemetry, frameNumber);
                    frame.put(name, snapshot);
                } catch (Exception e) {
                    // Don't let adapter failure crash the engine
                }
            }
        }
        pendingTelemetry.clear();

        // 3. Record the merged frame in history (single write)
        session.history().record(frameNumber, frame);

        // 4. Evaluate watchdog rules against the merged frame
        session.watchdog().evaluate(frameNumber, frame);

        return frame;
    }

    // --- Accessors ---

    public DebugSession session() { return session; }

    public int adapterCount() { return adapters.size(); }

    public int pendingTelemetryCount() { return pendingTelemetry.size(); }

    // --- Internal ---

    @SuppressWarnings("unchecked")
    private static <T> DebugSnapshot adaptUnchecked(TelemetryAdapter<T> adapter, Object telemetry, long frameNumber) {
        return adapter.adapt((T) telemetry, frameNumber);
    }
}
