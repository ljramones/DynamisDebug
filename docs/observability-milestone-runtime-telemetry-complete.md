# Observability Milestone: Runtime Telemetry Integration Complete

**Date:** 2026-03-25
**Status:** Complete — all existing subsystems instrumented

## Milestone Summary

The DynamisDebug observability platform now has deep runtime telemetry across
every major engine subsystem. All 11 actionable integration gaps from the
original roadmap are closed. The sole remaining item (#12 Networking) is
correctly deferred — the networking subsystem does not yet exist.

The engine can now explain frame behavior across CPU subsystem cost, GPU pass
cost, physics phase cost, audio stage cost, scripting behavior, AI decision
cost, job/thread pressure, rendering submission detail, content/cache/load
behavior, session-to-session regression, and remote/exportable telemetry streams.

## Scorecard

| # | Gap | Status | Commits |
|---|-----|--------|---------|
| 1 | GPU timing | DONE | a86a741, 448a5a9, 15d499e, cd61e27 |
| 2 | ECS system timing | DONE | EcsTimingTelemetryAdapter + SubsystemCoordinator |
| 3 | Physics depth | DONE | Ode4jStepLoop + JoltPhysicsWorld per-phase timing |
| 4 | Audio DSP stages | DONE | e6d4f34, f8f5f49 |
| 5 | Rendering pipeline detail | DONE | c88082d, f6bbefa |
| 6 | Scripting execution | DONE | 1aafba3, 683e6aa |
| 7 | AI decision timing | DONE | ed5ef30, 2427121 |
| 8 | Threading / job system | DONE | aff2d8e |
| 9 | Content / asset pipeline | DONE | 693603a, bff3189 |
| 10 | Session comparison | DONE | Prior session |
| 11 | Remote telemetry | DONE | Prior session |
| 12 | Networking | DEFERRED | Subsystem does not exist |

## Repos Touched

| Repository | Layer | Changes |
|------------|-------|---------|
| DynamisDebug | 1 | Adapter enrichments, new adapters, watchdog rules, gaps doc |
| DynamisLightEngine | 4 | EngineStats extended, VulkanContext per-variant counting, debug draw |
| DynamisAudio | 4 | SoftwareMixer per-stage timing, AudioTelemetry extended |
| DynamisScripting | 5 | DslCompiler cache counters, TriggerEvaluator errors, RuntimeTick timing, JPMS fix |
| DynamisAI | 5 | DefaultCognitionService completed/timeout counters |
| DynamisPhysics | 5 | Per-phase timing in ODE4J + Jolt backends |
| DynamisContent | 3 | DefaultAssetCache hit/miss, DefaultAssetManager load timing |
| DynamisWorldEngine | 7 | SubsystemCoordinator timing, FakeEngineRuntime compat |

## Adapter Inventory

### New Adapters Created

| Adapter | Source | Telemetry |
|---------|--------|-----------|
| `EcsTimingTelemetryAdapter` | SubsystemCoordinator | Per-system timing, frame total, dominant system |
| `ThreadingTelemetryAdapter` | Distributed pools | Event bus, AI cognition/nav, GPU upload stats |

### Existing Adapters Enriched With Real Runtime Data

| Adapter | New Metrics Added |
|---------|-------------------|
| `GpuTimingTelemetryAdapter` | Per-pass GPU timing (shadow, geometry, post, UI) |
| `LightEngineTelemetryAdapter` | Per-pass draw calls, pipeline switches, per-variant draws, submitted/visible |
| `AudioTelemetryAdapter` | voiceRenderNanos, busProcessNanos, deviceWriteNanos |
| `PhysicsTelemetryAdapter` | broadPhaseNs, solverNs, integrationNs (ODE4J + Jolt) |
| `ScriptingTelemetryAdapter` | chroniclerNanos, evaluationErrors, cacheHits, cacheMisses |
| `AiTelemetryAdapter` | ExecutionTelemetry plane: frameTotalMs, hottestTaskMs, completedInferences, timeouts |
| `ContentTelemetryAdapter` | lastLoadMs, lastLoadedAssetId, loadSlow flag |

### Existing Adapters Unchanged (Already Complete)

| Adapter | Why Unchanged |
|---------|---------------|
| `CollisionTelemetryAdapter` | Contacts/colliders/cache already sufficient |
| `AnimisTelemetryAdapter` | Animation state/transitions already sufficient |
| `EcsTelemetryAdapter` | Entity count/churn — timing handled by new EcsTimingTelemetryAdapter |
| `SceneGraphTelemetryAdapter` | Nodes/renderables/batches already sufficient |
| `TerrainTelemetryAdapter` | Chunks/foliage/GPU time already sufficient |
| `VfxTelemetryAdapter` | VFX counts/budget already sufficient |
| `SkyTelemetryAdapter` | Sky/weather/time-of-day already sufficient |
| `InputTelemetryAdapter` | Devices/events/gamepads already sufficient |
| `WindowTelemetryAdapter` | Dimensions/DPI/focus already sufficient |
| `UiTelemetryAdapter` | Layers/widgets/FPS already sufficient |
| `LocalizationTelemetryAdapter` | Locale/missing keys already sufficient |
| `WorldEngineTelemetryAdapter` | Engine coordination state already sufficient |

## Watchdog Rule Inventory (22 rules)

### Physics (2)
- `physics.stepTimeHigh` — step exceeds frame budget
- `physics.sleepingRatioHigh` — large sleeping body count

### Scripting / Canon (8)
- `canon.commitStall` — no commits when expected
- `oracle.rejectSpike` — validation failures spiking
- `chronicler.backlogSpike` — world event backlog high
- `percept.stalenessHigh` — stale percepts accumulating
- `degradation.tier3Spike` — agents in must-not-act-wrong tier
- `scripting.frameOverBudget` — tick over 4ms
- `scripting.errorBurst` — evaluation errors spiking
- `scripting.cacheMissHigh` — DSL cache misses high

### Content (2)
- `content.loadFailures` — asset load failures accumulating
- `content.cacheMissHigh` — cache miss rate high

### Threading (3)
- `threading.cognitionQueueBacklog` — AI queue depth high
- `threading.gpuUploadBacklog` — GPU upload backlog growing
- `threading.eventBusDeadLetters` — dead letters accumulating

### AI (7)
- `ai.budgetExceeded` — frame budget exceeded
- `ai.degradeSpike` — many tasks degraded
- `ai.inferenceBacklog` — inference queue backing up
- `ai.replanThrashing` — plan thrashing detected
- `ai.perceptStaleness` — perception running stale
- `ai.frameOverBudget` — frame exceeds 8ms
- `ai.timeoutBurst` — inference timeouts spiking

### ECS (3, in EcsWatchdogRules)
- `ecs.frameOverBudget` WARNING — frame total >8ms
- `ecs.frameOverBudget` ERROR — frame total >12ms
- `ecs.systemDominance` — single system >4ms

## Telemetry Categories Now Available

| Category | What It Explains |
|----------|-----------------|
| **GPU timing** | Per-pass GPU cost (shadow/geometry/post/UI), GPU-bound detection |
| **CPU subsystem cost** | Per-system execution time, dominant system, frame total |
| **Physics phases** | Broadphase vs solver vs integration, per-backend (ODE4J + Jolt) |
| **Audio DSP** | Voice render vs bus process vs device write stage timing |
| **Rendering detail** | Per-pass draw calls, pipeline switches, per-variant draws, submitted vs visible |
| **Scripting execution** | Chronicler timing, DSL cache efficiency, evaluation errors |
| **AI decisions** | Frame total, hottest task, completed/timeout inferences, budget usage |
| **Threading** | Event bus metrics, AI cognition/nav queue depth, GPU upload health |
| **Content/assets** | Cache hit/miss rates, load timing, failure tracking |
| **Session comparison** | Regression scoring, auto-matched windows, weighted metrics |
| **Remote/export** | TCP streaming, NDJSON export, .dbgpack bundles |

## Architectural Lessons Learned

1. **Enrich existing adapters, don't create parallel ones.** Most gaps were closed by
   adding fields to existing snapshot records and extracting new metrics in existing
   adapters. Only 2 genuinely new adapters were needed.

2. **Subsystems stay debug-agnostic.** Counters and timing fields were added as public
   state on subsystem classes (AtomicLong, volatile fields). No subsystem imports
   DynamisDebug.

3. **Watchdog rules grow incrementally.** Started with 12, ended with 22+3(ECS). Each
   gap added 2-3 rules using the existing `WatchdogRule.above()`/`below()` factory
   methods. No new watchdog machinery needed.

4. **Backwards-compatible record extension.** Every extended record kept a
   backwards-compatible constructor so existing callers didn't break.

5. **SpotBugs catches real issues.** Volatile increments were flagged immediately;
   switching to AtomicLong was the correct fix.

6. **The platform amplifies automatically.** Every new metric flows through history,
   timeline, trends, queries, overlay, replay, comparison, and export without any
   additional wiring.

## Integration Pattern for Future Subsystems

When a new subsystem needs observability:

1. **Add counters/timing to the subsystem** — AtomicLong/volatile fields with public
   accessors. No debug dependencies.
2. **Define or extend a snapshot record** — in the existing adapter or a new one.
3. **Extract metrics** — in `extractMetrics()` method of the adapter.
4. **Add flags** — boolean conditions that indicate problems.
5. **Add watchdog rules** — `WatchdogRule.above()`/`below()` with source matching the
   adapter's `subsystemName()`.
6. **Submit telemetry** — via `bridge.submitTelemetry(name, snapshot)` during frame
   capture.

The platform handles history, timeline, trends, queries, watchdog evaluation,
overlay presentation, replay, comparison, remote streaming, and export automatically.

## Deferred: Networking

When the networking subsystem is built, it should follow the same pattern:

- `NetworkTelemetryAdapter` with metrics for latency, jitter, bandwidth, dropped
  packets, state sync divergence
- Watchdog rules for latency spikes, packet loss, bandwidth saturation
- No networking-specific observability framework

## Next Evolution Areas

These are not gaps — they are refinements to an already-working platform:

- **Threshold tuning** — watchdog thresholds are initial estimates; calibrate from
  real gameplay sessions
- **Dashboard/layout polish** — overlay panel sizing and trend selection for the new
  metric families
- **Regression scoring calibration** — weight the new metrics appropriately in
  session comparison
- **Per-entity drill-down** — the AI adapter has `hottestTaskId`; similar per-entity
  identification could be added to other subsystems
- **Networking integration** — when the subsystem exists
