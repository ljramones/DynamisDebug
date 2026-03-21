package org.dynamisengine.debug.core;

import org.dynamisengine.debug.api.*;
import org.dynamisengine.debug.api.event.DebugEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DefaultDebugQueryServiceTest {

    private DebugHistory history;
    private DebugEventBuffer eventBuffer;
    private DefaultDebugQueryService service;

    @BeforeEach
    void setUp() {
        history = new DebugHistory(100);
        eventBuffer = new DebugEventBuffer(100);
        service = new DefaultDebugQueryService(history, eventBuffer);
    }

    private DebugSnapshot snap(String source, DebugCategory cat, long frame) {
        return new DebugSnapshot(frame, frame * 16, source, cat, Map.of("val", 1.0), Map.of(), "");
    }

    private DebugEvent event(String source, DebugCategory cat, DebugSeverity sev, long frame) {
        return new DebugEvent(frame, frame * 16, source, cat, sev, "evt", "msg");
    }

    // --- snapshot queries ---

    @Test
    void querySnapshotsMatchAll() {
        history.record(1, Map.of("phys", snap("phys", DebugCategory.PHYSICS, 1)));
        history.record(2, Map.of("audio", snap("audio", DebugCategory.AUDIO, 2)));

        var results = service.querySnapshots(DebugQuery.all());
        assertEquals(2, results.size());
    }

    @Test
    void querySnapshotsByCategory() {
        history.record(1, Map.of(
                "phys", snap("phys", DebugCategory.PHYSICS, 1),
                "audio", snap("audio", DebugCategory.AUDIO, 1)
        ));

        var query = DebugQuery.builder().categories(DebugCategory.PHYSICS).build();
        var results = service.querySnapshots(query);
        assertEquals(1, results.size());
        assertEquals("phys", results.get(0).source());
    }

    @Test
    void querySnapshotsByFrameRange() {
        history.record(5, Map.of("a", snap("a", DebugCategory.ENGINE, 5)));
        history.record(10, Map.of("a", snap("a", DebugCategory.ENGINE, 10)));
        history.record(15, Map.of("a", snap("a", DebugCategory.ENGINE, 15)));

        var query = DebugQuery.builder().frameRange(8, 12).build();
        var results = service.querySnapshots(query);
        assertEquals(1, results.size());
        assertEquals(10, results.get(0).frameNumber());
    }

    // --- event queries ---

    @Test
    void queryEventsMatchAll() {
        eventBuffer.submit(event("a", DebugCategory.ENGINE, DebugSeverity.INFO, 1));
        eventBuffer.submit(event("b", DebugCategory.AUDIO, DebugSeverity.WARNING, 2));

        assertEquals(2, service.queryEvents(DebugQuery.all()).size());
    }

    @Test
    void queryEventsBySeverity() {
        eventBuffer.submit(event("a", DebugCategory.ENGINE, DebugSeverity.INFO, 1));
        eventBuffer.submit(event("b", DebugCategory.ENGINE, DebugSeverity.ERROR, 2));
        eventBuffer.submit(event("c", DebugCategory.ENGINE, DebugSeverity.CRITICAL, 3));

        var query = DebugQuery.builder().severities(DebugSeverity.ERROR, DebugSeverity.CRITICAL).build();
        var results = service.queryEvents(query);
        assertEquals(2, results.size());
    }

    @Test
    void queryEventsBySourceAndCategory() {
        eventBuffer.submit(event("phys", DebugCategory.PHYSICS, DebugSeverity.WARNING, 1));
        eventBuffer.submit(event("audio", DebugCategory.AUDIO, DebugSeverity.WARNING, 2));

        var query = DebugQuery.builder()
                .sources("phys")
                .categories(DebugCategory.PHYSICS)
                .build();
        var results = service.queryEvents(query);
        assertEquals(1, results.size());
        assertEquals("phys", results.get(0).source());
    }

    // --- snapshotsForFrame ---

    @Test
    void snapshotsForFrameReturnsCorrectFrame() {
        history.record(1, Map.of("a", snap("a", DebugCategory.ENGINE, 1)));
        history.record(2, Map.of("b", snap("b", DebugCategory.ENGINE, 2)));

        var frame = service.snapshotsForFrame(1);
        assertEquals(1, frame.size());
        assertTrue(frame.containsKey("a"));
    }

    @Test
    void snapshotsForFrameMissingReturnsEmpty() {
        assertTrue(service.snapshotsForFrame(999).isEmpty());
    }

    // --- integration with DebugSession ---

    @Test
    void sessionExposesQueryService() {
        var session = new DebugSession(64, 100);
        session.registerProvider(new DebugSnapshotProvider() {
            @Override public String debugSourceName() { return "test"; }
            @Override public DebugCategory debugCategory() { return DebugCategory.ENGINE; }
            @Override public DebugSnapshot captureSnapshot(long frameNumber) {
                return snap("test", DebugCategory.ENGINE, frameNumber);
            }
        });
        session.captureFrame(1);
        session.captureFrame(2);

        var results = session.queries().querySnapshots(DebugQuery.all());
        assertEquals(2, results.size());
    }
}
