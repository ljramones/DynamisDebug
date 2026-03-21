package org.dynamisengine.debug.bridge;

import org.dynamisengine.debug.core.DebugSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central integration point for subsystem telemetry adapters.
 *
 * Subsystems register their {@link TelemetryAdapter} instances here.
 * The bridge feeds adapted data into the {@link DebugSession} during
 * each frame capture.
 *
 * This is the "churn zone" where integration-specific code lives,
 * keeping the API and core modules clean.
 */
public final class DebugBridge {

    private final DebugSession session;
    private final Map<String, TelemetryAdapter<?>> adapters = new ConcurrentHashMap<>();

    public DebugBridge(DebugSession session) {
        this.session = session;
    }

    public <T> void registerAdapter(TelemetryAdapter<T> adapter) {
        adapters.put(adapter.subsystemName(), adapter);
    }

    public void unregisterAdapter(String subsystemName) {
        adapters.remove(subsystemName);
    }

    public DebugSession session() { return session; }

    public int adapterCount() { return adapters.size(); }
}
