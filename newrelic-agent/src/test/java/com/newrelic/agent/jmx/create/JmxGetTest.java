/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.jmx.create;

import com.newrelic.agent.jmx.JmxType;
import com.newrelic.agent.jmx.metrics.JmxMetric;
import com.newrelic.agent.stats.StatsEngine;
import com.newrelic.agent.stats.StatsEngineImpl;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JmxGetTest {

    @Test
    public void testNullRootMetricName() throws MalformedObjectNameException {
        JmxGet object = new JmxSingleMBeanGet("ThreadPool:type=rara,key1=*,key2=*",
                "ThreadPool:type=rara,key1=*,key2=*", null, new ArrayList<JmxMetric>(), null, null);
        String root = object.getRootMetricName(new ObjectName("ThreadPool:type=rara,key1=value1,key2=value2"),
                getServer());
        Assert.assertEquals("JMX/ThreadPool/rara/value1/value2/", root);
    }

    @Test
    public void testNullRootMetricNameMulti() throws MalformedObjectNameException {
        JmxGet object = new JmxMultiMBeanGet("ThreadPool:type=rara,key1=*,key2=*",
                "ThreadPool:type=rara,key1=*,key2=*", null, new ArrayList<JmxMetric>(), null, null);
        String root = object.getRootMetricName(new ObjectName("ThreadPool:type=rara,key1=value1,key2=value2"),
                getServer());
        Assert.assertEquals("JMX/ThreadPool/rara/value1/value2/", root);
    }

    private MBeanServer getServer() {
        return Mockito.mock(MBeanServer.class);
    }

    @Test
    public void testGetRootMetricDefault() throws MalformedObjectNameException {
        Map<JmxType, List<String>> mapping = new HashMap<>();
        List<String> atts = new ArrayList<>();
        atts.add("Name");
        atts.add("Date");
        atts.add("Time");
        mapping.put(JmxType.SIMPLE, atts);
        JmxGet object = new JmxSingleMBeanGet("ThreadPool:type=rara,key1=*,key2=*", null,
                "ThreadPool:type=rara,key1=*,key2=*", mapping, null);
        String root = object.getRootMetricName(new ObjectName("ThreadPool:type=rara,key1=value1,key2=value2"),
                getServer());
        Assert.assertEquals("JMX/ThreadPool/rara/value1/value2/", root);
    }

    @Test
    public void testGetRootMetricNoType() throws MalformedObjectNameException {
        Map<JmxType, List<String>> mapping = new HashMap<>();
        List<String> atts = new ArrayList<>();
        atts.add("Name");
        atts.add("Date");
        atts.add("Time");
        mapping.put(JmxType.SIMPLE, atts);
        JmxGet object = new JmxSingleMBeanGet("ThreadPool:key1=*,key2=*", null, "ThreadPool:type=rara,key1=*,key2=*",
                mapping, null);
        String root = object.getRootMetricName(new ObjectName("ThreadPool:key1=value1,key2=value2"), getServer());
        Assert.assertEquals("JMX/ThreadPool/null/value2/value1/", root);
    }

    @Test
    public void testGetRootMetricNoTypeMulti() throws MalformedObjectNameException {
        Map<JmxType, List<String>> mapping = new HashMap<>();
        List<String> atts = new ArrayList<>();
        atts.add("Name");
        atts.add("Date");
        atts.add("Time");
        mapping.put(JmxType.SIMPLE, atts);
        JmxGet object = new JmxMultiMBeanGet("ThreadPool:key1=*,key2=*", null, "ThreadPool:type=rara,key1=*,key2=*",
                mapping, null);
        String root = object.getRootMetricName(new ObjectName("ThreadPool:key1=value1,key2=value2"), getServer());
        Assert.assertEquals("JMX/ThreadPool/null/value2/value1/", root);
    }

    @Test
    public void testGetRootMetricNoPattern() throws MalformedObjectNameException {
        List<JmxMetric> metrics = new ArrayList<>();
        metrics.add(JmxMetric.create("hello", JmxType.SIMPLE));
        metrics.add(JmxMetric.create("goodbye", JmxType.MONOTONICALLY_INCREASING));
        JmxGet object = new JmxSingleMBeanGet("ThreadPool:type=rara,key1=*,key2=*",
                "ThreadPool:type=rara,key1=*,key2=*", "TheRoot/Wahoo/", metrics, null, null);
        String root = object.getRootMetricName(new ObjectName("ThreadPool:type=rara,key1=value1,key2=value2"),
                getServer());
        Assert.assertEquals("JMX/TheRoot/Wahoo/", root);
    }

    @Test
    public void testGetRootMetricNoPatternMissingEndSlash() throws MalformedObjectNameException {
        List<JmxMetric> metrics = new ArrayList<>();
        metrics.add(JmxMetric.create("hello", JmxType.SIMPLE));
        metrics.add(JmxMetric.create("goodbye", JmxType.MONOTONICALLY_INCREASING));
        JmxGet object = new JmxSingleMBeanGet("ThreadPool:type=rara,key1=*,key2=*",
                "ThreadPool:type=rara,key1=*,key2=*", "TheRoot/Wahoo", metrics, null, null);
        String root = object.getRootMetricName(new ObjectName("ThreadPool:type=rara,key1=value1,key2=value2"),
                getServer());
        Assert.assertEquals("JMX/TheRoot/Wahoo/", root);
    }

    @Test
    public void testGetRootMetricWithPattern() throws MalformedObjectNameException {
        List<JmxMetric> metrics = new ArrayList<>();
        metrics.add(JmxMetric.create("hello", JmxType.SIMPLE));
        metrics.add(JmxMetric.create("goodbye", JmxType.MONOTONICALLY_INCREASING));
        JmxGet object = new JmxSingleMBeanGet("ThreadPool:type=rara,key1=*,key2=*",
                "ThreadPool:type=rara,key1=*,key2=*", "TheRoot/Wahoo/{type}/{key1}", metrics, null, null);
        String root = object.getRootMetricName(new ObjectName("ThreadPool:type=rara,key1=value1,key2=value2"),
                getServer());
        Assert.assertEquals("JMX/TheRoot/Wahoo/rara/value1/", root);
    }

    @Test
    public void testGetRootMetricWithNoDelimiterAndBoundedRangePattern() throws MalformedObjectNameException {
        List<JmxMetric> metrics = new ArrayList<>();
        metrics.add(JmxMetric.create("hello", JmxType.SIMPLE));
        metrics.add(JmxMetric.create("goodbye", JmxType.MONOTONICALLY_INCREASING));
        JmxGet object = new JmxSingleMBeanGet("ThreadPool:type=rara,key1=*,key2=*,key3=*,otherKey=*",
                "ThreadPool:type=rara,key1=*,key2=*,key3=*,otherKey=*", "TheRoot/Wahoo/{type}/{for:key[1:3:]}/{otherKey}", metrics, null, null);
        String root = object.getRootMetricName(new ObjectName("ThreadPool:type=rara,key1=value1,key2=value2,key3=value3,otherKey=otherValue"),
                getServer());
        Assert.assertEquals("JMX/TheRoot/Wahoo/rara/value1/value2/otherValue/", root);
    }

    @Test
    public void testGetRootMetricWithDelimiterAndUnBoundedRangePattern() throws MalformedObjectNameException {
        List<JmxMetric> metrics = new ArrayList<>();
        metrics.add(JmxMetric.create("hello", JmxType.SIMPLE));
        metrics.add(JmxMetric.create("goodbye", JmxType.MONOTONICALLY_INCREASING));
        JmxGet object = new JmxSingleMBeanGet("ThreadPool:type=rara,key1=*,key2=*,key3=*,otherKey=*",
                "ThreadPool:type=rara,key1=*,key2=*,key3=*,otherKey=*", "TheRoot/Wahoo/{type}/{for:key[1::.]}/{otherKey}", metrics, null, null);
        String root = object.getRootMetricName(new ObjectName("ThreadPool:type=rara,key1=value1,key2=value2,key3=value3,otherKey=otherValue"),
                getServer());
        Assert.assertEquals("JMX/TheRoot/Wahoo/rara/value1.value2.value3/otherValue/", root);
    }

    @Test
    public void testGetRootMetricWithAttributePattern() throws MalformedObjectNameException,
            AttributeNotFoundException, InstanceNotFoundException, MBeanException, ReflectionException {
        List<JmxMetric> metrics = new ArrayList<>();
        metrics.add(JmxMetric.create("hello", JmxType.SIMPLE));
        metrics.add(JmxMetric.create("goodbye", JmxType.MONOTONICALLY_INCREASING));
        JmxGet object = new JmxSingleMBeanGet("ThreadPool:type=rara,key1=*,key2=*",
                "ThreadPool:type=rara,key1=*,key2=*", "TheRoot/Wahoo/{:attr1:}/", metrics, null, null);

        ObjectName objectName = new ObjectName("ThreadPool:type=rara,key1=value1,key2=value2");
        MBeanServer server = getServer();
        Mockito.when(server.getAttribute(objectName, "attr1")).thenReturn("dude");

        String root = object.getRootMetricName(objectName, server);
        Assert.assertEquals("JMX/TheRoot/Wahoo/dude/", root);
    }

    @Test
    public void testGetRootMetricWithPatternMissingEndSlash() throws MalformedObjectNameException {
        List<JmxMetric> metrics = new ArrayList<>();
        metrics.add(JmxMetric.create("hello", JmxType.SIMPLE));
        metrics.add(JmxMetric.create("goodbye", JmxType.MONOTONICALLY_INCREASING));
        JmxGet object = new JmxSingleMBeanGet("ThreadPool:type=rara,key1=*,key2=*",
                "ThreadPool:type=rara,key1=*,key2=*", "TheRoot/Wahoo/{type}/{key1}", metrics, null, null);
        String root = object.getRootMetricName(new ObjectName("ThreadPool:type=rara,key1=value1,key2=value2"),
                getServer());
        Assert.assertEquals("JMX/TheRoot/Wahoo/rara/value1/", root);
    }

    @Test
    public void testGetRootMetricWithNonExistKey() throws MalformedObjectNameException {
        List<JmxMetric> metrics = new ArrayList<>();
        metrics.add(JmxMetric.create("hello", JmxType.SIMPLE));
        metrics.add(JmxMetric.create("goodbye", JmxType.MONOTONICALLY_INCREASING));
        JmxGet object = new JmxSingleMBeanGet("ThreadPool:type=rara,key1=*,key2=*",
                "ThreadPool:type=rara,key1=*,key2=*", "TheRoot/Wahoo/{type}/{keydoesnotexist}/", metrics, null, null);
        String root = object.getRootMetricName(new ObjectName("ThreadPool:type=rara,key1=value1,key2=value2"),
                getServer());
        Assert.assertEquals("JMX/TheRoot/Wahoo/rara//", root);
    }

    @Test
    public void testGetRootMetricWithNonExistAllKeys() throws MalformedObjectNameException {
        List<JmxMetric> metrics = new ArrayList<>();
        metrics.add(JmxMetric.create("hello", JmxType.SIMPLE));
        metrics.add(JmxMetric.create("goodbye", JmxType.MONOTONICALLY_INCREASING));
        JmxGet object = new JmxSingleMBeanGet("ThreadPool:type=rara,key1=*,key2=*",
                "ThreadPool:type=rara,key1=*,key2=*", "TheRoot/Wahoo/{blabla}/{keydoesnotexist}/", metrics, null, null);
        String root = object.getRootMetricName(new ObjectName("ThreadPool:type=rara,key1=value1,key2=value2"),
                getServer());
        Assert.assertEquals("JMX/TheRoot/Wahoo///", root);
    }

    @Test
    public void testJmxGetToString() throws MalformedObjectNameException {
        List<JmxMetric> metrics = new ArrayList<>();
        metrics.add(JmxMetric.create("hello", JmxType.SIMPLE));
        metrics.add(JmxMetric.create("goodbye", JmxType.MONOTONICALLY_INCREASING));
        JmxGet object = new JmxSingleMBeanGet("ThreadPool:type=rara,key1=*,key2=*",
                "ThreadPool:type=rara,key1=*,key2=*", "TheRoot/Wahoo/{blabla}/{keydoesnotexist}/", metrics, null, null);
        String root = object.toString();
        Assert.assertEquals(
                "object_name: ThreadPool:type=rara,key1=*,key2=* attributes: [hello type: simple, goodbye type: monotonically_increasing]",
                root);
    }

    @Test
    public void testJmxTestToStringNoAttributes() throws MalformedObjectNameException {
        JmxGet object = new JmxSingleMBeanGet("ThreadPool:type=rara,key1=*,key2=*",
                "ThreadPool:type=rara,key1=*,key2=*", "TheRoot/Wahoo/{blabla}/{keydoesnotexist}/",
                (List<JmxMetric>) null, null, null);
        String root = object.toString();
        Assert.assertEquals("object_name: ThreadPool:type=rara,key1=*,key2=* attributes: []", root);
    }

    @Test
    public void testJmxGetAtts() throws MalformedObjectNameException {
        List<JmxMetric> metrics = new ArrayList<>();
        metrics.add(JmxMetric.create("hello", JmxType.SIMPLE));
        metrics.add(JmxMetric.create("goodbye", JmxType.MONOTONICALLY_INCREASING));
        JmxGet object = new JmxSingleMBeanGet("ThreadPool:type=rara,key1=*,key2=*",
                "ThreadPool:type=rara,key1=*,key2=*", "TheRoot/Wahoo/{blabla}/{keydoesnotexist}/", metrics, null, null);
        Collection<String> actual = object.getAttributes();
        Assert.assertEquals(2, actual.size());
        Assert.assertTrue(actual.contains("hello"));
        Assert.assertTrue(actual.contains("goodbye"));
    }

    @Test
    public void testJmxGetStats() throws MalformedObjectNameException {
        StatsEngine stats = new StatsEngineImpl();
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();

        List<JmxMetric> metrics = new ArrayList<>();
        metrics.add(JmxMetric.create("hello", JmxType.SIMPLE));
        metrics.add(JmxMetric.create("goodbye", JmxType.MONOTONICALLY_INCREASING));
        JmxGet object = new JmxSingleMBeanGet("ThreadPool:type=rara,key1=*", "ThreadPool:type=rara,key1=*", null,
                metrics, null, null);

        Map<String, Float> values = new HashMap<>();
        values.put("hello", 5f);
        values.put("goodbye", 4f);

        object.recordStats(stats, createMap("ThreadPool:type=rara,key1=a", values), server);
        Assert.assertEquals(5f, stats.getStats("JMX/ThreadPool/rara/a/hello").getTotal(), .001);
        Assert.assertEquals(4f, stats.getStats("JMX/ThreadPool/rara/a/goodbye").getTotal(), .001);

        values.clear();
        values.put("hello", 6f);
        values.put("goodbye", 7f);

        object.recordStats(stats, createMap("ThreadPool:type=rara,key1=a", values), server);
        Assert.assertEquals(11f, stats.getStats("JMX/ThreadPool/rara/a/hello").getTotal(), .001);
        Assert.assertEquals(7f, stats.getStats("JMX/ThreadPool/rara/a/goodbye").getTotal(), .001);
    }

    @Test
    public void testJmxGetStatsMulti() throws MalformedObjectNameException {
        StatsEngine stats = new StatsEngineImpl();
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();

        List<JmxMetric> metrics = new ArrayList<>();
        metrics.add(JmxMetric.create("hello1", JmxType.SIMPLE));
        metrics.add(JmxMetric.create("goodbye1", JmxType.MONOTONICALLY_INCREASING));
        JmxGet object = new JmxMultiMBeanGet("ThreadPool:type=rara,key1=*", "ThreadPool:type=rara,key1=*", null,
                metrics, null, null);

        Map<String, Float> values = new HashMap<>();
        values.put("hello1", 5f);
        values.put("goodbye1", 4f);

        object.recordStats(stats, createMap("ThreadPool:type=rara,key1=a", values), server);
        Assert.assertEquals(5f, stats.getStats("JMX/ThreadPool/rara/a/hello1").getTotal(), .001);
        Assert.assertEquals(4f, stats.getStats("JMX/ThreadPool/rara/a/goodbye1").getTotal(), .001);

        values.clear();
        values.put("hello1", 6f);
        values.put("goodbye1", 7f);

        object.recordStats(stats, createMap("ThreadPool:type=rara,key1=a", values), server);
        Assert.assertEquals(11f, stats.getStats("JMX/ThreadPool/rara/a/hello1").getTotal(), .001);
        Assert.assertEquals(7f, stats.getStats("JMX/ThreadPool/rara/a/goodbye1").getTotal(), .001);
        Assert.assertEquals(2f, stats.getStats("JMX/ThreadPool/rara/a/hello1").getCallCount(), .001);
        Assert.assertEquals(2f, stats.getStats("JMX/ThreadPool/rara/a/goodbye1").getCallCount(), .001);
    }

    @Test
    public void testJmxGetStatsMultiSimilar() throws MalformedObjectNameException {
        StatsEngine stats = new StatsEngineImpl();
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();

        List<JmxMetric> metrics = new ArrayList<>();
        metrics.add(JmxMetric.create("hello2", JmxType.SIMPLE));
        metrics.add(JmxMetric.create("goodbye2", JmxType.MONOTONICALLY_INCREASING));
        JmxGet object = new JmxMultiMBeanGet("ThreadPool:type=rara,key1=*,key2=*", "ThreadPool:type=rara,key1=*",
                "JmxBuiltIn/ThreadPool/{key1}", metrics, null, null);

        Map<ObjectName, Map<String, Float>> data = new HashMap<>();

        Map<String, Float> values1 = new HashMap<>();
        values1.put("hello2", 2f);
        values1.put("goodbye2", 4f);
        data.put(new ObjectName("ThreadPool:type=rara,key1=a,key2=b"), values1);

        Map<String, Float> values2 = new HashMap<>();
        values2.put("hello2", 4f);
        values2.put("goodbye2", 5f);
        data.put(new ObjectName("ThreadPool:type=rara,key1=a,key2=c"), values2);

        Map<String, Float> values3 = new HashMap<>();
        values3.put("hello2", 5f);
        values3.put("goodbye2", 7f);
        data.put(new ObjectName("ThreadPool:type=rara,key1=a,key2=d"), values3);

        object.recordStats(stats, data, server);

        Assert.assertEquals(11f, stats.getStats("JmxBuiltIn/ThreadPool/a/hello2").getTotal(), .001);
        Assert.assertEquals(16f, stats.getStats("JmxBuiltIn/ThreadPool/a/goodbye2").getTotal(), .001);
        Assert.assertEquals(1, stats.getStats("JmxBuiltIn/ThreadPool/a/hello2").getCallCount());
        Assert.assertEquals(1, stats.getStats("JmxBuiltIn/ThreadPool/a/goodbye2").getCallCount());
    }

    @Test
    public void testJmxGetStatsSingleSimilar() throws MalformedObjectNameException {
        StatsEngine stats = new StatsEngineImpl();
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();

        List<JmxMetric> metrics = new ArrayList<>();
        metrics.add(JmxMetric.create("hello3", JmxType.SIMPLE));
        metrics.add(JmxMetric.create("goodbye3", JmxType.SIMPLE));
        JmxGet object = new JmxSingleMBeanGet("ThreadPool:type=rara,key1=*,key2=*", "ThreadPool:type=rara,key1=*",
                "JmxBuiltIn/ThreadPool/{key1}", metrics, null, null);

        Map<ObjectName, Map<String, Float>> data = new HashMap<>();

        Map<String, Float> values1 = new HashMap<>();
        values1.put("hello3", 2f);
        values1.put("goodbye3", 3f);
        data.put(new ObjectName("ThreadPool:type=rara,key1=a,key2=b"), values1);

        Map<String, Float> values2 = new HashMap<>();
        values2.put("hello3", 4f);
        values2.put("goodbye3", 5f);
        data.put(new ObjectName("ThreadPool:type=rara,key1=a,key2=c"), values2);

        Map<String, Float> values3 = new HashMap<>();
        values3.put("hello3", 5f);
        values3.put("goodbye3", 7f);
        data.put(new ObjectName("ThreadPool:type=rara,key1=a,key2=d"), values3);
        object.recordStats(stats, data, server);

        Assert.assertEquals(3, stats.getStats("JmxBuiltIn/ThreadPool/a/hello3").getCallCount());
        Assert.assertEquals(3, stats.getStats("JmxBuiltIn/ThreadPool/a/goodbye3").getCallCount());
        Assert.assertEquals(11f, stats.getStats("JmxBuiltIn/ThreadPool/a/hello3").getTotal(), .001);
        Assert.assertEquals(15f, stats.getStats("JmxBuiltIn/ThreadPool/a/goodbye3").getTotal(), .001);
    }

    private Map<ObjectName, Map<String, Float>> createMap(String name, Map<String, Float> values)
            throws MalformedObjectNameException {
        Map<ObjectName, Map<String, Float>> output = new HashMap<>();
        output.put(new ObjectName(name), values);
        return output;
    }

    @Test
    public void cleanValue() {
        String value = "/java_test_webapp";
        Assert.assertEquals("java_test_webapp", JmxGet.cleanValue(value));
    }
}
