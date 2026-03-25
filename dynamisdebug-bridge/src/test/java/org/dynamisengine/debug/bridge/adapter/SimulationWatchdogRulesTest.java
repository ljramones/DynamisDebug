package org.dynamisengine.debug.bridge.adapter;

import org.dynamisengine.debug.api.DebugSeverity;
import org.dynamisengine.debug.api.WatchdogRule;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SimulationWatchdogRulesTest {

    @Test
    void allRulesPresent() {
        var rules = SimulationWatchdogRules.all();
        assertEquals(15, rules.size());
    }

    @Test
    void physicsRules() {
        var step = SimulationWatchdogRules.physicsStepTimeHigh();
        assertEquals("physics", step.source());
        assertEquals("stepTimeMs", step.metricName());
        assertTrue(step.evaluate(20.0));
        assertFalse(step.evaluate(10.0));
    }

    @Test
    void canonCommitStall() {
        var rule = SimulationWatchdogRules.canonCommitStall();
        assertEquals("scripting", rule.source());
        assertEquals(WatchdogRule.Comparison.LESS_THAN, rule.comparison());
        assertTrue(rule.evaluate(0.0)); // no commits = fires
        assertFalse(rule.evaluate(3.0)); // commits = ok
    }

    @Test
    void oracleRejectSpike() {
        var rule = SimulationWatchdogRules.oracleRejectSpike();
        assertEquals("scripting", rule.source());
        assertTrue(rule.evaluate(10.0));
        assertFalse(rule.evaluate(2.0));
    }

    @Test
    void degradationTier3Spike() {
        var rule = SimulationWatchdogRules.degradationTier3Spike();
        assertEquals(DebugSeverity.ERROR, rule.severity());
        assertTrue(rule.evaluate(10.0));
        assertFalse(rule.evaluate(3.0));
    }

    @Test
    void aiBudgetExceeded() {
        var rule = SimulationWatchdogRules.aiBudgetExceeded();
        assertEquals("ai", rule.source());
        assertTrue(rule.evaluate(110.0));
        assertFalse(rule.evaluate(80.0));
    }

    @Test
    void aiReplanThrashing() {
        var rule = SimulationWatchdogRules.aiReplanThrashing();
        assertTrue(rule.evaluate(25.0));
        assertFalse(rule.evaluate(5.0));
    }

    @Test
    void allRulesHaveCooldown() {
        for (var rule : SimulationWatchdogRules.all()) {
            assertTrue(rule.cooldownFrames() > 0, rule.name() + " should have cooldown");
        }
    }

    @Test
    void allRulesHaveSource() {
        for (var rule : SimulationWatchdogRules.all()) {
            assertNotNull(rule.source(), rule.name() + " should have source");
            assertFalse(rule.source().isEmpty(), rule.name() + " source should not be empty");
        }
    }
}
