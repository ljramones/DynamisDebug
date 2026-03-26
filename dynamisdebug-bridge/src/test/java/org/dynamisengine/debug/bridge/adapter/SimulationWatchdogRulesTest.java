package org.dynamisengine.debug.bridge.adapter;

import org.dynamisengine.debug.api.DebugSeverity;
import org.dynamisengine.debug.api.WatchdogRule;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SimulationWatchdogRulesTest {

    @Test
    void allRulesPresent() {
        var rules = SimulationWatchdogRules.all();
        assertEquals(23, rules.size());
    }

    @Test
    void gpuRulesHaveEscalation() {
        var warning = SimulationWatchdogRules.gpuFrameOverBudgetWarning();
        var error = SimulationWatchdogRules.gpuFrameOverBudgetError();
        assertEquals(DebugSeverity.WARNING, warning.severity());
        assertEquals(DebugSeverity.ERROR, error.severity());
        assertTrue(warning.evaluate(10.0));
        assertFalse(warning.evaluate(6.0));
        assertTrue(error.evaluate(14.0));
        assertFalse(error.evaluate(10.0));
    }

    @Test
    void physicsRulesHaveEscalation() {
        var warning = SimulationWatchdogRules.physicsStepWarning();
        var error = SimulationWatchdogRules.physicsStepError();
        assertEquals(DebugSeverity.WARNING, warning.severity());
        assertEquals(DebugSeverity.ERROR, error.severity());
        assertTrue(warning.evaluate(10.0));
        assertFalse(warning.evaluate(6.0));
        assertEquals(60, warning.cooldownFrames());
        assertEquals(30, error.cooldownFrames());
    }

    @Test
    void aiRulesHaveEscalation() {
        var warning = SimulationWatchdogRules.aiFrameOverBudgetWarning();
        var error = SimulationWatchdogRules.aiFrameOverBudgetError();
        assertEquals(DebugSeverity.WARNING, warning.severity());
        assertEquals(DebugSeverity.ERROR, error.severity());
        assertEquals(60, warning.cooldownFrames());
        assertEquals(30, error.cooldownFrames());
    }

    @Test
    void canonCommitStall() {
        var rule = SimulationWatchdogRules.canonCommitStall();
        assertEquals("scripting", rule.source());
        assertEquals(WatchdogRule.Comparison.LESS_THAN, rule.comparison());
        assertTrue(rule.evaluate(0.0));
        assertFalse(rule.evaluate(3.0));
        assertEquals(120, rule.cooldownFrames()); // noisy rule gets longer cooldown
    }

    @Test
    void degradationTier3Spike() {
        var rule = SimulationWatchdogRules.degradationTier3Spike();
        assertEquals(DebugSeverity.ERROR, rule.severity());
        assertTrue(rule.evaluate(5.0));
        assertFalse(rule.evaluate(2.0));
        assertEquals(30, rule.cooldownFrames()); // ERROR gets short cooldown
    }

    @Test
    void aiBudgetExceeded() {
        var rule = SimulationWatchdogRules.aiBudgetExceeded();
        assertEquals("ai", rule.source());
        assertTrue(rule.evaluate(110.0));
        assertFalse(rule.evaluate(80.0));
    }

    @Test
    void cooldownsTiered() {
        // ERROR rules should have shorter cooldowns than WARNING rules
        for (var rule : SimulationWatchdogRules.all()) {
            if (rule.severity() == DebugSeverity.ERROR) {
                assertTrue(rule.cooldownFrames() <= 30,
                        rule.name() + " ERROR should have cooldown <= 30, got " + rule.cooldownFrames());
            }
        }
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
