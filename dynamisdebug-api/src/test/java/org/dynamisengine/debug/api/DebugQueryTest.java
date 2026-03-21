package org.dynamisengine.debug.api;

import org.dynamisengine.debug.api.event.DebugEvent;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DebugQueryTest {

    private static DebugSnapshot snap(String source, DebugCategory cat, long frame, long ts) {
        return new DebugSnapshot(frame, ts, source, cat, Map.of(), Map.of(), "");
    }

    private static DebugEvent event(String source, DebugCategory cat, DebugSeverity sev, long frame, long ts) {
        return new DebugEvent(frame, ts, source, cat, sev, "evt", "msg");
    }

    // --- all() ---

    @Test
    void allMatchesEverything() {
        var query = DebugQuery.all();
        assertTrue(query.matchesSnapshot(snap("a", DebugCategory.ENGINE, 1, 100)));
        assertTrue(query.matchesEvent(event("b", DebugCategory.AUDIO, DebugSeverity.ERROR, 99, 999)));
    }

    // --- category filter ---

    @Test
    void filterByCategory() {
        var query = DebugQuery.builder().categories(DebugCategory.PHYSICS).build();
        assertTrue(query.matchesSnapshot(snap("phys", DebugCategory.PHYSICS, 1, 100)));
        assertFalse(query.matchesSnapshot(snap("audio", DebugCategory.AUDIO, 1, 100)));
    }

    @Test
    void filterByMultipleCategories() {
        var query = DebugQuery.builder().categories(DebugCategory.PHYSICS, DebugCategory.AUDIO).build();
        assertTrue(query.matchesSnapshot(snap("a", DebugCategory.PHYSICS, 1, 0)));
        assertTrue(query.matchesSnapshot(snap("b", DebugCategory.AUDIO, 1, 0)));
        assertFalse(query.matchesSnapshot(snap("c", DebugCategory.RENDERING, 1, 0)));
    }

    // --- severity filter ---

    @Test
    void filterBySeverity() {
        var query = DebugQuery.builder().severities(DebugSeverity.ERROR, DebugSeverity.CRITICAL).build();
        assertTrue(query.matchesEvent(event("s", DebugCategory.ENGINE, DebugSeverity.ERROR, 1, 0)));
        assertFalse(query.matchesEvent(event("s", DebugCategory.ENGINE, DebugSeverity.INFO, 1, 0)));
    }

    // --- source filter ---

    @Test
    void filterBySource() {
        var query = DebugQuery.builder().sources("physics").build();
        assertTrue(query.matchesSnapshot(snap("physics", DebugCategory.PHYSICS, 1, 0)));
        assertFalse(query.matchesSnapshot(snap("audio", DebugCategory.AUDIO, 1, 0)));
    }

    // --- frame range ---

    @Test
    void filterByFrameRange() {
        var query = DebugQuery.builder().frameRange(10, 20).build();
        assertTrue(query.matchesSnapshot(snap("a", DebugCategory.ENGINE, 15, 0)));
        assertFalse(query.matchesSnapshot(snap("a", DebugCategory.ENGINE, 5, 0)));
        assertFalse(query.matchesSnapshot(snap("a", DebugCategory.ENGINE, 25, 0)));
    }

    // --- time range ---

    @Test
    void filterByTimeRange() {
        var query = DebugQuery.builder().timeRange(100, 200).build();
        assertTrue(query.matchesEvent(event("s", DebugCategory.ENGINE, DebugSeverity.INFO, 1, 150)));
        assertFalse(query.matchesEvent(event("s", DebugCategory.ENGINE, DebugSeverity.INFO, 1, 50)));
        assertFalse(query.matchesEvent(event("s", DebugCategory.ENGINE, DebugSeverity.INFO, 1, 250)));
    }

    // --- combined filters ---

    @Test
    void combinedFiltersAreAndSemantics() {
        var query = DebugQuery.builder()
                .categories(DebugCategory.PHYSICS)
                .sources("phys")
                .frameRange(10, 20)
                .build();

        assertTrue(query.matchesSnapshot(snap("phys", DebugCategory.PHYSICS, 15, 0)));
        assertFalse(query.matchesSnapshot(snap("phys", DebugCategory.PHYSICS, 25, 0))); // frame out
        assertFalse(query.matchesSnapshot(snap("other", DebugCategory.PHYSICS, 15, 0))); // source mismatch
        assertFalse(query.matchesSnapshot(snap("phys", DebugCategory.AUDIO, 15, 0))); // category mismatch
    }

    // --- defensive copy ---

    @Test
    void setsAreDefensivelyCopied() {
        var query = DebugQuery.builder().categories(DebugCategory.ENGINE).build();
        assertThrows(UnsupportedOperationException.class,
                () -> query.categories().add(DebugCategory.AUDIO));
    }

    // --- builder reuse ---

    @Test
    void builderProducesIndependentQueries() {
        var builder = DebugQuery.builder().categories(DebugCategory.ENGINE);
        var q1 = builder.build();
        var q2 = builder.sources("x").build();

        assertTrue(q1.sources().isEmpty());
        assertFalse(q2.sources().isEmpty());
    }
}
