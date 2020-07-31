/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracers.nameformatters;

import org.junit.Assert;

import org.junit.Test;

import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.metricname.ClassMethodMetricNameFormat;
import com.newrelic.agent.tracers.metricname.DefaultMetricNameFormat;
import com.newrelic.agent.tracers.metricname.MetricNameFormat;
import com.newrelic.agent.tracers.metricname.SimpleMetricNameFormat;

public class MetricNameFormatTest {
    @Test
    public void classMethodMetricNameFormat() {
        ClassMethodSignature sig = new ClassMethodSignature("com.foo.Bar", "getName()", "()V");

        Assert.assertEquals("Java/com.newrelic.agent.tracers.nameformatters.MetricNameFormatTest/getName()",
                ClassMethodMetricNameFormat.getMetricName(sig, this));
        Assert.assertEquals("Test/com.newrelic.agent.tracers.nameformatters.MetricNameFormatTest/getName()",
                ClassMethodMetricNameFormat.getMetricName(sig, this, "Test"));

        ClassMethodMetricNameFormat formatter = new ClassMethodMetricNameFormat(sig, this.getClass().getName());
        Assert.assertEquals("Java/com.newrelic.agent.tracers.nameformatters.MetricNameFormatTest/getName()",
                formatter.getMetricName());
        Assert.assertEquals("Java/com.newrelic.agent.tracers.nameformatters.MetricNameFormatTest/getName()",
                formatter.getTransactionSegmentName());
        Assert.assertEquals("Spring/com.newrelic.agent.tracers.nameformatters.MetricNameFormatTest/getName()",
                new ClassMethodMetricNameFormat(sig, this.getClass().getName(), "Spring").getMetricName());
    }

    @Test
    public void defaultFormat() {
        ClassMethodSignature sig = new ClassMethodSignature("com.foo.Bar", "getName()", "()V");
        DefaultMetricNameFormat formatter = new DefaultMetricNameFormat(sig, this, "Java/{0}/{1}");
        Assert.assertEquals("Java/com.newrelic.agent.tracers.nameformatters.MetricNameFormatTest/getName()",
                formatter.getMetricName());
        Assert.assertEquals("Java/com.newrelic.agent.tracers.nameformatters.MetricNameFormatTest/getName()",
                formatter.getTransactionSegmentName());

        formatter = new DefaultMetricNameFormat(sig, this, "Class/{0}");
        Assert.assertEquals("Class/com.newrelic.agent.tracers.nameformatters.MetricNameFormatTest",
                formatter.getMetricName());

        formatter = new DefaultMetricNameFormat(sig, this, "Method/{1}");
        Assert.assertEquals("Method/getName()", formatter.getMetricName());
    }

    @Test
    public void simpleFormat() {
        MetricNameFormat formatter = new SimpleMetricNameFormat("Foo/Bar");
        Assert.assertEquals("Foo/Bar", formatter.getMetricName());
        Assert.assertEquals("Foo/Bar", formatter.getTransactionSegmentName());

        formatter = new SimpleMetricNameFormat("Foo/Bean", "FooBar/Dude/Man");
        Assert.assertEquals("Foo/Bean", formatter.getMetricName());
        Assert.assertEquals("FooBar/Dude/Man", formatter.getTransactionSegmentName());
    }
}
