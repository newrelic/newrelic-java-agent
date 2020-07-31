/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.jmx;

import com.newrelic.agent.jmx.metrics.JmxAction;
import com.newrelic.agent.jmx.metrics.JmxMetric;
import com.newrelic.agent.stats.StatsEngine;
import com.newrelic.agent.stats.StatsEngineImpl;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class JmxMetricTest {

    @Test
    public void jmxMetricRecordStatsSimple() {
        StatsEngine stats = new StatsEngineImpl();
        JmxMetric metric = JmxMetric.create("hello", JmxType.SIMPLE);
        Map<String, Float> values = new HashMap<>();
        values.put("hello", 5f);
        metric.recordSingleMBeanStats(stats, "Jmx/Sample/", values);
        Assert.assertEquals(1, stats.getStats("Jmx/Sample/hello").getCallCount());
        Assert.assertEquals(5f, stats.getStats("Jmx/Sample/hello").getTotal(), 0);

        values.clear();
        values.put("hello", 5f);
        metric.recordSingleMBeanStats(stats, "Jmx/Sample/", values);
        Assert.assertEquals(2, stats.getStats("Jmx/Sample/hello").getCallCount());
        Assert.assertEquals(10f, stats.getStats("Jmx/Sample/hello").getTotal(), 0);
    }

    @Test
    public void jmxMetricRecordStatsSimpleForMultiBean() {
        StatsEngine stats = new StatsEngineImpl();
        JmxMetric metric = JmxMetric.create("hello", JmxType.SIMPLE);
        Map<String, Float> values = new HashMap<>();
        values.put("hello", 5f);
        Map<String, Float> actual = new HashMap<>();
        metric.applySingleMBean("Jmx/Sample/", values, actual);
        metric.recordMultMBeanStats(stats, actual);
        Assert.assertEquals(1, stats.getStats("Jmx/Sample/hello").getCallCount());
        Assert.assertEquals(5f, stats.getStats("Jmx/Sample/hello").getTotal(), 0);

        values.clear();
        actual.clear();
        values.put("hello", 5f);
        metric.applySingleMBean("Jmx/Sample/", values, actual);
        metric.recordMultMBeanStats(stats, actual);
        Assert.assertEquals(2, stats.getStats("Jmx/Sample/hello").getCallCount());
        Assert.assertEquals(10f, stats.getStats("Jmx/Sample/hello").getTotal(), 0);
    }

    @Test
    public void jmxMetricRecordStatsSimpleForMultiBeanAddition() {
        StatsEngine stats = new StatsEngineImpl();
        JmxMetric metric = JmxMetric.create("hello", JmxType.SIMPLE);
        Map<String, Float> values = new HashMap<>();
        values.put("hello", 5f);
        Map<String, Float> actual = new HashMap<>();
        metric.applySingleMBean("Jmx/Sample/", values, actual);
        Assert.assertEquals(Float.valueOf(5), actual.get("Jmx/Sample/hello"));

        values.clear();
        // do not clear actual
        values.put("hello", 7f);
        metric.applySingleMBean("Jmx/Sample/", values, actual);
        Assert.assertEquals(Float.valueOf(12), actual.get("Jmx/Sample/hello"));
        metric.recordMultMBeanStats(stats, actual);
        Assert.assertEquals(1, stats.getStats("Jmx/Sample/hello").getCallCount());
        Assert.assertEquals(12f, stats.getStats("Jmx/Sample/hello").getTotal(), 0);
    }

    @Test
    public void jmxMetricRecordStatsMonotonically() {
        StatsEngine stats = new StatsEngineImpl();
        JmxMetric metric = JmxMetric.create("hello", JmxType.MONOTONICALLY_INCREASING);
        Map<String, Float> values = new HashMap<>();
        values.put("hello", 5f);
        metric.recordSingleMBeanStats(stats, "Jmx/Sample/", values);
        Assert.assertEquals(1, stats.getStats("Jmx/Sample/hello").getCallCount());
        Assert.assertEquals(5f, stats.getStats("Jmx/Sample/hello").getTotal(), .001);

        values.clear();
        values.put("hello", 7f);
        metric.recordSingleMBeanStats(stats, "Jmx/Sample/", values);
        Assert.assertEquals(2, stats.getStats("Jmx/Sample/hello").getCallCount());
        Assert.assertEquals(7f, stats.getStats("Jmx/Sample/hello").getTotal(), .001);
    }

    @Test
    public void jmxMetricRecordStatsMonotonicallyForMultiBean() {
        StatsEngine stats = new StatsEngineImpl();
        JmxMetric metric = JmxMetric.create("hello", JmxType.MONOTONICALLY_INCREASING);
        Map<String, Float> values = new HashMap<>();
        values.put("hello", 5f);
        Map<String, Float> actual = new HashMap<>();
        metric.applySingleMBean("Jmx/Sample/", values, actual);
        metric.recordMultMBeanStats(stats, actual);
        Assert.assertEquals(1, stats.getStats("Jmx/Sample/hello").getCallCount());
        Assert.assertEquals(5f, stats.getStats("Jmx/Sample/hello").getTotal(), 0);

        values.clear();
        actual.clear();
        values.put("hello", 7f);
        metric.applySingleMBean("Jmx/Sample/", values, actual);
        metric.recordMultMBeanStats(stats, actual);
        Assert.assertEquals(2, stats.getStats("Jmx/Sample/hello").getCallCount());
        Assert.assertEquals(7f, stats.getStats("Jmx/Sample/hello").getTotal(), 0);
    }

    @Test
    public void jmxMetricRecordStatsMonotonicallyForMultiBeanAdd() {
        StatsEngine stats = new StatsEngineImpl();
        JmxMetric metric = JmxMetric.create("hello", JmxType.MONOTONICALLY_INCREASING);
        Map<String, Float> values = new HashMap<>();
        values.put("hello", 5f);
        Map<String, Float> actual = new HashMap<>();
        metric.applySingleMBean("Jmx/Sample/", values, actual);

        values.clear();
        values.put("hello", 7f);
        metric.applySingleMBean("Jmx/Sample/", values, actual);
        metric.recordMultMBeanStats(stats, actual);
        Assert.assertEquals(1, stats.getStats("Jmx/Sample/hello").getCallCount());
        Assert.assertEquals(12f, stats.getStats("Jmx/Sample/hello").getTotal(), 0);
    }

    @Test
    public void jmxMetricRecordStatsChangeNameSimpleMulti() {
        StatsEngine stats = new StatsEngineImpl();
        JmxMetric metric = JmxMetric.create("hello", "theHello", JmxType.SIMPLE);
        Map<String, Float> values = new HashMap<>();
        values.put("hello", 5f);
        Map<String, Float> actual = new HashMap<>();
        metric.applySingleMBean("Jmx/Sample/", values, actual);
        metric.recordMultMBeanStats(stats, actual);
        Assert.assertEquals(1, stats.getStats("Jmx/Sample/theHello").getCallCount());
        Assert.assertEquals(5f, stats.getStats("Jmx/Sample/theHello").getTotal(), 0);

        values.clear();
        values.put("hello", 7f);
        actual.clear();
        metric.applySingleMBean("Jmx/Sample/", values, actual);
        metric.recordMultMBeanStats(stats, actual);
        Assert.assertEquals(2, stats.getStats("Jmx/Sample/theHello").getCallCount());
        Assert.assertEquals(12f, stats.getStats("Jmx/Sample/theHello").getTotal(), .001);
    }

    @Test
    public void jmxMetricRecordStatsChangeNameSimpleMultiAdd() {
        StatsEngine stats = new StatsEngineImpl();
        JmxMetric metric = JmxMetric.create("hello", "theHello", JmxType.SIMPLE);
        Map<String, Float> values = new HashMap<>();
        values.put("hello", 5f);
        Map<String, Float> actual = new HashMap<>();
        metric.applySingleMBean("Jmx/Sample/", values, actual);
        Assert.assertEquals(0, stats.getStats("Jmx/Sample/theHello").getCallCount());

        values.clear();
        values.put("hello", 7f);
        metric.applySingleMBean("Jmx/Sample/", values, actual);
        metric.recordMultMBeanStats(stats, actual);
        Assert.assertEquals(1, stats.getStats("Jmx/Sample/theHello").getCallCount());
        Assert.assertEquals(12f, stats.getStats("Jmx/Sample/theHello").getTotal(), .001);
    }

    @Test
    public void jmxMetricRecordStatsChangeNameSimple() {
        StatsEngine stats = new StatsEngineImpl();
        JmxMetric metric = JmxMetric.create("hello", "theHello", JmxType.SIMPLE);
        Map<String, Float> values = new HashMap<>();
        values.put("hello", 5f);
        metric.recordSingleMBeanStats(stats, "Jmx/Sample/", values);
        Assert.assertEquals(1, stats.getStats("Jmx/Sample/theHello").getCallCount());
        Assert.assertEquals(5f, stats.getStats("Jmx/Sample/theHello").getTotal(), 0);

        values.clear();
        values.put("hello", 7f);
        metric.recordSingleMBeanStats(stats, "Jmx/Sample/", values);
        Assert.assertEquals(2, stats.getStats("Jmx/Sample/theHello").getCallCount());
        Assert.assertEquals(12f, stats.getStats("Jmx/Sample/theHello").getTotal(), .001);
    }

    @Test
    public void jmxMetricRecordStatsChangeNameMono() {
        StatsEngine stats = new StatsEngineImpl();
        JmxMetric metric = JmxMetric.create("hello", "theHello", JmxType.MONOTONICALLY_INCREASING);
        Map<String, Float> values = new HashMap<>();
        values.put("hello", 5f);
        metric.recordSingleMBeanStats(stats, "Jmx/Sample/", values);
        Assert.assertEquals(1, stats.getStats("Jmx/Sample/theHello").getCallCount());
        Assert.assertEquals(5f, stats.getStats("Jmx/Sample/theHello").getTotal(), 0);

        values.clear();
        values.put("hello", 7f);
        metric.recordSingleMBeanStats(stats, "Jmx/Sample/", values);
        Assert.assertEquals(2, stats.getStats("Jmx/Sample/theHello").getCallCount());
        Assert.assertEquals(7f, stats.getStats("Jmx/Sample/theHello").getTotal(), .001);
    }

    @Test
    public void jmxMetricRecordStatsSubTypeSimple() {
        StatsEngine stats = new StatsEngineImpl();
        JmxMetric metric = JmxMetric.create(new String[] { "first", "second" }, "theDiff",
                JmxAction.SUBTRACT_ALL_FROM_FIRST, JmxType.SIMPLE);
        Map<String, Float> values = new HashMap<>();
        values.put("first", 10f);
        values.put("second", 7f);
        metric.recordSingleMBeanStats(stats, "Jmx/Sample/", values);
        Assert.assertEquals(1, stats.getStats("Jmx/Sample/theDiff").getCallCount());
        Assert.assertEquals(3f, stats.getStats("Jmx/Sample/theDiff").getTotal(), .001);

        values.clear();
        values.put("first", 12f);
        values.put("second", 6f);
        metric.recordSingleMBeanStats(stats, "Jmx/Sample/", values);
        Assert.assertEquals(2, stats.getStats("Jmx/Sample/theDiff").getCallCount());
        Assert.assertEquals(9f, stats.getStats("Jmx/Sample/theDiff").getTotal(), .001);
    }

    @Test
    public void jmxMetricRecordStatsSubTypeSimpleMulti() {
        StatsEngine stats = new StatsEngineImpl();
        JmxMetric metric = JmxMetric.create(new String[] { "first", "second" }, "theDiff",
                JmxAction.SUBTRACT_ALL_FROM_FIRST, JmxType.SIMPLE);
        Map<String, Float> values = new HashMap<>();
        values.put("first", 10f);
        values.put("second", 7f);
        Map<String, Float> actual = new HashMap<>();
        metric.applySingleMBean("Jmx/Sample/", values, actual);
        metric.recordMultMBeanStats(stats, actual);
        Assert.assertEquals(1, stats.getStats("Jmx/Sample/theDiff").getCallCount());
        Assert.assertEquals(3f, stats.getStats("Jmx/Sample/theDiff").getTotal(), .001);

        values.clear();
        actual.clear();
        values.put("first", 12f);
        values.put("second", 6f);
        metric.applySingleMBean("Jmx/Sample/", values, actual);
        metric.recordMultMBeanStats(stats, actual);
        Assert.assertEquals(2, stats.getStats("Jmx/Sample/theDiff").getCallCount());
        Assert.assertEquals(9f, stats.getStats("Jmx/Sample/theDiff").getTotal(), .001);
    }

    @Test
    public void jmxMetricRecordStatsSubTypeSimpleMultiAdd() {
        StatsEngine stats = new StatsEngineImpl();
        JmxMetric metric = JmxMetric.create(new String[] { "first", "second" }, "theDiff",
                JmxAction.SUBTRACT_ALL_FROM_FIRST, JmxType.SIMPLE);
        Map<String, Float> values = new HashMap<>();
        values.put("first", 10f);
        values.put("second", 7f);
        Map<String, Float> actual = new HashMap<>();
        metric.applySingleMBean("Jmx/Sample/", values, actual);
        Assert.assertEquals(0, stats.getStats("Jmx/Sample/theDiff").getCallCount());

        values.clear();
        values.put("first", 12f);
        values.put("second", 6f);
        metric.applySingleMBean("Jmx/Sample/", values, actual);
        metric.recordMultMBeanStats(stats, actual);
        Assert.assertEquals(1, stats.getStats("Jmx/Sample/theDiff").getCallCount());
        Assert.assertEquals(9f, stats.getStats("Jmx/Sample/theDiff").getTotal(), .001);
    }

    @Test
    public void jmxMetricRecordStatsSubTypeMono() {
        StatsEngine stats = new StatsEngineImpl();
        JmxMetric metric = JmxMetric.create(new String[] { "first", "second" }, "theDiff",
                JmxAction.SUBTRACT_ALL_FROM_FIRST, JmxType.MONOTONICALLY_INCREASING);
        Map<String, Float> values = new HashMap<>();
        values.put("first", 10f);
        values.put("second", 7f);
        metric.recordSingleMBeanStats(stats, "Jmx/Sample/", values);
        Assert.assertEquals(1, stats.getStats("Jmx/Sample/theDiff").getCallCount());
        Assert.assertEquals(3f, stats.getStats("Jmx/Sample/theDiff").getTotal(), .001);

        values.clear();
        values.put("first", 12f);
        values.put("second", 5f);
        metric.recordSingleMBeanStats(stats, "Jmx/Sample/", values);
        Assert.assertEquals(2, stats.getStats("Jmx/Sample/theDiff").getCallCount());
        Assert.assertEquals(7f, stats.getStats("Jmx/Sample/theDiff").getTotal(), .001);
    }

    @Test
    public void jmxMetricRecordStatsSubTypeMonoMulti() {
        StatsEngine stats = new StatsEngineImpl();
        JmxMetric metric = JmxMetric.create(new String[] { "first", "second" }, "theDiff",
                JmxAction.SUBTRACT_ALL_FROM_FIRST, JmxType.MONOTONICALLY_INCREASING);
        Map<String, Float> values = new HashMap<>();
        values.put("first", 10f);
        values.put("second", 7f);
        Map<String, Float> actual = new HashMap<>();
        metric.applySingleMBean("Jmx/Sample/", values, actual);
        metric.recordMultMBeanStats(stats, actual);
        Assert.assertEquals(1, stats.getStats("Jmx/Sample/theDiff").getCallCount());
        Assert.assertEquals(3f, stats.getStats("Jmx/Sample/theDiff").getTotal(), .001);

        values.clear();
        actual.clear();
        values.put("first", 12f);
        values.put("second", 5f);
        metric.applySingleMBean("Jmx/Sample/", values, actual);
        metric.recordMultMBeanStats(stats, actual);
        Assert.assertEquals(2, stats.getStats("Jmx/Sample/theDiff").getCallCount());
        Assert.assertEquals(7f, stats.getStats("Jmx/Sample/theDiff").getTotal(), .001);
    }

    @Test
    public void jmxMetricRecordStatsSubTypeMonoMultiAdd() {
        StatsEngine stats = new StatsEngineImpl();
        JmxMetric metric = JmxMetric.create(new String[] { "first", "second" }, "theDiffe",
                JmxAction.SUBTRACT_ALL_FROM_FIRST, JmxType.MONOTONICALLY_INCREASING);
        Map<String, Float> values = new HashMap<>();
        values.put("first", 10f);
        values.put("second", 7f);
        Map<String, Float> actual = new HashMap<>();
        metric.applySingleMBean("Jmx/Sample/", values, actual);
        Assert.assertEquals(0, stats.getStats("Jmx/Sample/theDiffe").getCallCount());

        values.clear();
        values.put("first", 12f);
        values.put("second", 5f);
        metric.applySingleMBean("Jmx/Sample/", values, actual);
        metric.recordMultMBeanStats(stats, actual);
        Assert.assertEquals(1, stats.getStats("Jmx/Sample/theDiffe").getCallCount());
        Assert.assertEquals(10f, stats.getStats("Jmx/Sample/theDiffe").getTotal(), .001);
    }

}
