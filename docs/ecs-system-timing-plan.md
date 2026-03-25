# ECS System Timing — Implementation Plan

**Date:** 2026-03-25
**Priority:** #2 in telemetry integration gaps (after GPU timing)
**Target:** DynamisECS + DynamisDebug bridge + DynamisUI mapper

## 1. Objective

Coarse ECS system timing so the runtime can answer:
- Which ECS systems consumed CPU frame time
- Which system dominated the frame
- When a system spikes or regresses
- How ECS timing correlates with GPU timing

## 2. Scope (Strict)

Per-system wall-clock execution duration ONLY. No per-entity, per-component,
allocator, GC, job/subtask, scheduler contention, or flamegraph instrumentation.

## 3. Required Metrics

```
ecs.frameTotalMs            Sum of all system execution times
ecs.systemCount             Number of systems that ran
ecs.dominantSystemTimeMs    Time of the slowest system
ecs.system.<name>.timeMs    Per-system execution time
```

## 4. Required Events

| Event | Condition | Severity | Cooldown |
|-------|-----------|----------|----------|
| ecs.frameOverBudget | ecs.frameTotalMs > 8.0ms | WARNING | 60 |
| ecs.frameOverBudget | ecs.frameTotalMs > 12.0ms | ERROR | 30 |
| ecs.systemSpike | system > median × 2.0 AND delta > 0.5ms | WARNING | 60 |
| ecs.systemDominance | system > 50% of frameTotalMs | WARNING | 120 |

## 5. Pipeline

```
ECS runtime (system execution loop)
  → per-system nanoTime measurements
  → EcsTimingTelemetryAdapter → DebugSnapshot(source="ecs")
  → DebugSession → history/timeline/watchdog
  → DebugViewSnapshotMapper (trends: ecs.frameTotalMs, dominantSystemTimeMs)
  → overlay/replay/compare/remote/export
```

## 6. Task Series

1. Seam audit — find where systems execute, determine timing insertion point
2. Timing capture — nanoTime around each system invocation
3. Snapshot adapter — bridge to DebugSnapshot
4. Watchdog rules — provisional thresholds
5. Mapper/trends — register ECS timing for sparklines
6. Runtime wiring — connect to live session
7. Validation — end-to-end verification

## 7. Constraints

- Near-zero overhead
- No per-frame allocations where practical
- No scheduler redesign
- No profiler architecture
- Stable system names across frames
- Support deterministic replay/export
