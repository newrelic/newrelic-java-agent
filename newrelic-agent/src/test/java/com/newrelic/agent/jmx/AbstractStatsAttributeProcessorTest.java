package com.newrelic.agent.jmx;

import com.newrelic.agent.MetricNames;
import com.newrelic.agent.stats.StatsEngine;
import org.junit.Test;

import static org.junit.Assert.*;

import javax.management.Attribute;
import javax.management.ObjectInstance;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class AbstractStatsAttributeProcessorTest {
    @Test
    public void isBuiltInMetric_nameStartsWithJmxCustom_returnsTrue() {
        assertTrue(TestAbstractStatsAttributeProcessor.isBuiltInMetric(MetricNames.JMX_CUSTOM + "/test"));
    }

    @Test
    public void isBuiltInMetric_nameDoesNotStartsWithJmxCustom_returnsFalse() {
        assertFalse(TestAbstractStatsAttributeProcessor.isBuiltInMetric("foo/test"));
    }

    @Test
    public void isBuiltInMetric_nameIsNull_returnsFalse() {
        assertFalse(TestAbstractStatsAttributeProcessor.isBuiltInMetric(null));
    }

    @Test
    public void getTimeUnit_returnsTimeUnitBasedOnString() {
        assertEquals(TimeUnit.HOURS, TestAbstractStatsAttributeProcessor.getTimeUnit("HOUR"));
        assertEquals(TimeUnit.MINUTES, TestAbstractStatsAttributeProcessor.getTimeUnit("MINUTE"));
        assertEquals(TimeUnit.SECONDS, TestAbstractStatsAttributeProcessor.getTimeUnit("SECOND"));
        assertEquals(TimeUnit.MILLISECONDS, TestAbstractStatsAttributeProcessor.getTimeUnit("MILLISECOND"));
        assertEquals(TimeUnit.MICROSECONDS, TestAbstractStatsAttributeProcessor.getTimeUnit("MICROSECOND"));
        assertEquals(TimeUnit.NANOSECONDS, TestAbstractStatsAttributeProcessor.getTimeUnit("NANOSECOND"));

        assertEquals(TimeUnit.MILLISECONDS, TestAbstractStatsAttributeProcessor.getTimeUnit("FOO"));
    }

    private static class TestAbstractStatsAttributeProcessor extends AbstractStatsAttributeProcessor {
        @Override
        public boolean process(StatsEngine statsEngine, ObjectInstance instance, Attribute attribute, String metricName, Map<String, Float> values) {
            return false;
        }
    }
}
