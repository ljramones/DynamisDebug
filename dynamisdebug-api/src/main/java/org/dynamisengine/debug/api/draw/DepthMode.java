package org.dynamisengine.debug.api.draw;

/**
 * Controls depth-testing behavior for debug draw commands.
 */
public enum DepthMode {
    /** Normal depth testing — occluded by scene geometry. */
    TESTED,
    /** Always visible — renders on top of scene geometry (x-ray). */
    ALWAYS_VISIBLE
}
