# DynamisDebug — Agent Guidelines

## What This Repo Is
Canonical engine observability platform (Layer 1). Sole authority for
diagnostic truth, telemetry coordination, watchdog rules, debug draw
intent, and analytical queries.

## Module Structure
| Module | Purpose |
|---|---|
| `dynamisdebug-api` | Pure contracts — snapshots, events, queries, flags, counters, draw commands, watchdog rules. |
| `dynamisdebug-core` | Runtime — DebugSession, DebugHistory, DebugTimeline, DebugWatchdog, DebugAnalytics, DebugEventBuffer, query service. |
| `dynamisdebug-draw` | Renderer-agnostic debug draw intent — DebugDrawQueue + convenience methods. |
| `dynamisdebug-bridge` | Subsystem adapters (19) + DebugBridge frame capture orchestrator + DebugEventBusAdapter. |

## Dependency Order
```
dynamisdebug-api → dynamisdebug-core / dynamisdebug-draw → dynamisdebug-bridge
```

## Authority Domain (registered in docs/architecture/subsystem-authority.md)

DynamisDebug **owns**:
- Debug session state and retained diagnostic history
- Telemetry aggregation and diagnostic snapshots
- Timeline events and event history
- Watchdog rule definition, cooldowns, evaluation, and alert generation
- Debug analytics and query logic
- Debug draw intent/contracts and diagnostic draw commands
- Export contracts for diagnostic truth

DynamisDebug **does NOT own**:
- UI presentation, overlay layout, or view composition (DynamisUI)
- Rendering implementations for debug/UI visuals (DynamisLightEngine)
- Windowing or input handling (DynamisWindow, DynamisInput)
- Proving scenarios or demo-specific wiring (DynamisGames/proving)

## Boundary Relationships
```
DynamisDebug  → provides diagnostic truth to DynamisUI
DynamisUI     → owns DebugOverlayRenderer SPI, DebugViewSnapshot contract, builders, models
LightEngine   → owns rendering implementations (OpenGL + Vulkan)
proving       → demonstrates usage only, owns no infrastructure
```

## Conventions (must match DynamisCore baseline)
- groupId: `org.dynamisengine.debug`
- Package root: `org.dynamisengine.debug.*`
- Logging: `DynamisLogger` only
- `module-info.java` required in every module
- JUnit 5, SpotBugs, Checkstyle (google_checks.xml)

## Critical Rules
- Proving modules must not duplicate DynamisDebug or DynamisUI types
- All shared infrastructure has a single owning module
- If ownership is unclear, resolve ownership before coding
