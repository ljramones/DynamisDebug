package org.dynamisengine.debug.bridge.adapter;

import org.dynamisengine.debug.api.DebugCategory;
import org.dynamisengine.debug.api.DebugSnapshot;
import org.dynamisengine.debug.bridge.TelemetryAdapter;
import org.dynamisengine.sky.api.state.SunState;
import org.dynamisengine.sky.api.state.TimeOfDayState;
import org.dynamisengine.sky.api.state.WeatherState;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Adapts sky system state into a unified {@link DebugSnapshot}.
 *
 * <p>Captures time-of-day, sun position/intensity, weather parameters,
 * and ambient lighting state.
 *
 * <p>Read-only consumer — no debug concepts pushed back into DynamisSky.
 */
public final class SkyTelemetryAdapter implements TelemetryAdapter<SkyTelemetryAdapter.SkyTelemetrySnapshot> {

    @Override
    public String subsystemName() { return "sky"; }

    @Override
    public DebugCategory category() { return DebugCategory.RENDERING; }

    @Override
    public DebugSnapshot adapt(SkyTelemetrySnapshot telemetry, long frameNumber) {
        Map<String, Double> metrics = extractMetrics(telemetry);

        Map<String, Boolean> flags = new LinkedHashMap<>();
        flags.put("isNight", telemetry.sun().altitudeDegrees() < 0);
        flags.put("isRaining", telemetry.weather().rainIntensity() > 0);
        flags.put("timeLocked", telemetry.timeOfDay().locked());

        String text = String.format(java.util.Locale.ROOT,
                "tod=%.1fh sun=%.0f° ambient=%.2f cloud=%.0f%% rain=%.0f%%",
                telemetry.timeOfDay().localTimeHours(),
                telemetry.sun().altitudeDegrees(),
                telemetry.timeOfDay().ambientIntensity(),
                telemetry.weather().cloudCoverage() * 100,
                telemetry.weather().rainIntensity() * 100);

        return new DebugSnapshot(frameNumber, System.currentTimeMillis(),
                subsystemName(), category(), metrics, flags, text);
    }

    @Override
    public Map<String, Double> extractMetrics(SkyTelemetrySnapshot telemetry) {
        Map<String, Double> metrics = new LinkedHashMap<>();

        // Time
        metrics.put("localTimeHours", telemetry.timeOfDay().localTimeHours());
        metrics.put("timeMultiplier", telemetry.timeOfDay().timeMultiplier());
        metrics.put("ambientIntensity", (double) telemetry.timeOfDay().ambientIntensity());
        metrics.put("colorTemperatureK", (double) telemetry.timeOfDay().colorTemperatureKelvin());

        // Sun
        metrics.put("sunAzimuthDeg", telemetry.sun().azimuthDegrees());
        metrics.put("sunAltitudeDeg", telemetry.sun().altitudeDegrees());
        metrics.put("sunIntensity", (double) telemetry.sun().intensity());

        // Weather
        metrics.put("windSpeed", (double) telemetry.weather().windSpeedMetersPerSecond());
        metrics.put("rainIntensity", (double) telemetry.weather().rainIntensity());
        metrics.put("snowIntensity", (double) telemetry.weather().snowIntensity());
        metrics.put("fogDensity", (double) telemetry.weather().fogDensity());
        metrics.put("cloudCoverage", (double) telemetry.weather().cloudCoverage());

        return metrics;
    }

    /**
     * Bundles sky state from SkyService queries.
     */
    public record SkyTelemetrySnapshot(
            TimeOfDayState timeOfDay,
            SunState sun,
            WeatherState weather
    ) {
        public SkyTelemetrySnapshot {
            if (timeOfDay == null || sun == null || weather == null)
                throw new IllegalArgumentException("All sky state must be non-null");
        }
    }
}
