package org.dynamisengine.debug.api.event;

/**
 * Receives debug events from subsystems.
 * Implementations buffer, filter, or forward events.
 */
public interface DebugEventSink {

    void submit(DebugEvent event);
}
