package org.dynamisengine.debug.api;

/**
 * Named boolean toggle for debug features.
 *
 * @param name    flag name (e.g. "showWireframe", "drawColliders")
 * @param enabled current state
 */
public record DebugFlag(String name, boolean enabled) {

    public DebugFlag toggled() { return new DebugFlag(name, !enabled); }

    public static DebugFlag of(String name, boolean initial) {
        return new DebugFlag(name, initial);
    }
}
