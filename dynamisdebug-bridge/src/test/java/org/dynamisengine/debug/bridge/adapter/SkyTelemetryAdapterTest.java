package org.dynamisengine.debug.bridge.adapter;

import org.dynamisengine.debug.api.DebugCategory;
import org.dynamisengine.sky.api.state.SunState;
import org.dynamisengine.sky.api.state.TimeOfDayState;
import org.dynamisengine.sky.api.state.WeatherState;
import org.dynamisengine.sky.api.Vec3;
import org.dynamisengine.sky.api.ColorRgb;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SkyTelemetryAdapterTest {

    private final SkyTelemetryAdapter adapter = new SkyTelemetryAdapter();

    @Test
    void subsystemNameAndCategory() {
        assertEquals("sky", adapter.subsystemName());
        assertEquals(DebugCategory.RENDERING, adapter.category());
    }

    @Test
    void adaptNoonState() {
        var tod = new TimeOfDayState(2460000.0, 12.0, 1.0, false, 0.95f, 5500);
        var sun = new SunState(new Vec3(0, 1, 0), new ColorRgb(1, 0.95f, 0.9f), 1.0f, 180.0, 75.0);
        var weather = WeatherState.CLEAR;

        var snapshot = new SkyTelemetryAdapter.SkyTelemetrySnapshot(tod, sun, weather);
        var debug = adapter.adapt(snapshot, 1);

        assertEquals(12.0, debug.metrics().get("localTimeHours"));
        assertEquals(75.0, debug.metrics().get("sunAltitudeDeg"));
        assertEquals(0.95, debug.metrics().get("ambientIntensity"), 0.01);
        assertFalse(debug.flags().get("isNight"));
        assertFalse(debug.flags().get("isRaining"));
        assertFalse(debug.flags().get("timeLocked"));
        assertTrue(debug.text().contains("tod=12.0h"));
    }

    @Test
    void adaptNightState() {
        var tod = new TimeOfDayState(2460000.5, 2.0, 1.0, false, 0.05f, 3000);
        var sun = new SunState(new Vec3(0, -0.5f, 0.87f), new ColorRgb(0, 0, 0), 0.0f, 0, -30.0);
        var weather = WeatherState.CLEAR;

        var snapshot = new SkyTelemetryAdapter.SkyTelemetrySnapshot(tod, sun, weather);
        var debug = adapter.adapt(snapshot, 1);

        assertTrue(debug.flags().get("isNight"));
    }

    @Test
    void adaptRainyWeather() {
        var tod = new TimeOfDayState(2460000.0, 14.0, 1.0, false, 0.5f, 5000);
        var sun = new SunState(new Vec3(0, 0.5f, 0.87f), new ColorRgb(0.5f, 0.5f, 0.5f), 0.5f, 200, 30.0);
        var weather = WeatherState.HEAVY_RAIN;

        var snapshot = new SkyTelemetryAdapter.SkyTelemetrySnapshot(tod, sun, weather);
        var debug = adapter.adapt(snapshot, 1);

        assertTrue(debug.flags().get("isRaining"));
        assertTrue(debug.metrics().get("rainIntensity") > 0);
        assertTrue(debug.metrics().get("cloudCoverage") > 0);
    }

    @Test
    void timeLocked() {
        var tod = new TimeOfDayState(2460000.0, 6.0, 0.0, true, 0.3f, 4000);
        var sun = new SunState(new Vec3(1, 0.1f, 0), new ColorRgb(1, 0.5f, 0.2f), 0.3f, 90, 5.0);
        var weather = WeatherState.CLEAR;

        var snapshot = new SkyTelemetryAdapter.SkyTelemetrySnapshot(tod, sun, weather);
        var debug = adapter.adapt(snapshot, 1);

        assertTrue(debug.flags().get("timeLocked"));
    }
}
