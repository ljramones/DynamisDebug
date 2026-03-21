package org.dynamisengine.debug.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WatchdogRuleTest {

    @Test
    void greaterThanEvaluation() {
        var rule = WatchdogRule.above("test", "src", "metric", 10.0,
                DebugSeverity.WARNING, "msg");
        assertTrue(rule.evaluate(15.0));
        assertFalse(rule.evaluate(10.0));
        assertFalse(rule.evaluate(5.0));
    }

    @Test
    void lessThanEvaluation() {
        var rule = WatchdogRule.below("test", "src", "metric", 0.5,
                DebugSeverity.WARNING, "msg");
        assertTrue(rule.evaluate(0.3));
        assertFalse(rule.evaluate(0.5));
        assertFalse(rule.evaluate(0.8));
    }

    @Test
    void equalsEvaluation() {
        var rule = new WatchdogRule("test", "src", "metric", 42.0,
                WatchdogRule.Comparison.EQUALS, DebugSeverity.INFO, "msg", 0);
        assertTrue(rule.evaluate(42.0));
        assertFalse(rule.evaluate(43.0));
    }

    @Test
    void notEqualsEvaluation() {
        var rule = new WatchdogRule("test", "src", "metric", 0.0,
                WatchdogRule.Comparison.NOT_EQUALS, DebugSeverity.INFO, "msg", 0);
        assertTrue(rule.evaluate(1.0));
        assertFalse(rule.evaluate(0.0));
    }

    @Test
    void aboveFactoryDefaults() {
        var rule = WatchdogRule.above("n", "s", "m", 5.0, DebugSeverity.ERROR, "msg");
        assertEquals("n", rule.name());
        assertEquals("s", rule.source());
        assertEquals("m", rule.metricName());
        assertEquals(5.0, rule.threshold());
        assertEquals(WatchdogRule.Comparison.GREATER_THAN, rule.comparison());
        assertEquals(DebugSeverity.ERROR, rule.severity());
        assertEquals(60, rule.cooldownFrames());
    }
}
