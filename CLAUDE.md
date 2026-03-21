# DynamisDebug

Canonical debug/diagnostic coordination layer for the Dynamis engine (Layer 1).

## Build

```bash
mvn clean install
mvn test          # 204 tests
```

## Architecture

```
dynamisdebug-api      Pure contracts (no deps). Snapshots, events, queries, flags, counters,
                      draw commands, watchdog rules.
dynamisdebug-core     Runtime: DebugSession, DebugHistory, DebugTimeline, DebugWatchdog,
                      DebugEventBuffer, command registry, query service, snapshot aggregator.
dynamisdebug-draw     Renderer-agnostic debug draw intent: DebugDrawQueue + convenience methods.
dynamisdebug-bridge   Adapters + integration: TelemetryAdapter<T>, DebugBridge (frame capture
                      orchestrator), DebugEventBusAdapter, 11 subsystem adapters.
```

## Doctrine

- **Subsystems produce telemetry; DynamisDebug normalizes and reasons over it.**
- **Bridge adapters are the churn zone; source repos stay debug-agnostic.**
- **Watchdogs detect anomalies; orchestration decides compensation.**
- **History, timeline, and query are first-class runtime diagnostics surfaces, not afterthoughts.**
- **In Layer 1, DynamisDebug may consume DynamisCore and DynamisEvent, but foundational repos must never depend on DynamisDebug.**
- **Debug integration in higher layers happens through bridge adapters and engine events, not reverse dependencies.**

DynamisDebug does NOT own:
- Subsystem telemetry generation (stays in each subsystem)
- Rendering implementation (stays in LightEngine/backends)
- UI widgets (stays in DynamisUI)
- Logging framework
- Benchmark/tracing frameworks

## Frame Lifecycle

```
Subsystems → submitTelemetry("name", data) → bridge buffer
                                                  ↓
bridge.captureFrame(N) → captureProviders (pull) + adapt buffered telemetry (push)
                       → merge into single frame → history.record(N, merged)
                       → watchdog.evaluate(N, merged) → DebugEvent alerts
                                                  ↓
                        timeline / queries / event buffer → engine can subscribe & compensate
```

## Adapter Inventory (11 adapters, Layers 2–4)

| Adapter | Layer | Category | Source Telemetry |
|---------|-------|----------|-----------------|
| CollisionTelemetryAdapter | 2 | PHYSICS | CollisionDebugSnapshot3D |
| GpuTelemetryAdapter | 2 | RENDERING | UploadTelemetry + BindlessHeapStats |
| AnimisTelemetryAdapter | 2 | ENGINE | AnimisTelemetrySnapshot (plain DTO) |
| EcsTelemetryAdapter | 3 | ECS | World + WorldDelta |
| SceneGraphTelemetryAdapter | 3 | RENDERING | DefaultSceneGraph views |
| ContentTelemetryAdapter | 3 | CONTENT | ContentTelemetrySnapshot (plain DTO) |
| AudioTelemetryAdapter | 4 | AUDIO | AudioTelemetry + voice state |
| LightEngineTelemetryAdapter | 4 | RENDERING | EngineStats |
| TerrainTelemetryAdapter | 4 | RENDERING | TerrainStats |
| VfxTelemetryAdapter | 4 | RENDERING | VfxStats + VfxBudgetStats |
| SkyTelemetryAdapter | 4 | RENDERING | SunState + TimeOfDayState + WeatherState |

## Key Types

- `DebugSnapshot` — point-in-time state ("what is true now")
- `DebugEvent` — transient occurrence ("what just happened")
- `DebugQuery` — immutable filter (category/severity/source/frame/time)
- `DebugSession` — central runtime coordinator
- `DebugBridge` — frame capture orchestrator (push + pull telemetry)
- `DebugTimeline` — time-series metric extraction with stats
- `DebugWatchdog` — reactive threshold evaluation, fires events on breach
- `WatchdogRule` — threshold rule with comparison, cooldown, severity
- `DebugSnapshotAggregator` — merges snapshots with source-prefixed keys
- `DebugDrawQueue` — frame-scoped draw command accumulator
- `DebugEventBusAdapter` — bridges debug lifecycle onto DynamisEvent bus
