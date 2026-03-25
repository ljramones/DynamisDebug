# GPU Timing Integration Spec

**Date:** 2026-03-24
**Priority:** #1 in telemetry integration gaps
**Target:** DynamisLightEngine / engine-impl-vulkan

## 1. Goal

Instrument Vulkan GPU timestamps for coarse render pass timing and expose
results through the existing DynamisDebug observability pipeline.

## 2. Motivation

The engine is Vulkan-first. CPU-side frame timing is already captured, but
GPU-side timing is absent. Without GPU timing:
- Cannot distinguish GPU-bound from CPU-bound frames
- Cannot identify which render pass dominates GPU cost
- Cannot detect pipeline stalls or sync issues
- Frame budget alerts may misattribute CPU costs to GPU

## 3. Output Metrics (Exact Names)

```text
gpu.frameTimeMs          Total GPU frame time (independent measurement)
gpu.shadowPassMs         Shadow map generation pass
gpu.geometryPassMs       Main scene geometry pass (opaque + forward/deferred)
gpu.postProcessMs        Post-processing chain (excluding UI)
gpu.uiPassMs             Debug/UI composition pass
gpu.timingAvailable      Boolean: true if timing data is valid this frame
```

All names use the `gpu.` prefix for unambiguous namespace.

**Important:** `gpu.frameTimeMs` is measured independently (first instrumented
command to final instrumented command) and may exceed the sum of pass timings
due to uninstrumented work, barriers, clears, or presentation-adjacent GPU activity.

## 4. Scope (Strict)

Instrument these coarse passes only:
- Frame total
- Shadow pass
- Geometry pass
- Post-processing
- UI pass (when present)

**Do NOT** instrument sub-passes, micro-stages, individual draw calls,
or shader-level timing in this slice.

Missing or disabled passes:
- If a pass is not executed (e.g. shadows disabled): omit the metric or emit `0.0` consistently
- Do not leave stale values from a previous frame

## 5. Pass Timing Boundaries

| Pass | Start | End |
|------|-------|-----|
| Frame total | First instrumented GPU command | Final instrumented GPU command |
| Shadow | First shadow pass command | Last shadow pass command completion |
| Geometry | Main scene opaque/forward/deferred start | Geometry pass completion |
| Post-process | Post chain start | Post chain completion (excludes UI) |
| UI | UI render pass begin | UI render pass end |

These boundaries define exactly where timestamp writes are placed.

## 6. Result Availability Contract

- Queries written in frame N are resolved in frame N+1 or later
- **No same-frame reads**
- **No CPU waits** for unavailable results
- Reset/reuse only after results are safely consumed
- Publish either:
  - Fresh values from the resolved frame
  - Last known good values with `gpu.timingAvailable = false`
- If timestamp queries are unsupported by the device: publish `gpu.timingAvailable = false` and omit all timing metrics

## 7. Vulkan Implementation

### Query Pool

```java
VkQueryPool timestampPool;  // 2 queries per pass × passes + 2 for frame total
```

Size: `(passes × 2) + 2` timestamps. Currently 5 passes = 12 queries.

Double-buffered: one set per frame-in-flight.

### Recording

```java
// Frame start
vkCmdWriteTimestamp(cmd, VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT, pool, FRAME_START);

// Before shadow pass
vkCmdWriteTimestamp(cmd, VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT, pool, SHADOW_START);
// ... shadow commands ...
vkCmdWriteTimestamp(cmd, VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT, pool, SHADOW_END);

// ... repeat for geometry, post, UI ...

// Frame end
vkCmdWriteTimestamp(cmd, VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT, pool, FRAME_END);
```

### Readback

```java
// On frame N+1, read frame N's results
vkGetQueryPoolResults(device, pool[prevFrame], ...);
float period = physicalDeviceProperties.limits.timestampPeriod; // ns per tick
gpu.shadowPassMs = (timestamps[SHADOW_END] - timestamps[SHADOW_START]) * period / 1_000_000.0;
```

### Reset Policy

Queries for frame N:
1. Reset at beginning of frame N's recording
2. Written during frame N
3. Read at beginning of frame N+1 (or later)
4. Never read and reset in the same frame

## 8. Integration into DebugSession

### DebugSnapshot

```java
new DebugSnapshot(
    tick, now, "gpu", DebugCategory.RENDERING,
    Map.of(
        "gpu.frameTimeMs", frameMs,
        "gpu.shadowPassMs", shadowMs,
        "gpu.geometryPassMs", geometryMs,
        "gpu.postProcessMs", postMs,
        "gpu.uiPassMs", uiMs,
        "gpu.timingAvailable", available ? 1.0 : 0.0
    ),
    Map.of("gpuBound", frameMs > cpuMs),
    ""
);
```

### Adapter

Enrich existing `GpuTelemetryAdapter` or `LightEngineTelemetryAdapter`.
No new adapter class needed.

### Watchdog Rules

| Rule | Threshold | Severity | Cooldown |
|------|-----------|----------|----------|
| `gpu.frameOverBudget` | `gpu.frameTimeMs > 16.67` | WARNING | 60 frames |
| `gpu.frameCritical` | `gpu.frameTimeMs > 33.33` | ERROR | 30 frames |
| `gpu.passDominance` | Any pass > 60% of `gpu.frameTimeMs` | WARNING | 120 frames |
| `gpu.passSpike` | Pass > rolling median × 2.0 AND delta > 1.0ms | WARNING | 60 frames |

Initial thresholds are provisional; tune after first integration.

### Trend Metrics

Add to `TREND_METRICS` in `DebugViewSnapshotMapper`:
```java
"gpu", List.of("gpu.frameTimeMs", "gpu.shadowPassMs", "gpu.geometryPassMs")
```

## 9. Fallback Behavior

| Condition | Behavior |
|-----------|----------|
| Timestamp queries unsupported | `gpu.timingAvailable = false`, omit all timing |
| Pass disabled (e.g. no shadows) | Omit metric or emit `0.0` |
| Results not yet available | Publish last known good, set `gpu.timingAvailable = false` |
| Query pool creation fails | Log warning, continue without GPU timing |

Never stall the CPU waiting for GPU results.

## 10. Validation Checklist

- [ ] Vulkan validation layers remain clean
- [ ] Query count matches instrumented timestamp writes exactly
- [ ] Disabled passes do not leave stale values
- [ ] Metrics remain stable under replay
- [ ] Overlay values match captured snapshot values
- [ ] Compare/diff consumes GPU metrics without schema changes
- [ ] No CPU stall or frame drop from timing readback
- [ ] `gpu.timingAvailable` correctly reflects query state

## 11. Schema Note

No UI model changes expected. GPU metrics flow through existing
`DebugSnapshot` → `DebugViewSnapshot` → overlay pipeline. The `Rendering`
category panel will automatically show GPU timing alongside existing
rendering metrics. New metric grouping can be considered later but is
not required for this slice.

## 12. Files Expected to Change

| File | Change |
|------|--------|
| `VulkanContext.java` | Query pool creation, timestamp recording, readback |
| `VulkanFrameCommandOrchestrator.java` | Timestamp writes around passes |
| `VulkanBackendResources.java` | Query pool handles |
| `GpuTelemetryAdapter` or `LightEngineTelemetryAdapter` | Enrich with GPU timing |
| `DebugViewSnapshotMapper.java` | Add GPU trend metrics |
| Watchdog rule registration | Add GPU threshold rules |

## 13. Non-Goals

- Sub-pass or draw-call level timing
- Shader profiling
- GPU memory allocation tracking (separate concern)
- OpenGL timing parity (can follow later)
- Performance optimization of the timing system itself
