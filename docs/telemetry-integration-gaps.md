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

### 1. GPU Timing (CRITICAL)

**Current:** CPU-side frame time only. No GPU timing queries.

**Missing:**
- Per-pass GPU timing (shadow, geometry, fog, smoke, post-process)
- GPU frame total vs CPU frame (identifies GPU-bound vs CPU-bound)
- Pipeline stall / sync point detection
- Upload bandwidth and resource churn per frame
- Shader compilation / cache miss events

**What integration should produce:**
- Metrics: `gpu.shadowMs`, `gpu.geometryMs`, `gpu.postMs`, `gpu.totalMs`, `gpu.uploadBytes`
- Events: `gpu.frameBudgetExceeded`, `gpu.syncStall`, `gpu.shaderCacheMiss`
- Watchdog rules: `gpu.totalMs > 12ms`, `gpu.uploadBytes > 10MB`

**Implementation path:**
- Vulkan: `vkCmdWriteTimestamp` before/after each render pass
- OpenGL: `glQueryCounter` with `GL_TIMESTAMP`
- Read back results with 1-frame latency (query pool double-buffer)

**Adapter:** `GpuTelemetryAdapter` exists but needs per-pass timing data

---

### 2. ECS System Timing (HIGH)

**Current:** Entity count, churn (create/destroy). No per-system execution time.

**Missing:**
- Per-system execution time per tick
- Hot system identification (which system dominates the frame)
- Component distribution / archetype stats
- Query cost breakdown
- Entity churn rate (creates + destroys per second)

**What integration should produce:**
- Metrics: `ecs.system.<name>.ms`, `ecs.totalSystemMs`, `ecs.queryCount`, `ecs.archetypeCount`
- Events: `ecs.systemSlow` (when a single system exceeds budget)
- Watchdog rules: `ecs.totalSystemMs > 4ms`, `ecs.system.physics.ms > 2ms`

**Implementation path:**
- Add timing hooks around each system's `tick()` in the ECS executor
- Expose via `EcsTelemetryAdapter` (already exists, needs enrichment)

---

### 3. Physics Subsystem Depth (HIGH)

**Current:** Step time, contacts, bodies (shallow).

**Missing:**
- Broadphase vs narrowphase timing breakdown
- Constraint solver iteration count and cost
- Island count and sizes
- Penetration depth / resolution stats
- CCD event counts
- Character controller state

**What integration should produce:**
- Metrics: `physics.broadphaseMs`, `physics.narrowphaseMs`, `physics.solverMs`, `physics.islands`, `physics.avgPenetration`
- Events: `physics.constraintExplosion`, `physics.unstableTimestep`, `physics.ccdOverflow`
- Watchdog rules: `physics.solverMs > 3ms`, `physics.avgPenetration > 0.1`

**Implementation path:**
- ODE4J and Jolt backends expose timing internally; need to surface it
- `PhysicsTelemetryAdapter` exists but needs richer input data

---

### 4. Audio DSP Pipeline (MEDIUM-HIGH)

**Current:** Voice count, DSP budget percent, ring buffer stats, underruns.

**Missing:**
- Per-stage DSP timing (mix, spatialize, effects, master)
- Voice allocation churn (voices started/stopped per frame)
- Per-bus load breakdown
- Spatialization cost per voice
- Buffer latency trend

**What integration should produce:**
- Metrics: `audio.mixMs`, `audio.spatializeMs`, `audio.effectsMs`, `audio.voiceChurn`
- Events: `audio.underrunRisk` (before actual underrun), `audio.voiceSteal`
- Watchdog rules: `audio.totalDspMs > 4ms`, `audio.voiceChurn > 10/frame`

**Implementation path:**
- CoreAudio/ALSA backends already have internal timing
- `AudioTelemetryAdapter` exists, needs DSP-stage breakdown

---

### 5. Rendering Pipeline Detail (MEDIUM-HIGH)

**Current:** FPS, draw calls, triangles, CPU/GPU ms (aggregate).

**Missing:**
- Per-pass draw call count and triangle count
- Material/shader switch count (batching efficiency)
- Occlusion culling effectiveness (visible vs submitted)
- Texture streaming bandwidth
- LOD transition stats

**What integration should produce:**
- Metrics: `render.shadowDrawCalls`, `render.geometryDrawCalls`, `render.shaderSwitches`, `render.cullEfficiency`
- Events: `render.lod.transition`, `render.texture.streamSpike`

**Implementation path:**
- Frame graph already tracks pass counts; expose them
- `LightEngineTelemetryAdapter` exists, needs pass-level granularity

---

### 6. Scripting Execution (MEDIUM)

**Current:** Canon commits, oracle rejects, chronicler pending, percept staleness (5 planes).

**Missing:**
- Per-script execution time
- Expression compilation cache stats (hit/miss rate)
- Allocation pressure during script evaluation
- Script error/exception counts

**What integration should produce:**
- Metrics: `scripting.evalMs`, `scripting.cacheHitRate`, `scripting.errorCount`
- Events: `scripting.evalSlow`, `scripting.compileFail`

**Implementation path:**
- MVEL3 compiler already has timing; expose it
- `ScriptingTelemetryAdapter` exists with 5 planes; add execution metrics

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

### 8. Threading / Job System (OPPORTUNITY)

**Current:** No threading telemetry.

**Missing:**
- Thread utilization per frame
- Job queue depth and completion time
- Contention / lock wait time
- Work stealing events

**What integration should produce:**
- Metrics: `thread.utilization`, `thread.queueDepth`, `thread.contentionMs`
- Events: `thread.starvation`, `thread.contention`

**Implementation path:**
- Requires instrumentation in the job scheduler (if one exists)
- New `ThreadTelemetryAdapter`

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
