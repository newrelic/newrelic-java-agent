/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation;

import com.newrelic.agent.AgentHelper;
import com.newrelic.agent.MetricData;
import com.newrelic.agent.MockNormalizer;
import com.newrelic.agent.instrumentation.classmatchers.ChildClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.InterfaceMatcher;
import com.newrelic.agent.instrumentation.custom.ExtensionClassAndMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.ExactMethodMatcher;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.CountStats;
import com.newrelic.agent.stats.StatsBase;
import com.newrelic.agent.stats.StatsEngine;
import org.junit.Assert;

import java.lang.instrument.UnmodifiableClassException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class InstrumentTestUtils {

    public static final String TRANS_PREFIX = "OtherTransaction/Custom/";
    public static final String METHOD_PREFIX = "Java/";

    public static void verifyInterfaceAdded(Object obj, String interfaceName) {
        Class[] ifaces = obj.getClass().getInterfaces();
        Assert.assertNotNull(ifaces);
        boolean foundIt = false;
        for (Class clazz : ifaces) {
            if (clazz.getName().equals(interfaceName)) {
                foundIt = true;
                break;
            }
        }
        Assert.assertTrue("Was unable to find the interface " + interfaceName, foundIt);
    }

    public static void verifySingleMetrics(String... names) {
        Map<String, Integer> expected = new HashMap<>();
        for (String current : names) {
            expected.put(current, 1);
        }
        InstrumentTestUtils.verifyCountMetric(expected);
    }

    public static void veryPresentNotPresent(List<String> present, List<String> notPresent) {
        List<MetricData> data = AgentHelper.getDefaultStatsEngine().getMetricData(new MockNormalizer());
        for (String current : present) {
            verifyMetricCount(current, 1, data);
        }
        for (String current : notPresent) {
            verifyNoMetric(current, data);
        }
    }

    public static void createTransformerAndRetransformClass(String className, String methodName, String methodDesc)
            throws UnmodifiableClassException {
        ExtensionClassAndMethodMatcher pc = InstrumentTestUtils.createPointCut(className, methodName, methodDesc);
        List<ExtensionClassAndMethodMatcher> list = new LinkedList<>();
        list.add(pc);
        ServiceFactory.getClassTransformerService().getLocalRetransformer().appendClassMethodMatchers(list);
        InstrumentTestUtils.retransformClass(className);
    }

    public static void createTransformerAndRetransformInterface(String interfaceName, String methodName, String methodDesc)
            throws UnmodifiableClassException {
        ExtensionClassAndMethodMatcher pc = InstrumentTestUtils.createInterfacePointCut(interfaceName, methodName, methodDesc);
        List<ExtensionClassAndMethodMatcher> list = new LinkedList<>();
        list.add(pc);
        ServiceFactory.getClassTransformerService().getLocalRetransformer().appendClassMethodMatchers(list);
        InstrumentTestUtils.retransformAllMatching(pc);
    }

    public static void createTransformerAndRetransformSuperclass(String superclassName, String methodName, String methodDesc)
            throws UnmodifiableClassException {
        ExtensionClassAndMethodMatcher pc = InstrumentTestUtils.createSuperclassPointCut(superclassName, methodName, methodDesc);
        List<ExtensionClassAndMethodMatcher> list = new LinkedList<>();
        list.add(pc);
        ServiceFactory.getClassTransformerService().getLocalRetransformer().appendClassMethodMatchers(list);
        InstrumentTestUtils.retransformAllMatching(pc);
    }

    public static ExtensionClassAndMethodMatcher createSuperclassPointCut(String superclassName, String methodName, String methodDesc) {
        return new ExtensionClassAndMethodMatcher(null, null, null, new ChildClassMatcher(superclassName, false),
                new ExactMethodMatcher(methodName, methodDesc), true, false, false, false, null);
    }

    public static ExtensionClassAndMethodMatcher createInterfacePointCut(String interfaceName, String methodName, String methodDesc) {
        return new ExtensionClassAndMethodMatcher(null, null, null, new InterfaceMatcher(interfaceName),
                new ExactMethodMatcher(methodName, methodDesc), true, false, false, false, null);
    }

    public static ExtensionClassAndMethodMatcher createPointCut(String className, String methodName, String methodDesc) {
        return new ExtensionClassAndMethodMatcher("mytest", null, null, new ExactClassMatcher(className),
                new ExactMethodMatcher(methodName, methodDesc), true, false, false, false, null);
    }

    public static void retransformAllMatching(ExtensionClassAndMethodMatcher pc) throws UnmodifiableClassException {
        List<Class> all = new ArrayList<>();
        Map<String, Class> tada = new HashMap<>();
        for (Class clazz : ServiceFactory.getCoreService().getInstrumentation().getAllLoadedClasses()) {
            if (pc.getClassMatcher().isMatch(clazz)) {
                all.add(clazz);
            }
        }

        printClassNames(all);
        ServiceFactory.getCoreService().getInstrumentation().retransformClasses(all.toArray(new Class[all.size()]));
        Assert.assertTrue("Zero classes were retransformed.", all.size() > 0);
    }

    private static void printClassNames(List<Class> clazzes) {
        for (Class c : clazzes) {
            System.out.println("NAME: " + c.getName());
        }
    }

    public static void retransformClass(String className) throws UnmodifiableClassException {
        boolean wasRetrans = false;
        for (Class clazz : ServiceFactory.getCoreService().getInstrumentation().getAllLoadedClasses()) {
            if (clazz.getName().endsWith(className)) {
                ServiceFactory.getCoreService().getInstrumentation().retransformClasses(clazz);
                wasRetrans = true;
            }
        }
        Assert.assertTrue(MessageFormat.format("The class {0} was never retransformed.", className), wasRetrans);
    }

    public static void verifyScopedMetrics(String metric) {
        Set<String> metrics = AgentHelper.getMetrics(true);
        AgentHelper.verifyMetrics(metrics, metric);
    }

    public static Map<String, Integer> getAndClearMetricData() {
        StatsEngine statsEngine = AgentHelper.getDefaultStatsEngine();
        List<MetricData> data = statsEngine.getMetricData(new MockNormalizer());
        Map<String, Integer> dataMap = new HashMap<>();
        for (MetricData metricData : data) {
            StatsBase stats = metricData.getStats();
            if (stats instanceof CountStats) {
                dataMap.put(metricData.getMetricName().getName(), ((CountStats) stats).getCallCount());
            } else {
                dataMap.put(metricData.getMetricName().getName(), 0);
            }
        }
        return dataMap;
    }

    public static void verifyCountMetric(Map<String, Integer> expected, StatsEngine engine) {
        List<MetricData> data = engine.getMetricData(new MockNormalizer());
        for (Entry<String, Integer> current : expected.entrySet()) {
            if (current.getValue() != null) {
                verifyMetricCount(current.getKey(), current.getValue(), data);
            } else {
                printData(data);
                Assert.fail(MessageFormat.format("The expected count for {0} should not be null", current.getKey()));
            }
        }
    }

    public static void verifyCountMetric(Map<String, Integer> expected) {
        StatsEngine statsEngine = AgentHelper.getDefaultStatsEngine();
        verifyCountMetric(expected, statsEngine);
    }

    public static void verifyNoMetric(String name, List<MetricData> data) {
        for (MetricData current : data) {
            if (current.getMetricName().getName().equals(name)) {
                Assert.fail("Was able to find metric name " + name);
            }
        }
    }

    public static void verifyMetricCount(String name, int count, List<MetricData> data) {
        boolean foundName = false;
        for (MetricData current : data) {
            if (current.getMetricName().getName().equals(name)) {
                foundName = true;
                StatsBase stats = current.getStats();
                if (stats instanceof CountStats) {
                    Assert.assertEquals(name, count, ((CountStats) stats).getCallCount());
                }
            }
        }
        printData(data);
        Assert.assertTrue("Was not able to find metric name " + name, foundName);
    }

    private static void printData(List<MetricData> data) {
        StringBuilder sb = new StringBuilder("Current Metrics: ");
        for (MetricData each : data) {
            sb.append(" ");
            sb.append(each.getMetricName());
        }
        System.out.println(sb.toString());
    }

    public static void verifyMetricNotPresent(String inputMetric) {
        Set<String> metrics = AgentHelper.getMetrics(true);
        Assert.assertFalse("Found metric " + inputMetric, metrics.contains(inputMetric));
    }

    public static void verifyMetricNotPresent(List<String> inputMetric) {
        Set<String> metrics = AgentHelper.getMetrics(true);
        for (String current : inputMetric) {
            Assert.assertFalse("Was able to find metric name " + current, metrics.contains(current));
        }
    }

    public static void verifyMetricPresent(String inputMetric) {
        Set<String> metrics = AgentHelper.getMetrics(true);
        Assert.assertTrue("Metric not found:" + inputMetric, metrics.contains(inputMetric));
    }

    public static void verifyMetricPresent(String... inputMetric) {
        Set<String> metrics = AgentHelper.getMetrics(true);
        for (String current : inputMetric) {
            Assert.assertTrue("Was not able to find metric name " + current, metrics.contains(current));
        }
    }

    public static void verifyMetrics(String[] present, String[] notPresent) {
        Set<String> metrics = AgentHelper.getMetrics(true);
        for (String value : present) {
            Assert.assertTrue(value + " not present when expected.", metrics.contains(value));
        }
        for (String value : notPresent) {
            Assert.assertFalse(value + " present when not expected.", metrics.contains(value));
        }
    }
}
