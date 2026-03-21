package org.dynamisengine.debug.bridge.adapter;

import org.dynamisengine.debug.api.DebugCategory;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WindowTelemetryAdapterTest {

    private final WindowTelemetryAdapter adapter = new WindowTelemetryAdapter();

    @Test
    void subsystemNameAndCategory() {
        assertEquals("window", adapter.subsystemName());
        assertEquals(DebugCategory.ENGINE, adapter.category());
    }

    @Test
    void adaptNormalState() {
        var snapshot = new WindowTelemetryAdapter.WindowTelemetrySnapshot(
                1280, 720, 2560, 1440, true, 3, 2, "VULKAN");
        var debug = adapter.adapt(snapshot, 1);

        assertEquals(1280.0, debug.metrics().get("windowWidth"));
        assertEquals(2560.0, debug.metrics().get("framebufferWidth"));
        assertEquals(2.0, debug.metrics().get("dpiScale"));
        assertTrue(debug.flags().get("focused"));
        assertFalse(debug.flags().get("minimized"));
        assertTrue(debug.text().contains("window=1280x720"));
        assertTrue(debug.text().contains("backend=VULKAN"));
    }

    @Test
    void detectMinimized() {
        var snapshot = new WindowTelemetryAdapter.WindowTelemetrySnapshot(
                0, 0, 0, 0, false, 5, 4, "OPENGL");
        var debug = adapter.adapt(snapshot, 1);
        assertTrue(debug.flags().get("minimized"));
        assertFalse(debug.flags().get("focused"));
    }

    @Test
    void noDpiScaleWhenZeroWidth() {
        var snapshot = new WindowTelemetryAdapter.WindowTelemetrySnapshot(
                0, 0, 0, 0, false, 0, 0, "AUTO");
        assertFalse(adapter.extractMetrics(snapshot).containsKey("dpiScale"));
    }
}
