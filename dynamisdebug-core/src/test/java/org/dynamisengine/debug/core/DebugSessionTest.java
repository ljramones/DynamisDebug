package org.dynamisengine.debug.core;

import org.dynamisengine.debug.api.*;
import org.dynamisengine.debug.api.event.DebugEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DebugSessionTest {

    private DebugSession session;

    @BeforeEach
    void setUp() {
        session = new DebugSession(64, 100);
    }

    // --- Providers & frame capture ---

    @Test
    void captureFrameCollectsFromRegisteredProviders() {
        session.registerProvider(stubProvider("physics", DebugCategory.PHYSICS));
        session.registerProvider(stubProvider("audio", DebugCategory.AUDIO));

        var frame = session.captureFrame(1);
        assertEquals(2, frame.size());
        assertTrue(frame.containsKey("physics"));
        assertTrue(frame.containsKey("audio"));
    }

    @Test
    void captureFrameStoresInHistory() {
        session.registerProvider(stubProvider("ecs", DebugCategory.ECS));
        session.captureFrame(1);
        session.captureFrame(2);
        assertEquals(2, session.history().size());
        assertEquals(2, session.history().latest().orElseThrow().frameNumber());
    }

    @Test
    void captureFrameToleratesProviderException() {
        session.registerProvider(new DebugSnapshotProvider() {
            @Override public String debugSourceName() { return "broken"; }
            @Override public DebugCategory debugCategory() { return DebugCategory.ENGINE; }
            @Override public DebugSnapshot captureSnapshot(long frameNumber) {
                throw new RuntimeException("boom");
            }
        });
        session.registerProvider(stubProvider("good", DebugCategory.ENGINE));

        var frame = session.captureFrame(1);
        assertTrue(frame.containsKey("good"));
        assertFalse(frame.containsKey("broken"));
    }

    @Test
    void unregisterProviderRemovesIt() {
        session.registerProvider(stubProvider("a", DebugCategory.ENGINE));
        session.unregisterProvider("a");
        assertTrue(session.captureFrame(1).isEmpty());
    }

    @Test
    void disabledSessionReturnsEmptyFrame() {
        session.registerProvider(stubProvider("x", DebugCategory.ENGINE));
        session.setEnabled(false);
        assertTrue(session.captureFrame(1).isEmpty());
        assertFalse(session.isEnabled());
    }

    // --- Events ---

    @Test
    void submitAndDrainEvents() {
        var event = new DebugEvent(1, 100, "src", DebugCategory.AUDIO, DebugSeverity.WARNING, "underrun", "msg");
        session.submit(event);
        session.submit(event);

        var drained = session.drainEvents();
        assertEquals(2, drained.size());
        assertEquals(0, session.drainEvents().size()); // buffer cleared
    }

    @Test
    void recentEventsDoesNotDrain() {
        var event = new DebugEvent(1, 100, "src", DebugCategory.ENGINE, DebugSeverity.INFO, "tick", "ok");
        session.submit(event);
        session.submit(event);
        session.submit(event);

        assertEquals(2, session.recentEvents(2).size());
        assertEquals(3, session.recentEvents(10).size()); // still there
    }

    @Test
    void disabledSessionDropsEvents() {
        session.setEnabled(false);
        session.submit(new DebugEvent(1, 100, "s", DebugCategory.ENGINE, DebugSeverity.INFO, "n", "m"));
        assertEquals(0, session.drainEvents().size());
    }

    // --- Counters ---

    @Test
    void counterSetAndIncrement() {
        session.setCounter("draws", DebugCategory.RENDERING, 0);
        session.incrementCounter("draws");
        session.incrementCounter("draws");
        assertEquals(2, session.counter("draws").orElseThrow().value());
    }

    @Test
    void counterAbsentReturnsEmpty() {
        assertTrue(session.counter("nonexistent").isEmpty());
    }

    @Test
    void incrementNonexistentCounterIsNoOp() {
        session.incrementCounter("ghost"); // should not throw
        assertTrue(session.counter("ghost").isEmpty());
    }

    @Test
    void allCounters() {
        session.setCounter("a", DebugCategory.ENGINE, 1);
        session.setCounter("b", DebugCategory.AUDIO, 2);
        assertEquals(2, session.allCounters().size());
    }

    // --- Flags ---

    @Test
    void flagSetAndToggle() {
        session.setFlag("wireframe", false);
        assertFalse(session.flag("wireframe"));
        session.toggleFlag("wireframe");
        assertTrue(session.flag("wireframe"));
    }

    @Test
    void flagAbsentReturnsFalse() {
        assertFalse(session.flag("missing"));
    }

    @Test
    void allFlags() {
        session.setFlag("a", true);
        session.setFlag("b", false);
        assertEquals(2, session.allFlags().size());
    }

    // --- Commands ---

    @Test
    void commandRegistryAccessible() {
        assertNotNull(session.commands());
    }

    // --- Helpers ---

    private static DebugSnapshotProvider stubProvider(String name, DebugCategory category) {
        return new DebugSnapshotProvider() {
            @Override public String debugSourceName() { return name; }
            @Override public DebugCategory debugCategory() { return category; }
            @Override public DebugSnapshot captureSnapshot(long frameNumber) {
                return new DebugSnapshot(frameNumber, System.currentTimeMillis(), name, category,
                        Map.of("val", 1.0), Map.of(), "ok");
            }
        };
    }
}
