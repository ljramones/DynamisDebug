package org.dynamisengine.debug.bridge.adapter;

import org.dynamisengine.audio.dsp.device.AudioTelemetry;
import org.dynamisengine.debug.api.DebugCategory;
import org.dynamisengine.debug.api.DebugSnapshot;
import org.dynamisengine.debug.bridge.TelemetryAdapter;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Adapts {@link AudioTelemetry} into a unified {@link DebugSnapshot}.
 *
 * <p>Captures device health, DSP budget, ring buffer state, voice counts,
 * underrun/overrun tracking, and hot-swap generation.
 *
 * <p>Read-only consumer — no debug concepts pushed back into DynamisAudio.
 */
public final class AudioTelemetryAdapter implements TelemetryAdapter<AudioTelemetryAdapter.AudioTelemetrySnapshot> {

    @Override
    public String subsystemName() { return "audio"; }

    @Override
    public DebugCategory category() { return DebugCategory.AUDIO; }

    @Override
    public DebugSnapshot adapt(AudioTelemetrySnapshot telemetry, long frameNumber) {
        Map<String, Double> metrics = extractMetrics(telemetry);

        AudioTelemetry t = telemetry.deviceTelemetry();
        Map<String, Boolean> flags = new LinkedHashMap<>();
        flags.put("hasUnderruns", t.ringUnderruns() > 0);
        flags.put("hasOverruns", t.ringOverruns() > 0);
        flags.put("dspOverBudget", t.dspBudgetPercent() > 100.0);

        String text = String.format(java.util.Locale.ROOT,
                "state=%s dsp=%.0f%% voices=%d/%d underruns=%d ring=%.0f%%",
                t.state(), t.dspBudgetPercent(),
                telemetry.physicalVoiceCount(), telemetry.voicePoolCapacity(),
                t.ringUnderruns(), t.ringFillPercent());

        return new DebugSnapshot(frameNumber, System.currentTimeMillis(),
                subsystemName(), category(), metrics, flags, text);
    }

    @Override
    public Map<String, Double> extractMetrics(AudioTelemetrySnapshot telemetry) {
        Map<String, Double> metrics = new LinkedHashMap<>();
        AudioTelemetry t = telemetry.deviceTelemetry();

        // Device & DSP
        metrics.put("dspBudgetPercent", t.dspBudgetPercent());
        metrics.put("feederAvgNanos", (double) t.feederAvgNanos());
        metrics.put("feederMaxNanos", (double) t.feederMaxNanos());
        metrics.put("callbackCount", (double) t.callbackCount());
        metrics.put("feederBlockCount", (double) t.feederBlockCount());
        metrics.put("outputLatencyMs", (double) t.outputLatencyMs());

        // Ring buffer
        metrics.put("ringFillPercent", t.ringFillPercent());
        metrics.put("ringUnderruns", (double) t.ringUnderruns());
        metrics.put("ringOverruns", (double) t.ringOverruns());
        metrics.put("ringHighWatermark", (double) t.ringHighWatermark());
        metrics.put("ringLowWatermark", (double) t.ringLowWatermark());

        // Hot-swap
        metrics.put("swapGeneration", (double) t.swapGeneration());

        // Voice management
        metrics.put("physicalVoiceCount", (double) telemetry.physicalVoiceCount());
        metrics.put("virtualVoiceCount", (double) telemetry.virtualVoiceCount());
        metrics.put("voicePoolCapacity", (double) telemetry.voicePoolCapacity());
        metrics.put("promotionsPerCycle", (double) telemetry.promotionsPerCycle());
        metrics.put("demotionsPerCycle", (double) telemetry.demotionsPerCycle());

        // Event queue
        metrics.put("droppedEventCount", (double) telemetry.droppedEventCount());

        return metrics;
    }

    /**
     * Bundles device telemetry and voice manager state.
     *
     * @param deviceTelemetry    from AudioDeviceManager.captureTelemetry()
     * @param physicalVoiceCount from VoiceManager.lastPhysicalCount
     * @param virtualVoiceCount  from VoiceManager.lastVirtualCount
     * @param voicePoolCapacity  from VoicePool.capacity()
     * @param promotionsPerCycle from VoiceManager.promotionsPerCycle
     * @param demotionsPerCycle  from VoiceManager.demotionsPerCycle
     * @param droppedEventCount  from AcousticEventQueue.droppedEventCount()
     */
    public record AudioTelemetrySnapshot(
            AudioTelemetry deviceTelemetry,
            int physicalVoiceCount,
            int virtualVoiceCount,
            int voicePoolCapacity,
            int promotionsPerCycle,
            int demotionsPerCycle,
            long droppedEventCount
    ) {
        public AudioTelemetrySnapshot {
            if (deviceTelemetry == null) throw new IllegalArgumentException("deviceTelemetry must not be null");
        }
    }
}
