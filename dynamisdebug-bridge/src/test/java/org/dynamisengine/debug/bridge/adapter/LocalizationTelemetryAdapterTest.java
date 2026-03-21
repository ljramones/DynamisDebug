package org.dynamisengine.debug.bridge.adapter;

import org.dynamisengine.debug.api.DebugCategory;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LocalizationTelemetryAdapterTest {

    private final LocalizationTelemetryAdapter adapter = new LocalizationTelemetryAdapter();

    @Test
    void subsystemNameAndCategory() {
        assertEquals("localization", adapter.subsystemName());
        assertEquals(DebugCategory.UI, adapter.category());
    }

    @Test
    void adaptHealthyState() {
        var snapshot = new LocalizationTelemetryAdapter.LocalizationTelemetrySnapshot(
                "en-US", 3, 0, 0, 0, 1);
        var debug = adapter.adapt(snapshot, 1);

        assertEquals(3.0, debug.metrics().get("namespaceCount"));
        assertEquals(0.0, debug.metrics().get("missingKeyCount"));
        assertEquals(1.0, debug.metrics().get("localeSwitchCount"));
        assertFalse(debug.flags().get("hasMissingKeys"));
        assertFalse(debug.flags().get("hasFallbacks"));
        assertTrue(debug.text().contains("locale=en-US"));
    }

    @Test
    void adaptWithMissingKeys() {
        var snapshot = new LocalizationTelemetryAdapter.LocalizationTelemetrySnapshot(
                "fr-FR", 2, 5, 3, 1, 2);
        var debug = adapter.adapt(snapshot, 1);

        assertTrue(debug.flags().get("hasMissingKeys"));
        assertTrue(debug.flags().get("hasFallbacks"));
        assertEquals(5.0, debug.metrics().get("missingKeyCount"));
        assertEquals(3.0, debug.metrics().get("fallbackHitCount"));
        assertEquals(1.0, debug.metrics().get("bundleLoadFailures"));
    }
}
