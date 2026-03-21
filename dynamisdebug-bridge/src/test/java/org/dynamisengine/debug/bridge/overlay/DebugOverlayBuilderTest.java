package org.dynamisengine.debug.bridge.overlay;

import org.dynamisengine.debug.api.*;
import org.dynamisengine.debug.api.event.DebugEvent;
import org.dynamisengine.debug.core.DebugSession;
import org.dynamisengine.debug.bridge.DebugBridge;
import org.dynamisengine.debug.bridge.TelemetryAdapter;
import org.dynamisengine.ui.debug.DebugPanel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DebugOverlayBuilderTest {

    private DebugSession session;
    private DebugOverlayBuilder builder;

    @BeforeEach
    void setUp() {
        session = new DebugSession(64, 100);
        builder = new DebugOverlayBuilder(session);
    }

    private DebugSnapshot snap(String source, DebugCategory cat, Map<String, Double> metrics,
                                Map<String, Boolean> flags, String text) {
        return new DebugSnapshot(1, System.currentTimeMillis(), source, cat, metrics, flags, text);
    }

    private Map<String, DebugSnapshot> sampleFrame() {
        Map<String, DebugSnapshot> frame = new LinkedHashMap<>();
        frame.put("worldengine", snap("worldengine", DebugCategory.ENGINE,
                Map.of("tick", 100.0, "budgetPercent", 85.0, "lastTickDurationMs", 14.2,
                        "healthySubsystemCount", 5.0, "degradedSubsystemCount", 0.0, "faultedSubsystemCount", 0.0),
                Map.of(), "RUNNING | tick=100"));
        frame.put("physics", snap("physics", DebugCategory.PHYSICS,
                Map.of("bodyCount", 200.0, "stepTimeMs", 3.5),
                Map.of("hasSleepingBodies", true), "bodies=200 step=3.5ms"));
        frame.put("ecs", snap("ecs", DebugCategory.ECS,
                Map.of("entityCount", 500.0), Map.of("hasChurn", false), "entities=500"));
        frame.put("gpu", snap("gpu", DebugCategory.RENDERING,
                Map.of("throughputGbps", 1.5), Map.of("backlogPressure", false), "inflight=1 backlog=0"));
        frame.put("lightengine", snap("lightengine", DebugCategory.RENDERING,
                Map.of("fps", 60.0, "drawCalls", 500.0), Map.of("gpuBound", true), "fps=60 draws=500"));
        return frame;
    }

    // --- buildAll ---

    @Test
    void buildAllReturnsEngineSummaryFirst() {
        var panels = builder.buildAll(sampleFrame());
        assertFalse(panels.isEmpty());
        assertEquals("Engine Summary", panels.get(0).title());
    }

    @Test
    void buildAllGroupsByCategory() {
        var panels = builder.buildAll(sampleFrame());
        // Engine Summary + ENGINE + PHYSICS + ECS + RENDERING = 5 panels (no alerts)
        var titles = panels.stream().map(DebugPanel::title).toList();
        assertTrue(titles.contains("Engine Summary"));
        assertTrue(titles.contains("ENGINE"));
        assertTrue(titles.contains("PHYSICS"));
        assertTrue(titles.contains("ECS"));
        assertTrue(titles.contains("RENDERING"));
    }

    @Test
    void buildAllIncludesAlertsWhenPresent() {
        session.submit(new DebugEvent(1, 0, "physics", DebugCategory.PHYSICS,
                DebugSeverity.WARNING, "stepHigh", "Step time exceeded budget"));

        var panels = builder.buildAll(sampleFrame());
        var titles = panels.stream().map(DebugPanel::title).toList();
        assertTrue(titles.contains("Alerts"));
    }

    @Test
    void buildAllOmitsAlertsWhenEmpty() {
        var panels = builder.buildAll(sampleFrame());
        var titles = panels.stream().map(DebugPanel::title).toList();
        assertFalse(titles.contains("Alerts"));
    }

    // --- Engine Summary ---

    @Test
    void engineSummaryShowsWorldEngineMetrics() {
        var panel = builder.buildEngineSummary(sampleFrame());
        var labels = panel.rows().stream().map(DebugPanel.Row::label).toList();
        assertTrue(labels.contains("Tick"));
        assertTrue(labels.contains("Budget"));
        assertTrue(labels.contains("Sources"));
        assertTrue(labels.contains("History"));
    }

    @Test
    void engineSummaryShowsSourceCount() {
        var panel = builder.buildEngineSummary(sampleFrame());
        var sources = panel.rows().stream()
                .filter(r -> r.label().equals("Sources"))
                .findFirst().orElseThrow();
        assertEquals("5", sources.value());
    }

    // --- Category Panels ---

    @Test
    void categoryPanelShowsSourceText() {
        var snapshots = List.of(
                snap("physics", DebugCategory.PHYSICS, Map.of(), Map.of("hasSleepingBodies", true),
                        "bodies=200 step=3.5ms"));
        var panel = builder.buildCategoryPanel(DebugCategory.PHYSICS, snapshots);

        assertEquals("PHYSICS", panel.title());
        var labels = panel.rows().stream().map(DebugPanel.Row::label).toList();
        assertTrue(labels.contains("physics"));
        // Active flag should appear
        var values = panel.rows().stream().map(DebugPanel.Row::value).toList();
        assertTrue(values.contains("ACTIVE"));
    }

    @Test
    void categoryPanelShowsMultipleSources() {
        var snapshots = List.of(
                snap("gpu", DebugCategory.RENDERING, Map.of(), Map.of(), "inflight=1"),
                snap("lightengine", DebugCategory.RENDERING, Map.of(), Map.of(), "fps=60"));
        var panel = builder.buildCategoryPanel(DebugCategory.RENDERING, snapshots);

        assertEquals("RENDERING", panel.title());
        assertTrue(panel.rows().size() >= 2);
    }

    // --- Alert Panel ---

    @Test
    void alertPanelShowsRecentEvents() {
        session.submit(new DebugEvent(1, 0, "ai", DebugCategory.AI,
                DebugSeverity.ERROR, "budgetExceeded", "AI budget over 100%"));
        session.submit(new DebugEvent(2, 0, "physics", DebugCategory.PHYSICS,
                DebugSeverity.WARNING, "stepHigh", "Step time 20ms"));

        var panel = builder.buildAlertPanel();
        assertEquals("Alerts", panel.title());
        assertEquals(2, panel.rows().size());
        assertTrue(panel.rows().get(0).label().contains("[E]"));
        assertTrue(panel.rows().get(1).label().contains("[W]"));
    }

    // --- Source Detail ---

    @Test
    void sourceDetailShowsAllMetricsAndFlags() {
        var snap = snap("physics", DebugCategory.PHYSICS,
                Map.of("bodyCount", 200.0, "stepTimeMs", 3.5),
                Map.of("hasSleepingBodies", true, "stepTimeHigh", false),
                "bodies=200");
        var panel = builder.buildSourceDetailPanel(snap);

        assertEquals("physics Detail", panel.title());
        var labels = panel.rows().stream().map(DebugPanel.Row::label).toList();
        assertTrue(labels.contains("bodyCount"));
        assertTrue(labels.contains("stepTimeMs"));
        assertTrue(labels.contains("hasSleepingBodies"));
        assertTrue(labels.contains("Summary"));
    }

    // --- Timeline Panel ---

    @Test
    void timelinePanelShowsStats() {
        // Record some history
        for (int i = 1; i <= 5; i++) {
            session.history().record(i, Map.of("physics",
                    snap("physics", DebugCategory.PHYSICS,
                            Map.of("stepTimeMs", (double) i), Map.of(), "")));
        }

        var panel = builder.buildTimelinePanel("physics", "stepTimeMs", 10);
        var labels = panel.rows().stream().map(DebugPanel.Row::label).toList();
        assertTrue(labels.contains("Avg"));
        assertTrue(labels.contains("Min"));
        assertTrue(labels.contains("Max"));
        assertTrue(labels.contains("Samples"));
    }

    @Test
    void timelinePanelHandlesNoData() {
        var panel = builder.buildTimelinePanel("missing", "metric", 10);
        var values = panel.rows().stream().map(DebugPanel.Row::value).toList();
        assertTrue(values.contains("No data"));
    }

    // --- Integration: full pipeline to overlay ---

    @Test
    void fullPipelineProducesValidPanels() {
        // Simulate a real frame capture through bridge
        var bridge = new DebugBridge(session);
        bridge.registerAdapter(new StubAdapter("physics", DebugCategory.PHYSICS,
                Map.of("bodyCount", 100.0), "bodies=100"));
        bridge.registerAdapter(new StubAdapter("ecs", DebugCategory.ECS,
                Map.of("entityCount", 50.0), "entities=50"));

        bridge.submitTelemetry("physics", "raw");
        bridge.submitTelemetry("ecs", "raw");
        var frame = bridge.captureFrame(1);

        var overlayBuilder = new DebugOverlayBuilder(session);
        var panels = overlayBuilder.buildAll(frame);

        assertTrue(panels.size() >= 3); // summary + PHYSICS + ECS
        var titles = panels.stream().map(DebugPanel::title).toList();
        assertTrue(titles.contains("Engine Summary"));
        assertTrue(titles.contains("PHYSICS"));
        assertTrue(titles.contains("ECS"));

        // Every panel has rows
        for (var panel : panels) {
            assertTrue(panel.rows().size() > 0, panel.title() + " should have rows");
        }
    }

    // --- Helper ---

    private record StubAdapter(String name, DebugCategory cat,
                                Map<String, Double> metrics, String text) implements TelemetryAdapter<String> {
        @Override public String subsystemName() { return name; }
        @Override public DebugCategory category() { return cat; }
        @Override public DebugSnapshot adapt(String t, long frameNumber) {
            return new DebugSnapshot(frameNumber, System.currentTimeMillis(), name, cat, metrics, Map.of(), text);
        }
        @Override public Map<String, Double> extractMetrics(String t) { return metrics; }
    }
}
