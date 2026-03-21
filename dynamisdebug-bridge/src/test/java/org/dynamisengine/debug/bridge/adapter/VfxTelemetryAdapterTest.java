package org.dynamisengine.debug.bridge.adapter;

import org.dynamisengine.debug.api.DebugCategory;
import org.dynamisengine.vfx.api.VfxBudgetStats;
import org.dynamisengine.vfx.api.VfxStats;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class VfxTelemetryAdapterTest {

    private final VfxTelemetryAdapter adapter = new VfxTelemetryAdapter();

    @Test
    void subsystemNameAndCategory() {
        assertEquals("vfx", adapter.subsystemName());
        assertEquals(DebugCategory.RENDERING, adapter.category());
    }

    @Test
    void adaptFullStats() {
        var budget = new VfxBudgetStats(1048576, 50000, 998576, 5, 0, 0, 0);
        var stats = new VfxStats(5, 50000, 0, 2000, 4096000, budget);
        var debug = adapter.adapt(stats, 1);

        assertEquals(5.0, debug.metrics().get("activeEffectCount"));
        assertEquals(50000.0, debug.metrics().get("activeParticleCount"));
        assertEquals(1048576.0, debug.metrics().get("budget.totalBudget"));
        assertEquals(50000.0, debug.metrics().get("budget.usedBudget"));
        assertFalse(debug.flags().get("budgetExhausted"));
        assertFalse(debug.flags().get("hasRejections"));
        assertTrue(debug.text().contains("effects=5"));
    }

    @Test
    void budgetExhausted() {
        var budget = new VfxBudgetStats(1000, 1000, 0, 3, 2, 0, 0);
        var stats = new VfxStats(3, 1000, 0, 0, 2048, budget);
        var debug = adapter.adapt(stats, 1);

        assertTrue(debug.flags().get("budgetExhausted"));
        assertTrue(debug.flags().get("hasRejections"));
    }

    @Test
    void nullBudgetHandled() {
        var stats = new VfxStats(1, 100, 0, 0, 512, null);
        var debug = adapter.adapt(stats, 1);
        assertFalse(debug.metrics().containsKey("budget.totalBudget"));
    }
}
