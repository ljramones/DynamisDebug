# Telemetry Integration Gaps

**Date:** 2026-03-24
**Context:** The observability platform is structurally complete. The gap is
not in the platform itself — it's in the depth and quality of telemetry
produced by each subsystem.

## Current State

19 bridge adapters exist across Layers 2–7. However, most produce shallow
metrics (entity counts, health flags) rather than deep diagnostic signals
(per-phase timing, causal events, allocation pressure).

## Priority-Ordered Integration Gaps

### 1. GPU Timing — DONE ✓

Completed 2026-03-25. Vulkan double-buffered timestamp queries (N+1 readback),
per-pass timing (shadow/geometry/post/ui), `GpuTimingTelemetryAdapter` →
DebugSnapshot → overlay/replay/compare. See `docs/gpu-timing-integration-spec.md`.

---

### 2. ECS System Timing — DONE ✓

Completed 2026-03-25. nanoTime around each sub.tick() in SubsystemCoordinator,
`EcsTimingTelemetryAdapter` → DebugSnapshot(source="ecs"), 3 watchdog rules
(frameOverBudget WARNING/ERROR, systemDominance). See `docs/ecs-system-timing-plan.md`.

---

### 3. Physics Subsystem Depth — DONE ✓

Completed 2026-03-25. Per-phase timing in both backends: ODE4J (broadPhaseNs,
solverNs, integrationNs accumulated across substeps) and Jolt (solverMs,
integrationMs). PhysicsStats fields populated, PhysicsTelemetryAdapter enriched.

---

### 4. Audio DSP Pipeline — DONE ✓

Completed 2026-03-25. Per-stage nanoTime in SoftwareMixer.renderBlock() for
3 stages: voiceRender, busProcess, deviceWrite. Zero-allocation (volatile
primitive fields). AudioTelemetry record extended, AudioTelemetryAdapter enriched.

---

### 5. Rendering Pipeline Detail — DONE ✓

Completed 2026-03-25. Per-pass draw call counts (shadow, geometry, post),
pipeline/shader switch count, submitted vs visible object counts, per-variant
draw counts (static, morph, skinned, instanced). EngineStats extended with 9
new fields, VulkanContext counts variants from mesh list, LightEngineTelemetryAdapter
extracts all new metrics. Flag: highPipelineSwitches (>5).

---

### 6. Scripting Execution — DONE ✓

Completed 2026-03-25. Per-phase timing (chronicler execution nanos), DSL cache
hit/miss tracking (AtomicLong in DslCompiler), evaluation error counting
(AtomicLong in TriggerEvaluator). RuntimeTickResult extended with 5 new fields.
ScriptingTelemetryAdapter enriched with execution metrics. 3 new watchdog rules:
frameOverBudget (>4ms), errorBurst (>3), cacheMissHigh (>10). Also fixed JPMS
module name mismatch (mvel3 -> org.mvel3).

---

### 7. AI Decision Timing (MEDIUM)

**Current:** 4 planes (simulation, cognition, planning, budget/LOD).

**Missing:**
- Per-agent planning time
- Perception update cost
- Navigation pathfinding cost
- Behavior tree tick time distribution

**What integration should produce:**
- Metrics: `ai.planningMs`, `ai.perceptionMs`, `ai.navMs`, `ai.agentsOverBudget`
- Events: `ai.planningTimeout`, `ai.perceptionStale`

**Implementation path:**
- `AiTelemetryAdapter` exists with 4 planes; enrich with timing data

---

### 8. Threading / Job System — DONE ✓

Completed 2026-03-25. No central job system exists — engine uses distributed
executor pools. New ThreadingTelemetryAdapter aggregates across: event bus
dispatch pool (BusMetrics), AI cognition virtual thread executor (queue depth,
completed, timeouts), AI navigation executor, GPU upload pool. AI
DefaultCognitionService enriched with completedCount/timeoutCount AtomicLong
counters. 3 new watchdog rules: cognitionQueueBacklog (>8), gpuUploadBacklog
(>5), eventBusDeadLetters (>10).

---

### 9. Content / Asset Pipeline (LOW)

**Current:** Cache hit/miss, failed resolutions.

**Missing:**
- Asset load time per type
- Streaming bandwidth
- Hot reload events

**Implementation path:**
- `ContentTelemetryAdapter` exists; enrich with timing

---

### 10. Networking (FUTURE)

**Not yet built.** When networking is added:
- Packet latency/jitter
- Bandwidth spikes
- Dropped packets
- State sync divergence
- New `NetworkTelemetryAdapter`

---

## Recommended Implementation Order

1. **GPU timing** — highest diagnostic value, Vulkan-first engine should have this
2. **ECS system timing** — most performance issues originate here
3. **Physics depth** — hard to debug without per-phase timing
4. **Audio DSP stages** — subtle timing-sensitive issues
5. **Rendering detail** — pass-level granularity
6. **Scripting/AI** — enrich existing plane-based adapters
7. **Threading** — new capability, high value once job system matures
8. **Content** — incremental enrichment
9. **Networking** — when the subsystem exists

## Pattern for Each Integration

For every subsystem:
1. Define the metrics that a senior engineer would want
2. Define the events that indicate problems
3. Surface through the existing `TelemetryAdapter<T>` for that subsystem
4. Add watchdog rules for the critical thresholds
5. Verify it appears in the overlay with trends and alerts

The observability platform handles everything else automatically
(history, timeline, trends, queries, export, compare, replay).

## Integration Effort Estimates

| Gap | Effort | New adapter? | New API? |
|-----|--------|-------------|----------|
| GPU timing | Medium-High | Enrich existing | Yes (timing queries) |
| ECS system timing | Medium | Enrich existing | Yes (system hooks) |
| Physics depth | Medium | Enrich existing | Depends on backend |
| Audio DSP stages | Low-Medium | Enrich existing | Minor |
| Rendering detail | Low-Medium | Enrich existing | Minor |
| Scripting execution | Low | Enrich existing | Minor |
| AI decision timing | Low | Enrich existing | Minor |
| Threading | Medium | New adapter | New capability |
| Content enrichment | Low | Enrich existing | Minor |
| Networking | High | New adapter | New subsystem |
