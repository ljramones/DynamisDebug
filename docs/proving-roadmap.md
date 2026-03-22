# DynamisDebug Proving Roadmap

**Date:** 2026-03-21
**Location:** `DynamisGames/proving/developer-tools/`

## Overview

These modules serve as:

- **Capability proofs** — validate debug system behavior
- **Usage examples** — show canonical integration patterns
- **Regression guards** — prevent drift or breakage
- **Developer onboarding** — teach how to use the debug spine

## Core Doctrine

- DynamisDebug owns **diagnostic truth**
- Bridge adapters own **telemetry adaptation**
- Subsystems remain **debug-agnostic producers**
- DynamisUI owns **diagnostic presentation** (overlay models, builder, widgets)
- LightEngine owns **world-space debug rendering**
- Snapshot is the **unit of diagnostic truth**
- Watchdogs detect; **orchestration responds**

### Ownership Rule

> If it is reusable debug presentation infrastructure, it belongs in `DynamisUI`.
> If it is reusable debug truth/instrumentation infrastructure, it belongs in `DynamisDebug`.
> If it only demonstrates usage, it belongs in proving.

---

## Proving Ladder

### 1. `debug-overlay-basics`

**Status:** Exists (debug-overlay module)

**Purpose:** Prove the UI face of the debug system.

**Demonstrates:**
- `DebugSession` creation and adapter registration
- Per-frame capture
- Overlay panel building
- Category grouping
- Flags and alerts display

**Teaches:** How to surface engine state in real time.

**Success criteria:**
- Overlay displays live metrics
- Categories are clear
- Flags and alerts are visible

---

### 2. `debug-history-timeline`

**Status:** Not yet created

**Purpose:** Prove time-aware diagnostics.

**Demonstrates:**
- Frame history accumulation
- Timeline extraction
- Trend analysis (min/max/avg)
- Last-N frame metrics
- Frame-range querying

**Teaches:** How to reason about change over time.

**Success criteria:**
- Last-N frame metrics visible
- Timeline stats compute correctly
- Metric trends are obvious

---

### 3. `debug-watchdog-basics`

**Status:** Not yet created

**Purpose:** Prove active anomaly detection.

**Demonstrates:**
- Watchdog rule registration
- Threshold breaches via synthetic/controlled spikes
- Alert generation
- Event buffer output
- Multiple rules firing independently

**Example conditions:**
- ECS churn spike
- GPU backlog spike
- AI budget exceeded
- Scripting percept staleness

**Teaches:** How to define rules and how alerts flow through the system.

**Success criteria:**
- Rules fire correctly on threshold breach
- Alerts appear in overlay/log/event buffer
- Capture pipeline remains stable

---

### 4. `debug-draw-basics`

**Status:** Complete and running

**Purpose:** Prove world-space debug visualization.

**Demonstrates:**
- `DebugDrawQueue` command accumulation
- Subsystem draw helpers (`CollisionDebugDraw`, `TerrainDebugDraw`, `SceneGraphDebugDraw`)
- `DebugDrawConsumer` contract
- OpenGL wireframe rendering of collision bounds, contact points/normals, terrain chunks, scene graph bounds
- `DepthMode.TESTED` vs `DepthMode.ALWAYS_VISIBLE`

**Teaches:** How to submit and render world-space diagnostics.

**Success criteria:**
- Lines and boxes render correctly
- Depth modes visibly differ
- Commands clear per frame (no ghost lines)

---

### 5. `debug-correlation-basics`

**Status:** Not yet created

**Purpose:** Prove cross-subsystem reasoning.

**Demonstrates:**
- Two or more subsystems spiking together
- Multiple independent watchdog alerts
- Overlay/timeline makes the relationship visible

**Example correlations:**
- ECS churn + GPU backlog
- AI degradation + scripting percept staleness
- Rendering pressure + audio budget pressure

**Teaches:** How to interpret system-wide behavior and correlate issues.

**Success criteria:**
- Correlated conditions are visible
- Multiple alerts coexist meaningfully
- User can see that one symptom may have multiple sources

---

### 6. `debug-session-queries`

**Status:** Not yet created

**Purpose:** Prove programmatic debug access patterns.

**Demonstrates:**
- Latest snapshot access
- Category query
- Source query
- Frame-range query
- Timeline metric extraction
- Summary/stat APIs

**Teaches:** Canonical query patterns for tools and gameplay systems.

**Success criteria:**
- Queries return correct data
- Examples are clear and reusable as developer recipes

---

### 7. `debug-subsystem-showcase`

**Status:** Not yet created

**Purpose:** Capstone module demonstrating the full debug spine.

**Demonstrates integration across:**
- Physics, ECS, rendering, audio, AI, scripting, input, UI, window, content

**Shows:**
- Overlay with all category panels
- Timeline/history
- Watchdog alerts
- Debug draw
- Cross-subsystem visibility in one session

**Teaches:** What full-engine observability looks like in practice.

**Success criteria:**
- All systems visible in one runtime
- Debug spine operates cohesively
- Developers can use it as the canonical reference example

---

## Recommended Build Order

1. `debug-overlay-basics` (exists)
2. `debug-history-timeline`
3. `debug-watchdog-basics`
4. `debug-draw-basics` (complete)
5. `debug-correlation-basics`
6. `debug-session-queries`
7. `debug-subsystem-showcase`

The order climbs the same way the system itself works: face, time, alerts, world-space visualization, correlation, API usage, full-stack demonstration.

---

## Canonical Module Structure

Each proving module README should include:

| Section | Purpose |
|---------|---------|
| **What it proves** | One clear statement |
| **How it works** | Brief architecture flow |
| **Controls** | Keys/toggles if interactive |
| **What to observe** | Expected behaviors and signals |
| **Canonical pattern** | Exact usage code developers should copy |

---

## Debug Overlay Architecture

### Where the overlay code lives

The overlay presentation infrastructure belongs in **DynamisUI**, not in proving modules:

```
DynamisUI/
  dynamisui-debug/        (or org.dynamisengine.ui.debug package)
    DebugOverlay
    DebugOverlayBuilder
    DebugOverlayPanel
    DebugOverlayRow
    DebugFlagView
    DebugMiniTrend
    DebugOverlayOptions
```

### What stays in DynamisDebug

- `DebugSession`, `DebugSnapshot`, `DebugTimeline`
- `WatchdogRule`, `DebugHistory`
- Adapter model and all bridge adapters
- Debug draw queue and contracts

### What proving modules do

A proving module should:

1. Create `DebugSession`
2. Register adapters
3. Capture frame
4. Create `DebugOverlayBuilder`
5. Build panels
6. Render overlay

The proving module proves the canonical integration path, not the underlying implementation.

---

## Overlay Layout Spec

### Structure

```
+-----------------------------------------------+
| Engine Summary + Alerts                       |
+-----------------------------------------------+
| Category Grid (2-3 columns)                   |
|                                               |
| [PHYSICS] [ECS]     [RENDERING]              |
| [AUDIO]   [AI]      [SCRIPTING]              |
| [CONTENT] [INPUT]   [UI]                     |
| [ENGINE]                                      |
+-----------------------------------------------+
| Timeline Strip (optional toggle)              |
+-----------------------------------------------+
```

### Category Mapping

| Category  | Sources |
|-----------|---------|
| PHYSICS   | physics, collision |
| ECS       | ecs |
| RENDERING | gpu, lightengine, terrain, vfx, sky, scenegraph |
| AUDIO     | audio |
| CONTENT   | content |
| INPUT     | input |
| UI        | ui, localization |
| AI        | ai (multi-plane: simulation, cognition, planning, budget/LOD) |
| SCRIPTING | scripting (multi-plane: canon, oracle, chronicler, percept, degradation) |
| ENGINE    | worldengine, window |

### Color Semantics

| Color  | Meaning |
|--------|---------|
| Green  | Healthy |
| Yellow | Warning |
| Red    | Critical |
| Cyan   | Debug/neutral |
| White  | Baseline |

### Panel Density Rules

- 3-6 lines per panel max
- Truncate long values
- Align numbers for scanning
- Never overflow panel

### Multi-Plane Panels (AI & Scripting)

Planes must be vertically grouped, consistently ordered, never flattened into noise. Use prefix-based rows for v1:

```
AI Panel:
  sim.agents    20
  sim.tick      3.5ms
  cog.queue     5
  plan.replans  12
  budget.usage  85%
```

---

## Overlay View Model Types

### `DebugOverlayPanel`

```java
public record DebugOverlayPanel(
    String id,
    String title,
    DebugCategory category,
    PanelSeverity severity,
    List<DebugOverlayRow> rows,
    List<DebugFlagView> flags,
    List<DebugMiniTrend> trends,
    boolean highlighted
) {}
```

### `DebugOverlayRow`

```java
public record DebugOverlayRow(
    String label,
    String value,
    RowSeverity severity
) {}
```

### `DebugFlagView`

```java
public record DebugFlagView(
    String name,
    FlagState state   // OK, ACTIVE, WARNING, ERROR
) {}
```

### `DebugMiniTrend`

```java
public record DebugMiniTrend(
    String metricName,
    List<Double> values   // last 30-60 frames
) {}
```

### `DebugOverlayOptions`

```java
public record DebugOverlayOptions(
    boolean showTimeline,
    int trendFrameCount,
    boolean showFlags,
    boolean showAlerts
) {}
```

---

## Builder API Shape

```java
public final class DebugOverlayBuilder {
    private final DebugSession session;
    private final DebugOverlayOptions options;

    public List<DebugOverlayPanel> buildAll(DebugFrame frame) {
        // Fixed order: summary, alerts, then category panels, then timeline
    }

    // Private per-category builders:
    // buildSummaryPanel, buildAlertPanel, buildPhysicsPanel, buildEcsPanel,
    // buildRenderingPanel, buildAudioPanel, buildAiPanel, buildScriptingPanel,
    // buildContentPanel, buildInputPanel, buildUiPanel, buildEnginePanel,
    // buildTimelinePanel
}
```

### Builder Helper Methods

```java
private Optional<DebugSnapshot> firstBySource(DebugFrame frame, String source)
private List<DebugSnapshot> allByCategory(DebugFrame frame, DebugCategory category)
private String metric(DebugSnapshot snapshot, String name, String fallback)
private OptionalDouble metricDouble(DebugSnapshot snapshot, String name)
private PanelSeverity severityFor(DebugSnapshot... snapshots)
private List<DebugFlagView> flags(DebugSnapshot... snapshots)
private List<DebugMiniTrend> trend(DebugSession session, String source, String metric, int frames)
```

---

## Overlay Implementation Phases

### Phase 1 (Current)

- Static panel grid
- Summary + alerts
- Category panels
- Basic styling

### Phase 2

- Timeline strip
- Panel filtering
- Alert highlighting

### Phase 3

- Drill-down views
- Correlation hints
- Replay integration

---

## Final Goal

> Every developer can observe, query, understand, and visualize the full engine runtime using a consistent and unified diagnostic model.
