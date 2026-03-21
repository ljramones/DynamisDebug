package org.dynamisengine.debug.api;

/**
 * Named profiling scope. Begin/end pairs measure duration.
 *
 * @param name     marker name (e.g. "physics.step", "render.geometry")
 * @param category domain category
 */
public record DebugMarker(String name, DebugCategory category) {

    public static DebugMarker of(String name, DebugCategory category) {
        return new DebugMarker(name, category);
    }

    public static DebugMarker engine(String name) { return of(name, DebugCategory.ENGINE); }
    public static DebugMarker rendering(String name) { return of(name, DebugCategory.RENDERING); }
    public static DebugMarker physics(String name) { return of(name, DebugCategory.PHYSICS); }
    public static DebugMarker audio(String name) { return of(name, DebugCategory.AUDIO); }
}
