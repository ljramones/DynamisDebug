# DynamisDebug

Canonical debug/diagnostic coordination layer for the Dynamis engine (Layer 1).

## Build

```bash
mvn clean install
```

## Architecture

```
dynamisdebug-api      Pure contracts (no deps). Markers, counters, flags, snapshots, events, draw commands.
dynamisdebug-core     Runtime: DebugSession, DebugHistory, DebugEventBuffer, command registry.
dynamisdebug-draw     Renderer-agnostic debug draw intent: DebugDrawQueue + convenience methods.
dynamisdebug-bridge   Adapters: TelemetryAdapter<T> for subsystem integration.
```

## Doctrine

Subsystems produce telemetry; DynamisDebug normalizes, records, queries, and visualizes diagnostic state.

DynamisDebug does NOT own:
- Subsystem telemetry generation (stays in each subsystem)
- Rendering implementation (stays in LightEngine/backends)
- UI widgets (stays in DynamisUI)
- Logging framework
- Benchmark/tracing frameworks

## Key Types

- `DebugSnapshot` -- point-in-time state ("what is true now")
- `DebugEvent` -- transient occurrence ("what just happened")
- `DebugSession` -- central runtime coordinator
- `DebugDrawQueue` -- frame-scoped draw command accumulator
- `DebugBridge` -- subsystem telemetry adapter registry
