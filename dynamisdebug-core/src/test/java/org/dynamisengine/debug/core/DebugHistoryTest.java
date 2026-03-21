package org.dynamisengine.debug.core;

import org.dynamisengine.debug.api.DebugCategory;
import org.dynamisengine.debug.api.DebugSnapshot;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DebugHistoryTest {

    private Map<String, DebugSnapshot> fakeFrame(long frame) {
        return Map.of("src", new DebugSnapshot(frame, 0, "src", DebugCategory.ENGINE, Map.of(), Map.of(), ""));
    }

    @Test
    void ringBufferEvictsOldest() {
        var history = new DebugHistory(3);
        history.record(1, fakeFrame(1));
        history.record(2, fakeFrame(2));
        history.record(3, fakeFrame(3));
        history.record(4, fakeFrame(4));

        assertEquals(3, history.size());
        assertEquals(4, history.latest().orElseThrow().frameNumber());

        var recent = history.recent(3);
        assertEquals(2, recent.get(0).frameNumber());
        assertEquals(4, recent.get(2).frameNumber());
    }

    @Test
    void latestOnEmptyReturnsEmpty() {
        assertTrue(new DebugHistory(10).latest().isEmpty());
    }

    @Test
    void recentReturnsChronologicalOrder() {
        var history = new DebugHistory(10);
        history.record(10, fakeFrame(10));
        history.record(20, fakeFrame(20));
        history.record(30, fakeFrame(30));

        var recent = history.recent(2);
        assertEquals(20, recent.get(0).frameNumber());
        assertEquals(30, recent.get(1).frameNumber());
    }
}
