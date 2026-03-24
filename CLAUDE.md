# DynamisDebug

Canonical debug/diagnostic coordination layer for the Dynamis engine (Layer 1).

## Build

```bash
mvn clean install
mvn test          # 249 tests
```

## Architecture

```
dynamisdebug-api      Pure contracts (no deps). Snapshots, events, queries, flags, counters,
                      draw commands, watchdog rules.
dynamisdebug-core     Runtime: DebugSession, DebugHistory, DebugTimeline, DebugWatchdog,
                      DebugEventBuffer, command registry, query service, snapshot aggregator.
dynamisdebug-draw     Renderer-agnostic debug draw intent: DebugDrawQueue + convenience methods.
dynamisdebug-bridge   Adapters + integration: TelemetryAdapter<T>, DebugBridge (frame capture
                      orchestrator), DebugEventBusAdapter, 19 subsystem adapters across all 7 layers.
```

## Doctrine

- **DynamisDebug is the sole authority for engine observability truth and diagnostic policy** (registered in `docs/architecture/subsystem-authority.md`).
- **The debug spine is a first-class engine subsystem.**
- **All runtime telemetry enters DynamisDebug through bridge adapters only.**
- **Subsystems remain debug-agnostic producers of public state.**
- **Frame-scoped normalized snapshots are the canonical diagnostic truth.**
- **Watchdogs detect anomalies; orchestration decides compensation.**
- **Plane-based telemetry is preferred for complex subsystems (AI: 4 planes, Scripting: 5 planes).**
- **DynamisDebug owns diagnostic truth; DynamisUI owns diagnostic presentation.**
- **In Layer 1, DynamisDebug may consume DynamisCore and DynamisEvent, but foundational repos must never depend on DynamisDebug.**

DynamisDebug does NOT own:
- Subsystem telemetry generation (stays in each subsystem)
- Debug overlay/inspector UI (stays in DynamisUI)
- Debug draw rendering (stays in DynamisLightEngine)
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

## Adapter Inventory (19 adapters, Layers 2–7)

| # | Adapter | Layer | Category | Source Telemetry |
|---|---------|-------|----------|-----------------|
| 1 | CollisionTelemetryAdapter | 2 | PHYSICS | CollisionDebugSnapshot3D |
| 2 | GpuTelemetryAdapter | 2 | RENDERING | UploadTelemetry + BindlessHeapStats |
| 3 | AnimisTelemetryAdapter | 2 | ENGINE | AnimisTelemetrySnapshot (plain DTO) |
| 4 | EcsTelemetryAdapter | 3 | ECS | World + WorldDelta |
| 5 | SceneGraphTelemetryAdapter | 3 | RENDERING | SceneGraph views |
| 6 | ContentTelemetryAdapter | 3 | CONTENT | ContentTelemetrySnapshot (plain DTO) |
| 7 | AudioTelemetryAdapter | 4 | AUDIO | AudioTelemetry + voice state |
| 8 | LightEngineTelemetryAdapter | 4 | RENDERING | EngineStats |
| 9 | TerrainTelemetryAdapter | 4 | RENDERING | TerrainStats |
| 10 | VfxTelemetryAdapter | 4 | RENDERING | VfxStats + VfxBudgetStats |
| 11 | SkyTelemetryAdapter | 4 | RENDERING | SunState + TimeOfDayState + WeatherState |
| 12 | PhysicsTelemetryAdapter | 5 | PHYSICS | PhysicsStats |
| 13 | AiTelemetryAdapter | 5 | AI | 4 planes: sim/cognition/planning/budget |
| 14 | ScriptingTelemetryAdapter | 5 | SCRIPTING | 5 planes: canon/oracle/chronicler/percept/degradation |
| 15 | InputTelemetryAdapter | 6 | INPUT | InputTelemetry |
| 16 | WindowTelemetryAdapter | 6 | ENGINE | WindowTelemetrySnapshot (plain DTO) |
| 17 | UiTelemetryAdapter | 6 | UI | UiTelemetrySnapshot (plain DTO) |
| 18 | LocalizationTelemetryAdapter | 6 | UI | LocalizationTelemetrySnapshot (plain DTO) |
| 19 | WorldEngineTelemetryAdapter | 7 | ENGINE | WorldTelemetrySnapshot |

## Watchdog Rule Packs

**SimulationWatchdogRules** (12 rules):
- Physics: stepTimeHigh, sleepingRatioHigh
- Scripting: canonCommitStall, oracleRejectSpike, chroniclerBacklogSpike, perceptStalenessHigh, degradationTier3Spike
- AI: budgetExceeded, degradeSpike, inferenceBacklog, replanThrashing, perceptStaleness

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

## Presentation Split

```
DynamisDebug (Layer 1)     → data, capture, history, timeline, queries, watchdogs, draw intent
DynamisUI (Layer 6)        → overlay panels, inspector, alert badges, timeline widgets
DynamisLightEngine (Layer 4) → debug draw rendering (lines, boxes, spheres, text in world space)
```
