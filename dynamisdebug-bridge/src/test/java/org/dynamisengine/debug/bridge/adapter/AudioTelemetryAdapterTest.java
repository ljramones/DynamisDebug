package org.dynamisengine.debug.bridge.adapter;

import org.dynamisengine.audio.dsp.device.AudioTelemetry;
import org.dynamisengine.debug.api.DebugCategory;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AudioTelemetryAdapterTest {

    private final AudioTelemetryAdapter adapter = new AudioTelemetryAdapter();

    private AudioTelemetry sampleTelemetry() {
        return new AudioTelemetry(
                org.dynamisengine.audio.dsp.device.AudioDeviceManager.State.RUNNING,
                "CoreAudio", "Built-in Output",
                new org.dynamisengine.audio.api.device.AudioFormat(48000, 2, 256, false),
                5.3f, 0, 10000, 5000, 800, 2000, 0.5f,
                3, 4, 4, 2, 0, 0);
    }

    @Test
    void subsystemNameAndCategory() {
        assertEquals("audio", adapter.subsystemName());
        assertEquals(DebugCategory.AUDIO, adapter.category());
    }

    @Test
    void adaptHealthyState() {
        var snapshot = new AudioTelemetryAdapter.AudioTelemetrySnapshot(
                sampleTelemetry(), 32, 10, 64, 2, 1, 0);
        var debug = adapter.adapt(snapshot, 1);

        assertEquals(32.0, debug.metrics().get("physicalVoiceCount"));
        assertEquals(10.0, debug.metrics().get("virtualVoiceCount"));
        assertEquals(64.0, debug.metrics().get("voicePoolCapacity"));
        assertEquals(0.0, debug.metrics().get("ringUnderruns"));
        assertFalse(debug.flags().get("hasUnderruns"));
        assertFalse(debug.flags().get("dspOverBudget"));
        assertTrue(debug.text().contains("voices=32/64"));
    }

    @Test
    void adaptWithUnderruns() {
        var telemetry = new AudioTelemetry(
                org.dynamisengine.audio.dsp.device.AudioDeviceManager.State.RUNNING,
                "CoreAudio", "Built-in Output",
                new org.dynamisengine.audio.api.device.AudioFormat(48000, 2, 256, false),
                5.3f, 0, 10000, 5000, 800, 2000, 0.5f,
                3, 4, 4, 2, 5, 0);
        var snapshot = new AudioTelemetryAdapter.AudioTelemetrySnapshot(
                telemetry, 32, 10, 64, 0, 0, 0);
        var debug = adapter.adapt(snapshot, 1);

        assertTrue(debug.flags().get("hasUnderruns"));
        assertEquals(5.0, debug.metrics().get("ringUnderruns"));
    }

    @Test
    void droppedEventCount() {
        var snapshot = new AudioTelemetryAdapter.AudioTelemetrySnapshot(
                sampleTelemetry(), 10, 5, 64, 0, 0, 42);
        assertEquals(42.0, adapter.extractMetrics(snapshot).get("droppedEventCount"));
    }
}
