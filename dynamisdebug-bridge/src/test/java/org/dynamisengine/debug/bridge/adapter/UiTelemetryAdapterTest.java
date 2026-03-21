package org.dynamisengine.debug.bridge.adapter;

import org.dynamisengine.debug.api.DebugCategory;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UiTelemetryAdapterTest {

    private final UiTelemetryAdapter adapter = new UiTelemetryAdapter();

    @Test
    void subsystemNameAndCategory() {
        assertEquals("ui", adapter.subsystemName());
        assertEquals(DebugCategory.UI, adapter.category());
    }

    @Test
    void adaptActiveUi() {
        var snapshot = new UiTelemetryAdapter.UiTelemetrySnapshot(
                3, 25, 20, 60.0f, 16.5f, true, 4);
        var debug = adapter.adapt(snapshot, 1);

        assertEquals(3.0, debug.metrics().get("layerCount"));
        assertEquals(25.0, debug.metrics().get("widgetCount"));
        assertEquals(20.0, debug.metrics().get("visibleWidgetCount"));
        assertEquals(60.0, debug.metrics().get("fps"));
        assertTrue(debug.flags().get("debugOverlayEnabled"));
        assertTrue(debug.flags().get("hasWidgets"));
        assertTrue(debug.text().contains("overlay=on"));
    }

    @Test
    void adaptEmptyUi() {
        var snapshot = new UiTelemetryAdapter.UiTelemetrySnapshot(
                0, 0, 0, 0, 0, false, 0);
        var debug = adapter.adapt(snapshot, 1);
        assertFalse(debug.flags().get("hasWidgets"));
        assertFalse(debug.flags().get("debugOverlayEnabled"));
        assertTrue(debug.text().contains("overlay=off"));
    }
}
