# DynamisDebug Architecture Review

**Date:** 2026-03-21
**Status:** Complete — all 7 layers delivered

## Executive Summary

DynamisDebug became the unified observability, diagnostic, and debug-visualization spine for the entire Dynamis engine without violating subsystem boundaries. The 7-layer rollout accomplished its mission: 19 adapters, full Layers 2–7 coverage, history/timeline/query/watchdog pipeline, UI overlay, debug draw contract with OpenGL implementation, and clean ownership boundaries producing a self-observable engine.

---

## Original Goal

Make `DynamisDebug` the **canonical diagnostic coordination layer** for the whole engine, without polluting lower-level repos.

That meant proving seven things:

1. Foundational layering was clean
2. Subsystem telemetry could be adapted without reverse dependencies
3. Telemetry could be normalized into one debug model
4. Frame-by-frame capture/history/timeline/query would work
5. Watchdogs could detect anomalies
6. UI could surface the state
7. World-space debug draw could visualize runtime truth

---

## Layer-by-Layer Assessment

### Layer 1 — Foundation

**Purpose:** Establish base contracts and dependency direction.

**Requirements:**
- `DynamisDebug` lives in Layer 1 correctly
- Foundational repos do not depend on Debug
- Debug may depend on Core/Event, not reverse
- Define canonical contracts: snapshots, draw commands, queues, events, sessions, watchdogs

**What was achieved:**
- Dependency direction stayed clean: `DynamisDebug -> DynamisEvent -> DynamisCore`
- No pollution of `dynamis-parent`, `DynamisCore`, or `vectrix`
- Bridge/event integration established
- Core debug truth model created

**Verdict: Succeeded cleanly.** Layer 1 became the diagnostic substrate without contaminating the substrate below it.

---

### Layer 2 — Early Engine Subsystems

**Purpose:** Prove that debug integration could consume meaningful runtime state from major technical subsystems without forcing them to depend on Debug.

**Target subsystems:** Animis, DynamisCollision, DynamisGPU

**Requirements:**
- Read-only adaptation from public APIs
- Prove the adapter pattern
- Establish categories and snapshot conventions
- Begin real telemetry normalization

**What was achieved:**

| Adapter | Telemetry |
|---------|-----------|
| `CollisionTelemetryAdapter` | Contacts, colliders, cache |
| `GpuTelemetryAdapter` | Inflight, backlog, throughput, heap |
| `AnimisTelemetryAdapter` | Animation state, transitions, root motion, events |

**Verdict: Succeeded strongly.** This layer proved the bridge doctrine was real.

---

### Layer 3 — World Structure and Content

**Purpose:** Expand observability into the systems that define world organization and content flow.

**Target subsystems:** DynamisECS, DynamisSceneGraph, DynamisContent

**Requirements:**
- Prove entity/world state can be summarized
- Prove scene composition state can be inspected
- Prove content/cache behavior can be normalized

**What was achieved:**

| Adapter | Telemetry |
|---------|-----------|
| `EcsTelemetryAdapter` | Entity count, churn |
| `SceneGraphTelemetryAdapter` | Nodes, renderables, batches |
| `ContentTelemetryAdapter` | Cache hit/miss, failed resolutions |

**Verdict: Succeeded.** This layer gave the debug spine real world-structure awareness.

---

### Layer 4 — Presentation and Sensory Runtime

**Purpose:** Make rendering/audio/world-presentation observable.

**Target subsystems:** DynamisAudio, DynamisLightEngine, DynamisTerrain, DynamisVFX, DynamisSky

**Requirements:**
- Surface runtime health of rendering and audio
- Expose meaningful world/presentation metrics
- Prepare for later spatial debug draw and overlay usefulness

**What was achieved:**

| Adapter | Telemetry |
|---------|-----------|
| `AudioTelemetryAdapter` | DSP budget, ring buffer, voices, underruns |
| `LightEngineTelemetryAdapter` | FPS, draw calls, triangles, CPU/GPU ms |
| `TerrainTelemetryAdapter` | Chunks, foliage, terrain GPU time |
| `VfxTelemetryAdapter` | VFX counts, budget, rejections |
| `SkyTelemetryAdapter` | Sky state, time-of-day, weather |

**Verdict: Succeeded very strongly.** This is where the spine became clearly useful for real runtime diagnosis.

---

### Layer 5 — Simulation Intelligence and World Law

**Purpose:** Instrument the systems that explain **why** the world changes and **how** cognition behaves under pressure.

**Target subsystems:** DynamisPhysics, DynamisAI, DynamisScripting (DynamisExpression deferred)

**Requirements:**
- Make simulation health explainable
- Expose AI and scripting in semantic planes, not just raw counters
- Support watchdog reasoning across simulation domains

**What was achieved:**

| Adapter | Telemetry |
|---------|-----------|
| `PhysicsTelemetryAdapter` | Physics step health |
| `AiTelemetryAdapter` | Simulation, Cognition, Planning, Budget/LOD planes |
| `ScriptingTelemetryAdapter` | Canon, Oracle, Chronicler, Percept, Degradation planes |

Watchdog packs for: canon stalls, reject spikes, backlog spikes, staleness, degradation, AI budget/replan/inference pressure.

**Verdict: Exceeded the original goal.** Layer 5 moved Debug from "engine metrics" into "runtime explainability."

---

### Layer 6 — Game Services and Player-Facing Runtime

**Purpose:** Make input/UI/windowing/service-level runtime state observable and prepare the eventual debug face.

**Target subsystems:** DynamisInput, DynamisUI, DynamisWindow, DynamisLocalization (SpatialInput deferred)

**Requirements:**
- Expose live player/system-facing runtime state
- Prepare `DynamisUI` to become the visual face of the debug spine
- Complete operational observability across game services

**What was achieved:**

| Adapter | Telemetry |
|---------|-----------|
| `InputTelemetryAdapter` | Devices, events, gamepads, contexts |
| `WindowTelemetryAdapter` | Dimensions, DPI, focus, resizes |
| `UiTelemetryAdapter` | Layers, widgets, FPS, overlay state |
| `LocalizationTelemetryAdapter` | Locale, missing keys, fallbacks |

**Verdict: Succeeded.** This completed the runtime-service side and established the presentation split: Debug owns truth, UI owns presentation.

---

### Layer 7 — Orchestration

**Purpose:** Make the top-level engine coordinator observable, so the engine can explain how it is coordinating the whole runtime.

**Target subsystem:** DynamisWorldEngine

**Requirements:**
- Expose engine-level summary state
- Allow correlation across subsystems
- Support the "self-observable engine" goal

**What was achieved:**

| Adapter | Telemetry |
|---------|-----------|
| `WorldEngineTelemetryAdapter` | Full engine-level coordination state |

**Verdict: Succeeded.** This closed the loop from subsystem truth to orchestration truth.

---

## Cross-Layer Goals

### 1. Keep Source Repos Clean

**Goal:** No reverse dependency on `DynamisDebug`.
**Result:** Achieved. Everything important flows through bridge adapters.

### 2. Create One Unified Debug Model

**Goal:** Normalize telemetry into one consistent structure.
**Result:** Achieved. Snapshots, categories, flags, summaries, planes, history, timeline, query all converge.

### 3. Support Both Push and Pull Capture

**Goal:** Subsystems can submit or be sampled.
**Result:** Achieved. Frame lifecycle proved push + pull + merge.

### 4. Make the Engine Self-Observable

**Goal:** Engine can observe and reason about itself.
**Result:** Achieved. History, timeline, query, watchdog, and overlay all working.

### 5. Support Anomaly Detection

**Goal:** Watchdog rules detect pressure/pathology, not just raw metrics.
**Result:** Achieved. Simulation watchdogs and cross-subsystem alerting proved this.

### 6. Separate Truth from Presentation

**Goal:** Debug owns data, UI owns face, LightEngine owns world draw.
**Result:** Achieved. This split is clean and explicit.

### 7. Enable Spatial Diagnostics

**Goal:** World-space debug draw intent and renderer contract.
**Result:** Achieved. Contract, consumer, renderer, and proving module all exist. OpenGL implementation complete.

---

## Module Architecture

```
DynamisDebug/
  dynamisdebug-api      Pure contracts (no dependencies) — snapshots, draw commands, events, queries
  dynamisdebug-core     Runtime coordination — session, history, timeline, watchdog, registry
  dynamisdebug-draw     Renderer-agnostic draw intent — DebugDrawQueue (frame-scoped accumulator)
  dynamisdebug-bridge   Subsystem adapters (19) + DebugBridge frame orchestrator + event bus adapter
```

**Dependency chain:**
- `dynamisdebug-api` → no dependencies (pure contracts)
- `dynamisdebug-core` → `dynamisdebug-api`
- `dynamisdebug-draw` → `dynamisdebug-api`
- `dynamisdebug-bridge` → `dynamisdebug-api` (+ transitive subsystem dependencies for adapters)

---

## Adapter Coverage

| Layer | Adapter | Subsystem |
|-------|---------|-----------|
| 2 | `CollisionTelemetryAdapter` | DynamisCollision |
| 2 | `GpuTelemetryAdapter` | DynamisGPU |
| 2 | `AnimisTelemetryAdapter` | Animis |
| 3 | `EcsTelemetryAdapter` | DynamisECS |
| 3 | `SceneGraphTelemetryAdapter` | DynamisSceneGraph |
| 3 | `ContentTelemetryAdapter` | DynamisContent |
| 4 | `AudioTelemetryAdapter` | DynamisAudio |
| 4 | `LightEngineTelemetryAdapter` | DynamisLightEngine |
| 4 | `TerrainTelemetryAdapter` | DynamisTerrain |
| 4 | `VfxTelemetryAdapter` | DynamisVFX |
| 4 | `SkyTelemetryAdapter` | DynamisSky |
| 5 | `PhysicsTelemetryAdapter` | DynamisPhysics |
| 5 | `AiTelemetryAdapter` | DynamisAI |
| 5 | `ScriptingTelemetryAdapter` | DynamisScripting |
| 6 | `InputTelemetryAdapter` | DynamisInput |
| 6 | `WindowTelemetryAdapter` | DynamisWindow |
| 6 | `UiTelemetryAdapter` | DynamisUI |
| 6 | `LocalizationTelemetryAdapter` | DynamisLocalization |
| 7 | `WorldEngineTelemetryAdapter` | DynamisWorldEngine |

---

## Proving Modules

| Module | Location | What it proves |
|--------|----------|----------------|
| `debug-overlay` | `DynamisGames/proving/developer-tools/` | Text overlay rendering, telemetry HUD |
| `debug-draw-basics` | `DynamisGames/proving/developer-tools/` | Full debug draw pipeline: queue, bridge helpers, OpenGL wireframe, depth modes |

---

## Ownership Boundaries (Final)

| Concern | Owner |
|---------|-------|
| Diagnostic truth, telemetry model, watchdogs | `DynamisDebug` |
| Visual presentation of debug state | `DynamisUI` |
| World-space debug rendering | `DynamisLightEngine` (production) / self-contained renderer (proving) |
| Subsystem-specific telemetry production | Each subsystem's public API (read-only) |
| Subsystem-to-debug adaptation | `dynamisdebug-bridge` adapters |

---

## What Remains

- **DynamisExpression** telemetry adapter (deferred — expression system is alpha)
- **DynamisSpatialInput** telemetry adapter (deferred — hardware validation pending)
- **Sphere rendering** in debug draw (`DebugSphereCommand` exists in API, renderer stub only)
- **World-space text** in debug draw (UI-layer responsibility, not Debug)
- Integration of debug draw into `physics-basics`, `terrain-basics`, and AI perception proving modules (Step 2 of the proving plan)
