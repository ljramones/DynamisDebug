package org.dynamisengine.debug.bridge.adapter;

import org.dynamisengine.animis.runtime.api.AnimatorInstance;
import org.dynamisengine.animis.runtime.api.RootMotionDelta;
import org.dynamisengine.debug.api.DebugCategory;
import org.dynamisengine.debug.api.DebugSnapshot;
import org.dynamisengine.debug.bridge.TelemetryAdapter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Adapts Animis animation state into a unified {@link DebugSnapshot}.
 *
 * <p>Consumes {@link AnimisTelemetrySnapshot} which bundles the publicly
 * accessible state from an animator instance. The snapshot uses only
 * plain values — no internal Animis types that aren't exported.
 *
 * <p>Captures: current state, active transition (from/to/progress),
 * root motion delta, joint count, and fired event names.
 *
 * <p>This adapter is a read-only consumer of Animis's public API.
 * It does not push any debug concepts back into Animis.
 */
public final class AnimisTelemetryAdapter implements TelemetryAdapter<AnimisTelemetryAdapter.AnimisTelemetrySnapshot> {

    @Override
    public String subsystemName() { return "animation"; }

    @Override
    public DebugCategory category() { return DebugCategory.ENGINE; }

    @Override
    public DebugSnapshot adapt(AnimisTelemetrySnapshot telemetry, long frameNumber) {
        Map<String, Double> metrics = extractMetrics(telemetry);

        Map<String, Boolean> flags = new LinkedHashMap<>();
        flags.put("inTransition", telemetry.transitionTarget() != null);
        flags.put("hasRootMotion", telemetry.rootMotion() != null
                && telemetry.rootMotion() != RootMotionDelta.ZERO);

        StringBuilder text = new StringBuilder();
        text.append("state=").append(telemetry.currentStateName());
        if (telemetry.transitionTarget() != null) {
            text.append(" -> ").append(telemetry.transitionTarget());
            if (telemetry.transitionBlendSeconds() > 0) {
                float progress = Math.min(1f,
                        telemetry.transitionElapsedSeconds() / telemetry.transitionBlendSeconds());
                text.append(String.format(java.util.Locale.ROOT, " (%.0f%%)", progress * 100));
            }
        }
        if (!telemetry.firedEvents().isEmpty()) {
            text.append(" events=").append(telemetry.firedEvents());
        }

        return new DebugSnapshot(frameNumber, System.currentTimeMillis(),
                subsystemName(), category(), metrics, flags, text.toString());
    }

    @Override
    public Map<String, Double> extractMetrics(AnimisTelemetrySnapshot telemetry) {
        Map<String, Double> metrics = new LinkedHashMap<>();
        metrics.put("jointCount", (double) telemetry.jointCount());
        metrics.put("firedEventCount", (double) telemetry.firedEvents().size());

        if (telemetry.transitionTarget() != null && telemetry.transitionBlendSeconds() > 0) {
            float progress = Math.min(1f,
                    telemetry.transitionElapsedSeconds() / telemetry.transitionBlendSeconds());
            metrics.put("transitionProgress", (double) progress);
            metrics.put("transitionBlendSeconds", (double) telemetry.transitionBlendSeconds());
        }

        RootMotionDelta rm = telemetry.rootMotion();
        if (rm != null) {
            metrics.put("rootMotion.dx", (double) rm.dx());
            metrics.put("rootMotion.dy", (double) rm.dy());
            metrics.put("rootMotion.dz", (double) rm.dz());
            metrics.put("rootMotion.dyaw", (double) rm.dyaw());
        }

        return metrics;
    }

    /**
     * Point-in-time animation state for telemetry extraction.
     *
     * <p>Uses only plain values and exported Animis types. Callers construct
     * this from an {@link AnimatorInstance} at frame boundaries.
     *
     * @param currentStateName          active state machine state name
     * @param transitionTarget          target state name if transitioning, null if stable
     * @param transitionElapsedSeconds  elapsed transition time
     * @param transitionBlendSeconds    total transition blend time
     * @param rootMotion                root motion delta this frame
     * @param jointCount                skeleton joint count
     * @param firedEvents               animation events fired this frame
     */
    public record AnimisTelemetrySnapshot(
            String currentStateName,
            String transitionTarget,
            float transitionElapsedSeconds,
            float transitionBlendSeconds,
            RootMotionDelta rootMotion,
            int jointCount,
            List<String> firedEvents
    ) {
        public AnimisTelemetrySnapshot {
            firedEvents = firedEvents != null ? List.copyOf(firedEvents) : List.of();
        }

        /** Convenience factory for stable state (no transition). */
        public static AnimisTelemetrySnapshot stable(
                String stateName, RootMotionDelta rootMotion,
                int jointCount, List<String> firedEvents
        ) {
            return new AnimisTelemetrySnapshot(stateName, null, 0, 0, rootMotion, jointCount, firedEvents);
        }

        /** Convenience factory for transitioning state. */
        public static AnimisTelemetrySnapshot transitioning(
                String fromState, String toState,
                float elapsedSeconds, float blendSeconds,
                RootMotionDelta rootMotion,
                int jointCount, List<String> firedEvents
        ) {
            return new AnimisTelemetrySnapshot(fromState, toState, elapsedSeconds, blendSeconds,
                    rootMotion, jointCount, firedEvents);
        }
    }
}
