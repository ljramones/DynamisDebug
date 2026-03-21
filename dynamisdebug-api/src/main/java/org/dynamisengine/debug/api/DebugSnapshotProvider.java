package org.dynamisengine.debug.api;

/**
 * Interface for subsystems that can produce diagnostic snapshots.
 *
 * Subsystems implement this and register with the debug system.
 * The debug system calls {@link #captureSnapshot(long)} each frame
 * or on demand.
 */
public interface DebugSnapshotProvider {

    /** The subsystem/component name. */
    String debugSourceName();

    /** The primary category for this provider's data. */
    DebugCategory debugCategory();

    /** Capture a snapshot at the given frame number. */
    DebugSnapshot captureSnapshot(long frameNumber);
}
