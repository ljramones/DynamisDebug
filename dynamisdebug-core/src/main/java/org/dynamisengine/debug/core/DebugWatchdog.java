package org.dynamisengine.debug.core;

import org.dynamisengine.debug.api.DebugCategory;
import org.dynamisengine.debug.api.DebugSnapshot;
import org.dynamisengine.debug.api.WatchdogRule;
import org.dynamisengine.debug.api.event.DebugEvent;
import org.dynamisengine.debug.api.event.DebugEventSink;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Evaluates {@link WatchdogRule}s against frame snapshots and fires
 * {@link DebugEvent}s when thresholds are crossed.
 *
 * <p>This is the reactive layer that turns passive observation into
 * active alerting. The engine can subscribe to these events to
 * compensate for issues automatically.
 *
 * <p>Features:
 * <ul>
 *   <li>Per-rule cooldown to prevent event spam</li>
 *   <li>Cross-subsystem correlation rules (evaluate multiple sources)</li>
 *   <li>Thread-safe rule registration</li>
 * </ul>
 */
public final class DebugWatchdog {

    private final DebugEventSink eventSink;
    private final List<WatchdogRule> rules = new CopyOnWriteArrayList<>();
    private final Map<String, Long> lastFiredFrame = new ConcurrentHashMap<>();

    public DebugWatchdog(DebugEventSink eventSink) {
        this.eventSink = eventSink;
    }

    public void addRule(WatchdogRule rule) {
        rules.add(rule);
    }

    public void removeRule(String ruleName) {
        rules.removeIf(r -> r.name().equals(ruleName));
        lastFiredFrame.remove(ruleName);
    }

    public int ruleCount() { return rules.size(); }

    /**
     * Evaluate all rules against the current frame's snapshots.
     * Fires events for any triggered rules that are not in cooldown.
     *
     * @param frameNumber current frame number
     * @param snapshots   the merged frame snapshot map (source → snapshot)
     * @return list of events that were fired this evaluation
     */
    public List<DebugEvent> evaluate(long frameNumber, Map<String, DebugSnapshot> snapshots) {
        List<DebugEvent> fired = new ArrayList<>();

        for (WatchdogRule rule : rules) {
            // Check cooldown
            Long lastFired = lastFiredFrame.get(rule.name());
            if (lastFired != null && (frameNumber - lastFired) < rule.cooldownFrames()) {
                continue;
            }

            DebugSnapshot snapshot = snapshots.get(rule.source());
            if (snapshot == null) continue;

            Double value = snapshot.metrics().get(rule.metricName());
            if (value == null) continue;

            if (rule.evaluate(value)) {
                String detail = String.format("%s: %s = %.2f (threshold: %.2f)",
                        rule.message(), rule.metricName(), value, rule.threshold());

                DebugEvent event = new DebugEvent(
                        frameNumber, System.currentTimeMillis(),
                        rule.source(), snapshot.category(),
                        rule.severity(), rule.name(), detail
                );

                eventSink.submit(event);
                fired.add(event);
                lastFiredFrame.put(rule.name(), frameNumber);
            }
        }

        return fired;
    }

    /** Clear all cooldown state (e.g. after a pause or reset). */
    public void resetCooldowns() {
        lastFiredFrame.clear();
    }

    public List<WatchdogRule> rules() {
        return List.copyOf(rules);
    }
}
