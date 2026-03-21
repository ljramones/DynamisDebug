package org.dynamisengine.debug.bridge;

import org.dynamisengine.debug.api.DebugCategory;
import org.dynamisengine.debug.api.DebugSnapshot;

import java.util.Map;

/**
 * Adapter interface for converting subsystem-specific telemetry
 * into unified {@link DebugSnapshot} format.
 *
 * Each subsystem that produces telemetry should have a corresponding
 * adapter registered with the bridge. The bridge calls these adapters
 * during frame capture to normalize heterogeneous telemetry data.
 *
 * @param <T> the subsystem-specific telemetry type
 */
public interface TelemetryAdapter<T> {

    /** The subsystem name this adapter handles. */
    String subsystemName();

    /** The debug category for this subsystem's data. */
    DebugCategory category();

    /** Convert subsystem telemetry to a unified debug snapshot. */
    DebugSnapshot adapt(T telemetry, long frameNumber);

    /** Extract numeric metrics from the telemetry. */
    Map<String, Double> extractMetrics(T telemetry);
}
