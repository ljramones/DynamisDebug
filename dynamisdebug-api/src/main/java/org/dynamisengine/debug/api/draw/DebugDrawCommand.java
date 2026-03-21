package org.dynamisengine.debug.api.draw;

/**
 * Sealed hierarchy of debug draw commands.
 * Renderer-agnostic: backends consume these to produce actual visuals.
 */
public sealed interface DebugDrawCommand
        permits DebugLineCommand, DebugBoxCommand, DebugSphereCommand, DebugTextCommand {}
