package org.dynamisengine.debug.bridge;

import org.dynamisengine.core.event.EngineEvent;
import org.dynamisengine.core.event.EventListener;
import org.dynamisengine.debug.api.*;
import org.dynamisengine.debug.api.event.DebugEvent;
import org.dynamisengine.debug.bridge.event.*;
import org.dynamisengine.debug.core.DebugSession;
import org.dynamisengine.event.EventBusBuilder;
import org.dynamisengine.event.EventBus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DebugEventBusAdapterTest {

    private DebugSession session;
    private EventBus bus;
    private DebugEventBusAdapter adapter;

    @BeforeEach
    void setUp() {
        session = new DebugSession(64, 100);
        bus = EventBusBuilder.create().synchronous().build();
        adapter = new DebugEventBusAdapter(session, bus);
    }

    // --- captureFrameAndPublish ---

    @Test
    void captureFramePublishesSnapshotEvent() {
        session.registerProvider(stubProvider("physics", DebugCategory.PHYSICS));

        var captured = new ArrayList<DebugSnapshotCapturedEvent>();
        bus.subscribe(DebugSnapshotCapturedEvent.class, captured::add);

        adapter.captureFrameAndPublish(1);

        assertEquals(1, captured.size());
        assertEquals(1, captured.get(0).frameNumber());
        assertTrue(captured.get(0).snapshots().containsKey("physics"));
    }

    @Test
    void captureFrameEmptyDoesNotPublish() {
        var captured = new ArrayList<DebugSnapshotCapturedEvent>();
        bus.subscribe(DebugSnapshotCapturedEvent.class, captured::add);

        adapter.captureFrameAndPublish(1); // no providers

        assertTrue(captured.isEmpty());
    }

    @Test
    void captureFrameReturnsSnapshots() {
        session.registerProvider(stubProvider("audio", DebugCategory.AUDIO));

        var result = adapter.captureFrameAndPublish(1);
        assertTrue(result.containsKey("audio"));
    }

    // --- executeCommandAndPublish ---

    @Test
    void executeCommandPublishesEvent() {
        session.commands().register(new DebugCommand("gc", "GC", args -> DebugCommandResult.ok("done")));

        var captured = new ArrayList<DebugCommandExecutedEvent>();
        bus.subscribe(DebugCommandExecutedEvent.class, captured::add);

        var result = adapter.executeCommandAndPublish("gc");

        assertTrue(result.success());
        assertEquals(1, captured.size());
        assertEquals("gc", captured.get(0).commandName());
        assertTrue(captured.get(0).result().success());
    }

    @Test
    void executeUnknownCommandPublishesError() {
        var captured = new ArrayList<DebugCommandExecutedEvent>();
        bus.subscribe(DebugCommandExecutedEvent.class, captured::add);

        var result = adapter.executeCommandAndPublish("missing");

        assertFalse(result.success());
        assertEquals(1, captured.size());
        assertFalse(captured.get(0).result().success());
    }

    // --- toggleFlagAndPublish ---

    @Test
    void toggleFlagPublishesEvent() {
        session.setFlag("wireframe", false);

        var captured = new ArrayList<DebugFlagToggledEvent>();
        bus.subscribe(DebugFlagToggledEvent.class, captured::add);

        adapter.toggleFlagAndPublish("wireframe");

        assertEquals(1, captured.size());
        assertEquals("wireframe", captured.get(0).flagName());
        assertTrue(captured.get(0).newValue());
    }

    // --- event forwarding ---

    @Test
    void submitForwardsToEventBus() {
        var captured = new ArrayList<DebugEventForwardedEvent>();
        bus.subscribe(DebugEventForwardedEvent.class, captured::add);

        var debugEvent = new DebugEvent(1, 100, "audio", DebugCategory.AUDIO,
                DebugSeverity.WARNING, "underrun", "256 samples lost");

        adapter.submit(debugEvent);

        assertEquals(1, captured.size());
        assertEquals("underrun", captured.get(0).debugEvent().name());
    }

    @Test
    void submitAlsoStoresInSession() {
        var debugEvent = new DebugEvent(1, 100, "src", DebugCategory.ENGINE,
                DebugSeverity.INFO, "test", "msg");

        adapter.submit(debugEvent);

        assertEquals(1, session.drainEvents().size());
    }

    @Test
    void forwardingCanBeDisabled() {
        var captured = new ArrayList<DebugEventForwardedEvent>();
        bus.subscribe(DebugEventForwardedEvent.class, captured::add);

        adapter.setForwardEvents(false);
        assertFalse(adapter.isForwardingEvents());

        adapter.submit(new DebugEvent(1, 100, "s", DebugCategory.ENGINE,
                DebugSeverity.INFO, "n", "m"));

        assertTrue(captured.isEmpty());
        assertEquals(1, session.drainEvents().size()); // still stored in session
    }

    // --- accessors ---

    @Test
    void accessors() {
        assertSame(session, adapter.session());
        assertSame(bus, adapter.eventBus());
    }

    // --- event types implement EngineEvent ---

    @Test
    void allEventsImplementEngineEvent() {
        assertInstanceOf(EngineEvent.class, DebugSnapshotCapturedEvent.of(1, Map.of()));
        assertInstanceOf(EngineEvent.class, DebugCommandExecutedEvent.of("cmd", DebugCommandResult.ok("")));
        assertInstanceOf(EngineEvent.class, DebugFlagToggledEvent.of("flag", true));
        assertInstanceOf(EngineEvent.class, DebugEventForwardedEvent.of(
                new DebugEvent(1, 0, "s", DebugCategory.ENGINE, DebugSeverity.INFO, "n", "m")));
    }

    @Test
    void eventTimestampsArePositive() {
        var evt = DebugSnapshotCapturedEvent.of(1, Map.of());
        assertTrue(evt.timestamp() > 0);
    }

    // --- helpers ---

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
