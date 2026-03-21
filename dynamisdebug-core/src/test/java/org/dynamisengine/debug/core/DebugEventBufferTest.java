package org.dynamisengine.debug.core;

import org.dynamisengine.debug.api.DebugCategory;
import org.dynamisengine.debug.api.DebugSeverity;
import org.dynamisengine.debug.api.event.DebugEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DebugEventBufferTest {

    private DebugEvent event(String name) {
        return new DebugEvent(1, 100, "test", DebugCategory.ENGINE, DebugSeverity.INFO, name, "msg");
    }

    @Test
    void ringBufferEvictsOldest() {
        var buf = new DebugEventBuffer(3);
        buf.submit(event("a"));
        buf.submit(event("b"));
        buf.submit(event("c"));
        buf.submit(event("d"));

        assertEquals(3, buf.size());
        var drained = buf.drain();
        assertEquals("b", drained.get(0).name());
        assertEquals("d", drained.get(2).name());
    }

    @Test
    void drainClearsBuffer() {
        var buf = new DebugEventBuffer(10);
        buf.submit(event("x"));
        assertEquals(1, buf.drain().size());
        assertEquals(0, buf.drain().size());
    }

    @Test
    void recentReturnsTailElements() {
        var buf = new DebugEventBuffer(10);
        buf.submit(event("a"));
        buf.submit(event("b"));
        buf.submit(event("c"));

        var recent = buf.recent(2);
        assertEquals(2, recent.size());
        assertEquals("b", recent.get(0).name());
        assertEquals("c", recent.get(1).name());
    }

    @Test
    void recentDoesNotMutateBuffer() {
        var buf = new DebugEventBuffer(10);
        buf.submit(event("x"));
        buf.recent(1);
        assertEquals(1, buf.size());
    }
}
