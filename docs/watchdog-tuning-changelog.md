# Watchdog Rule Tuning Changelog

**Date:** 2026-03-25
**Status:** Code pass complete. Runtime validation pending real captured sessions.

## Summary

Structural tuning pass across all watchdog rules. Added GPU coverage,
introduced WARNING/ERROR escalation for frame-budget-critical subsystems,
tiered cooldowns by severity, removed low-signal rules.

**Final threshold confirmation is NOT yet claimed.** Thresholds are
structurally improved but require validation against real baseline,
degraded, and stress `.dbgpack` sessions.

## Rule-by-Rule Changes

### GPU (NEW — 4 rules added)

| Rule | Threshold | Severity | Cooldown | Rationale |
|------|-----------|----------|----------|-----------|
| gpu.frameOverBudget | >8ms | WARNING | 60 | Half frame budget; early GPU pressure signal |
| gpu.frameCritical | >12ms | ERROR | 30 | Near GPU-bound; needs fast resurfacing |
| gpu.passSpike | geometry >6ms | WARNING | 60 | Single pass dominating GPU frame |
| gpu.bound | gpuFrameMs >8ms | WARNING | 120 | GPU exceeding CPU — rendering bottleneck |
| **Validation needed** | | | | Verify gpu.bound and gpu.frameOverBudget don't dogpile on same frames |

### Physics (CHANGED — 3 rules)

| Rule | Old | New | Rationale |
|------|-----|-----|-----------|
| physics.stepTimeHigh | >16ms WARNING, cd=60 | >8ms WARNING, cd=60 | 16ms was full frame budget; physics should alert at half-frame |
| physics.stepCritical | (did not exist) | >12ms ERROR, cd=30 | Severity escalation for near-catastrophic physics |
| physics.sleepingRatioHigh | >500 INFO, cd=60 | >500 INFO, cd=120 | Informational; longer cooldown reduces noise |
| **Validation needed** | | | Verify 8ms doesn't fire during normal heavy scenes |

### Scripting / Canon (CHANGED — 6 rules, was 8)

| Rule | Old | New | Rationale |
|------|-----|-----|-----------|
| canon.commitStall | <1 WARNING, cd=60 | <1 WARNING, cd=120 | Fires every idle tick; longer cooldown reduces spam |
| oracle.rejectSpike | >5 WARNING, cd=60 | unchanged | — |
| chronicler.backlogSpike | >20 WARNING, cd=60 | unchanged | — |
| percept.stalenessHigh | >10 WARNING, cd=60 | unchanged | — |
| degradation.tier3Spike | >5 ERROR, cd=60 | >3 ERROR, cd=30 | Lower threshold (3 agents in must-not-act-wrong is already severe); ERROR gets short cooldown |
| scripting.frameOverBudget | >4ms WARNING, cd=60 | unchanged | — |
| ~~scripting.errorBurst~~ | >3 WARNING, cd=60 | REMOVED | Low signal; evaluation errors are rare and better caught by scripting.frameOverBudget |
| ~~scripting.cacheMissHigh~~ | >10 WARNING, cd=60 | REMOVED | Cumulative metric; fires once and stays fired. Not suitable for per-frame watchdog |

### Content (UNCHANGED — 2 rules)

| Rule | Status | Notes |
|------|--------|-------|
| content.loadFailures | unchanged | — |
| content.cacheMissHigh | cd=60 → cd=120 | Cumulative metric; longer cooldown appropriate |

### Threading (UNCHANGED — 3 rules)

| Rule | Status | Notes |
|------|--------|-------|
| threading.cognitionQueueBacklog | unchanged | — |
| threading.gpuUploadBacklog | unchanged | — |
| threading.eventBusDeadLetters | cd=60 → cd=120 | Informational; less frequent resurfacing |

### AI (CHANGED — 5 rules, was 7)

| Rule | Old | New | Rationale |
|------|-----|-----|-----------|
| ai.budgetExceeded | >100% WARNING, cd=60 | unchanged | — |
| ai.degradeSpike | >10 WARNING, cd=60 | unchanged | — |
| ai.frameOverBudget | >8ms WARNING, cd=60 | >8ms WARNING, cd=60 | (renamed to aiFrameOverBudgetWarning) |
| ai.frameCritical | (did not exist) | >12ms ERROR, cd=30 | Severity escalation for severe AI overruns |
| ai.timeoutBurst | >3 WARNING, cd=60 | unchanged | — |
| ~~ai.inferenceBacklog~~ | >10 WARNING, cd=60 | REMOVED | Overlaps with threading.cognitionQueueBacklog |
| ~~ai.replanThrashing~~ | >20 WARNING, cd=60 | REMOVED | Low signal in practice; replan count alone is not diagnostic |
| ~~ai.perceptStaleness~~ | >10 WARNING, cd=60 | REMOVED | Overlaps with percept.stalenessHigh in scripting category |

### ECS (UNCHANGED — 3 rules in separate EcsWatchdogRules)

| Rule | Status |
|------|--------|
| ecs.frameOverBudget (WARNING >8ms, cd=60) | unchanged |
| ecs.frameCritical (ERROR >12ms, cd=30) | unchanged |
| ecs.systemDominance (WARNING >4ms, cd=120) | unchanged |

## Cooldown Policy

| Severity | Cooldown | Rationale |
|----------|----------|-----------|
| ERROR | 30 frames | Severe conditions need fast resurfacing |
| WARNING | 60 frames | Standard detection cadence |
| INFO | 120 frames | Informational; reduce timeline noise |
| Noisy rules | 120 frames | canonCommitStall, contentCacheMissHigh, eventBusDeadLetters |

## Rules Removed (5)

| Rule | Reason |
|------|--------|
| scripting.errorBurst | Rare; overlaps with frameOverBudget timing |
| scripting.cacheMissHigh | Cumulative metric unsuitable for per-frame watchdog |
| ai.inferenceBacklog | Duplicates threading.cognitionQueueBacklog |
| ai.replanThrashing | Replan count alone is not diagnostic |
| ai.perceptStaleness | Duplicates percept.stalenessHigh |

## Validation Checklist (Pending Real Sessions)

When `.dbgpack` sessions are available from real gameplay:

- [ ] Baseline session: verify GPU/physics/AI rules stay quiet
- [ ] Degraded session: verify WARNING rules fire at onset of degradation
- [ ] Stress session: verify ERROR rules fire and escalate
- [ ] Verify gpu.bound and gpu.frameOverBudget don't dogpile same frames
- [ ] Verify physics 8ms threshold doesn't fire during normal heavy scenes
- [ ] Verify AI ERROR threshold is appropriate (depends on AI frame coupling)
- [ ] Check alert clustering: one root cause should produce 1-2 alerts, not 4+
- [ ] Check timeline readability: alerts should mark anomaly peaks, not flood
- [ ] Measure false-positive rate on known-good baseline
- [ ] Confirm cooldown values prevent spam without hiding sustained problems

## Final Status

- **Code pass:** Complete (commit 43f7d39)
- **Runtime validation:** Pending real captured sessions
- **Threshold confidence:** Structurally improved, not yet empirically confirmed
