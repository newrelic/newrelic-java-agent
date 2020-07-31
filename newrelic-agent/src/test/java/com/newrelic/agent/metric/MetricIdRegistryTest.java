/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.metric;

import org.junit.Assert;
import org.junit.Test;

public class MetricIdRegistryTest {

    @Test
    public void getMetricId() {
        MetricIdRegistry registry = new MetricIdRegistry();
        String name = "Test";
        MetricName metricName = MetricName.create(name);
        Assert.assertNull(registry.getMetricId(metricName));
        Assert.assertEquals(0, registry.getSize());
    }

    @Test
    public void setMetricId() {
        MetricIdRegistry registry = new MetricIdRegistry();
        String name = "Test";
        MetricName metricName = MetricName.create(name);
        int metricId = 1;
        registry.setMetricId(metricName, metricId);
        Assert.assertEquals(metricId, registry.getMetricId(metricName).intValue());
        Assert.assertEquals(1, registry.getSize());
    }

    @Test
    public void setMetricIdMultiple() {
        MetricIdRegistry registry = new MetricIdRegistry();
        String name = "Test";
        MetricName metricName = MetricName.create(name);
        String name2 = "Test2";
        MetricName metricName2 = MetricName.create(name2);
        int metricId = 1;
        registry.setMetricId(metricName, metricId);
        int metricId2 = 2;
        registry.setMetricId(metricName2, metricId2);
        Assert.assertEquals(metricId, registry.getMetricId(metricName).intValue());
        Assert.assertEquals(metricId2, registry.getMetricId(metricName2).intValue());
        Assert.assertEquals(2, registry.getSize());
    }

    @Test
    public void clear() {
        MetricIdRegistry registry = new MetricIdRegistry();
        String name = "Test";
        MetricName metricName = MetricName.create(name);
        String name2 = "Test2";
        MetricName metricName2 = MetricName.create(name2);
        int metricId = 1;
        registry.setMetricId(metricName, metricId);
        int metricId2 = 2;
        registry.setMetricId(metricName2, metricId2);
        registry.clear();
        Assert.assertEquals(0, registry.getSize());
    }

    @Test
    public void metricLimit() {
        MetricIdRegistry registry = new MetricIdRegistry();
        for (int i = 0; i < MetricIdRegistry.METRIC_LIMIT; i++) {
            MetricName metricName = MetricName.create(String.valueOf(i));
            registry.setMetricId(metricName, i);
        }
        Assert.assertEquals(MetricIdRegistry.METRIC_LIMIT, registry.getSize());
        MetricName metricName = MetricName.create(String.valueOf(MetricIdRegistry.METRIC_LIMIT));
        int metricId = MetricIdRegistry.METRIC_LIMIT;
        registry.setMetricId(metricName, metricId);
        Assert.assertEquals(1, registry.getSize());
    }

}
