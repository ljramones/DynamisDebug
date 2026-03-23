# DynamisDebug Proving Ladder — Complete

**Date:** 2026-03-22
**Status:** All 7 modules delivered and verified

## System Identity

DynamisDebug is not just debug tooling. It is an **engine observability platform**:

- **Real-time telemetry** — DebugSession, 19 subsystem adapters
- **Historical analysis** — history ring buffer, timeline, time-series extraction
- **Policy enforcement** — watchdog rules with cooldowns, severity escalation
- **Visual inspection** — structured overlay panels + world-space debug draw
- **Analytical queries** — DebugAnalytics (spike detection, noise ranking, threshold analysis, correlation)

This platform supports gameplay debugging, performance analysis, QA tooling, and live diagnostics.

## Overview

The 7-module proving ladder validates every capability end-to-end through interactive demonstrations that serve as both regression guards and developer onboarding.

## Why the Proving Ladder Exists

Each module isolates and validates a single dimension of observability:

- **overlay** — visibility
- **history** — time
- **correlation** — causality
- **watchdog** — policy
- **queries** — analysis
- **capstone** — integration

This ensures the system is understood, testable, and teachable.

## Design Principles

1. **Separation of Concerns** — Debug produces truth, UI presents it, proving demonstrates usage
2. **Observability over Visualization** — The system must answer questions, not just display data
3. **Incremental Proof** — Every capability is proven in isolation before integration
4. **Deterministic Behavior** — Queries, history, and alerts must be reproducible and testable
5. **Bounded Complexity** — Features are added with explicit non-goals to prevent overgrowth

## The 7 Modules

| # | Module | What it proves | Key takeaway |
|---|--------|---------------|--------------|
| 1 | `debug-overlay` | Live HUD with grouped alerts, trends, compact layout | Alert grouping and panel sizing matter more than raw feature count |
| 2 | `debug-draw-basics` | World-space debug visualization (collision/terrain/scenegraph) | Debug draw and overlay are independent capabilities that coexist cleanly |
| 3 | `debug-history-timeline` | Time-aware diagnostics with 4-phase scenario cycling | Trends transform a status board into a diagnostic instrument |
| 4 | `debug-correlation-basics` | Cross-subsystem causal patterns (3 distinct fault signatures) | Alert ordering and sparkline timing reveal root cause |
| 5 | `debug-watchdog-basics` | Rule behavior, cooldowns, escalation, flapping | Cooldowns are essential — 0-cooldown rules produce 20x+ noise |
| 6 | `debug-session-queries` | Programmatic observability (spike/noise/threshold/correlation queries) | The spine is queryable as data, not just viewable as panels |
| 7 | `debug-subsystem-showcase` | Capstone: everything together in one multi-subsystem runtime | All capabilities integrate cleanly under realistic load conditions |

## Architecture Summary

```
Layer 1: DynamisDebug
  dynamisdebug-api        Pure contracts (snapshots, events, queries, draw commands, watchdog rules)
  dynamisdebug-core       Runtime (session, history, timeline, watchdog, analytics, query service)
  dynamisdebug-draw       Debug draw intent (DebugDrawQueue)
  dynamisdebug-bridge     19 subsystem adapters + frame capture orchestrator

Layer 6: DynamisUI / ui-debug
  model/                  DebugOverlayPanel, Row, Flag, MiniTrend, PanelId, Region, Severity enums
  builder/                DebugOverlayBuilder, DebugViewSnapshot (UI data contract), DebugViewSnapshotMapper
  render/                 DebugOverlayRenderer SPI (OpenGL/Vulkan/headless)
  runtime/                DebugOverlayOptions (budget/throttling)

Proving modules (DynamisGames/proving/developer-tools/)
  7 modules               Scenario generation + wiring only (no owned models or mapper logic)
```

### Data Flow

```
Subsystem state
  -> DebugSnapshot (via bridge adapters or direct construction)
  -> DebugSession.history().record(tick, snapshots)
  -> DebugWatchdog.evaluate(tick, snapshots) -> DebugEvent alerts
  -> DebugViewSnapshotMapper.mapFromFrame(tick, snapshots) -> DebugViewSnapshot
  -> DebugOverlayBuilder.buildAll(viewSnapshot) -> List<DebugOverlayPanel>
  -> DebugOverlayRenderer (SPI implementation) -> pixels

  Parallel path:
  -> DebugDrawQueue.submit(command) -> DebugDrawRenderer -> world-space wireframes

  Query path:
  -> DebugAnalytics.findSpikes/rankNoisyRules/analyzeThresholdCrossings/findCorrelatedFrames
  -> query result records -> custom panel in overlay
```

### Ownership Boundaries

| Concern | Owner |
|---------|-------|
| Diagnostic truth, history, watchdogs, analytics | DynamisDebug (Layer 1) |
| Overlay models, builder, renderer SPI | DynamisUI / ui-debug (Layer 6) |
| World-space debug rendering | DynamisLightEngine (production) / self-contained renderer (proving) |
| Subsystem telemetry production | Each subsystem's public API |
| Subsystem-to-debug adaptation | dynamisdebug-bridge adapters (19) |
| DebugViewSnapshot (UI data contract) | DynamisUI / ui-debug |
| DebugViewSnapshotMapper | DynamisUI / ui-debug (depends downward on DynamisDebug) |

## Key Learnings

### 1. Alert grouping was essential
Without deduplication, sustained watchdog firing made the overlay unusable. Grouping by rule name with count (`x8`) and severity-priority sorting transformed readability.

### 2. Cooldowns are critical
The watchdog-basics module proved a 0-cooldown rule produces 20x+ more fires than a properly cooled rule. This is now visually irrefutable in the proving module.

### 3. Correlation patterns are teachable
Three distinct fault signatures (ECS overload, physics spike, audio pressure) each produce a different multi-panel pattern. Developers can learn to read these like diagnostic fingerprints.

### 4. Trends changed usability dramatically
Adding sparklines from real history data transformed the overlay from a static snapshot display into a diagnostic instrument that answers "is this getting worse?"

### 5. Queries unlock a new dimension
DebugAnalytics (spike detection, noise ranking, threshold analysis, correlated windows) proved the spine is a queryable analytics platform, not just a visual dashboard.

### 6. Panel sizing matters more than features
Compact empty panels (30px dim) vs trend-rich panels with breathing room made the overlay feel intentionally weighted toward live signal rather than uniformly wasting space.

### 7. Legacy rendering paths must be removed, not kept alongside
Having both old and new overlay paths caused overlap/confusion. The new path should be the sole owner immediately.

### 8. STBEasyFont is ASCII-only
Unicode characters (em-dash) render as garbled text. All overlay text must be pure ASCII.

### 9. Direct ByteBuffer required for LWJGL
`ByteBuffer.allocate()` (heap) crashes with SIGSEGV. Must use `ByteBuffer.allocateDirect()`.

### 10. GL.createCapabilities() must precede any GL calls
Missing this call causes "No context is current" fatal error on macOS.

## Known Gaps

### API / Architecture
- **DebugViewSnapshotMapper** logically belongs in a debug-ui-bridge module, not in DynamisUI. Acceptable short-term, should move later.
- **Timeline events are pull-based** (`recentEvents()`) — subscription model would be more efficient for high-volume scenarios.
- **Trend extraction re-queries history each frame** — could cache/invalidate for performance at scale.
- **No per-event deduplication in timeline** — same watchdog can appear multiple times.
- **No root-cause inference engine** — correlation is visual/manual, not automated.

### Overlay / Rendering
- **No drill-down/focus mode** — panels cannot be expanded for detail view.
- **No panel interaction** (click, hover, scroll).
- **Renderer is OpenGL-only** — Vulkan parity not yet implemented.
- **Layout is simple column-based** — no dynamic layout engine.
- **Text rendering limited** — STBEasyFont, ASCII only, no scaling/wrapping.

### Debug Draw
- **Sphere rendering stubbed** — `DebugSphereCommand` exists in API but is not rendered.
- **World-space text not implemented** — UI-layer responsibility.

## Production Surface Classification

### Production-worthy (lock these APIs)
- `DebugSession` — central coordinator
- `DebugSnapshot` — canonical truth record
- `DebugEvent` — transient occurrence
- `DebugHistory` / `DebugTimeline` — time-series storage and extraction
- `DebugWatchdog` / `WatchdogRule` — anomaly detection
- `DebugAnalytics` — higher-level queries (spikes, noise, thresholds, correlation)
- `DebugDrawQueue` / `DebugDrawCommand` hierarchy — debug draw intent
- `DebugOverlayPanel` / `DebugOverlayRow` / `DebugFlagView` / `DebugMiniTrend` — UI model
- `DebugOverlayBuilder` — panel construction
- `DebugOverlayRenderer` SPI — backend-agnostic rendering contract
- `DebugViewSnapshot` — stable UI data contract boundary
- `DebugViewSnapshotMapper` — truth-to-UI bridge
- `DebugOverlayOptions` — configuration/budget

### Internal (can evolve)
- Panel layout heuristics (compact height, trend breathing room)
- Trend window sizing and metric selection (`TREND_METRICS` map)
- Timeline event formatting and display limits
- Alert grouping algorithm and severity propagation rules
- Text truncation width calculation

### Proving-only (do not productize)
- Scenario generators (phase cycling, metric simulation)
- Synthetic telemetry patterns
- Demo UI shortcuts (key bindings for phase/query/reset)
- Copied subsystem stubs (WindowSubsystem, WindowInputSubsystem, TextRenderer)

## What This Enables

### Immediate
- **Gameplay debugging** — see ECS, physics, audio state in real time
- **Performance debugging** — frame budget, spike detection, trend analysis
- **QA tooling** — watchdog alerts surface anomalies without manual inspection
- **Developer onboarding** — proving modules teach the canonical patterns

### Future
- **Live ops / telemetry export** — DebugViewSnapshot is serializable
- **Replay debugging** — history ring buffer supports time-scrubbing
- **Remote dashboards** — DebugOverlayRenderer SPI supports headless/remote rendering
- **Editor integration** — DebugAnalytics powers editor inspector panels
- **Automated regression** — watchdog rules can drive CI pass/fail decisions

## Not Yet (Explicitly Deferred)

- **Full editor integration** — the overlay is runtime-only; editor panels are a separate concern
- **Arbitrary query language / SQL-like system** — DebugAnalytics covers the proven patterns; generalization is premature
- **Complex UI widgets or layout engine** — simple column-based layout is sufficient for Phase 1
- **Networked multi-client debugging** — remote export is future; real-time multi-client is much later
- **Automatic root-cause inference** — correlation is visual/manual; automated causal reasoning is research-grade
- **Mouse/click interaction** — keyboard-only for now; pointer interaction adds UI framework complexity

**Rationale:** Keep the system focused, composable, and proven incrementally. Every deferred item can be added later without architectural changes because the boundaries are clean.

## Next Steps (Scoped)

### 1. Focus / Drill-Down Mode

Expand selected panel to fullscreen showing:
- Full metric list (no truncation)
- Enlarged trends (full-width sparklines)
- Last N events for that category
- Keyboard navigation between panels (arrow keys or number keys)

**Non-goals:** No mouse interaction. No nested UI hierarchy. No mini-editor.

### 2. Replay Integration (Time Navigation)

- Freeze DebugSession at a selected historical frame
- Switch DebugViewSnapshotMapper to read from historical frame instead of live
- Add time cursor (frame index or timestamp)
- Allow stepping forward/backward through history

**Non-goals:** No full game-state rewind. No input replay. No deterministic re-simulation.

### 3. Vulkan Renderer Parity

Implement DebugOverlayRenderer SPI for Vulkan backend. The SPI contract already exists; this is a rendering-only task.

### 4. Remote Telemetry / Headless Export

DebugViewSnapshot is a serializable record. Export it for external dashboards or headless CI analysis.

## Production API Surface

`DebugAnalytics` is a production-grade query layer intended for reuse beyond proving modules:

- `findSpikes()` — threshold exceedance detection with max value and last spike frame
- `rankNoisyRules()` — watchdog fire-count ranking for rule quality assessment
- `analyzeThresholdCrossings()` — transition counting with first/last crossing and duration above
- `findCorrelatedFrames()` — multi-condition co-occurrence detection across subsystems

This API is suitable for runtime diagnostics, editor tooling, and automated test assertions.

## Architectural Note

`DebugViewSnapshotMapper` currently lives in `DynamisUI/ui-debug` for convenience (it depends downward on DynamisDebug, which is valid). Long-term it may move to a dedicated `debug-ui-bridge` module in DynamisDebug to keep the truth-production pipeline closer to its source.

## Test Coverage

| Module | Tests |
|--------|-------|
| DynamisDebug (dynamisdebug-core) | 74 (includes 9 DebugAnalytics) |
| DynamisUI (ui-debug) | 62 (model + builder + mapper) |
| **Total** | **136** |

## Session Summary

This proving ladder was completed in a single extended session (2026-03-21 to 2026-03-22). Starting from a GPG passphrase hang fix and the initial debug-draw-basics scaffold, the session produced:

- 1 new production API class (`DebugAnalytics`)
- 15 new UI model/builder/render types
- 7 proving modules with 40+ Java source files
- 136 tests
- 2 architecture review documents
- Commits across 4 repositories (DynamisDebug, DynamisUI, DynamisGames, dynamis-parent)
