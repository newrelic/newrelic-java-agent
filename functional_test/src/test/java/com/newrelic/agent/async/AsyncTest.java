/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.async;

import com.newrelic.agent.TransactionActivity;
import com.newrelic.agent.TransactionData;
import com.newrelic.agent.TransactionStatsListener;
import com.newrelic.agent.attributes.AttributeNames;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.SimpleStatsEngine;
import com.newrelic.agent.stats.StatsBase;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.agent.trace.TransactionSegment;
import com.newrelic.agent.trace.TransactionTrace;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.TransactionActivityInitiator;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public abstract class AsyncTest implements TransactionStatsListener {

    public static String NO_ASYNC_CONTEXT = "NO_ASYNC_CONTEXT";

    protected TransactionData data;
    protected List<TransactionData> dataList;
    protected TransactionStats stats;
    protected List<TransactionStats> statsList;
    private int timesSet;

    @Before
    public void setup() {
        ServiceFactory.getTransactionService().addTransactionStatsListener(this);

        data = null;
        dataList = new ArrayList<>();
        stats = null;
        statsList = new ArrayList<>();
        timesSet = 0;
    }

    @After
    public void unregister() {
        ServiceFactory.getTransactionService().removeTransactionStatsListener(this);
    }

    @Override
    public void dispatcherTransactionStatsFinished(TransactionData transactionData, TransactionStats transactionStats) {
        TransactionStats statsCopy = new TransactionStats();
        try {
            // Create deep copy of transactionStats object
            for (Map.Entry<String, StatsBase> entry : transactionStats.getUnscopedStats().getStatsMap().entrySet()) {
                statsCopy.getUnscopedStats().getStatsMap().put(entry.getKey(), (StatsBase) entry.getValue().clone());
            }
            for (Map.Entry<String, StatsBase> entry : transactionStats.getScopedStats().getStatsMap().entrySet()) {
                statsCopy.getScopedStats().getStatsMap().put(entry.getKey(), (StatsBase) entry.getValue().clone());
            }
        } catch (Exception e) {
            statsCopy = transactionStats;
        }

        timesSet++;
        data = transactionData;
        stats = statsCopy;
        dataList.add(transactionData);
        statsList.add(statsCopy);
    }

    public String fmtMetric(Object... parts) {
        StringBuilder metric = new StringBuilder();
        for (Object part : parts) {
            if (part instanceof String) {
                metric.append(part);
            } else if (part instanceof Class) {
                metric.append(((Class<?>) part).getName());
            } else {
                metric.append(part.getClass().getName());
            }
        }
        return metric.toString();
    }

    public void verifyCpu(long minCpu) {
        Assert.assertNotNull(data);
        Assert.assertNotNull(data.getIntrinsicAttributes().get(AttributeNames.CPU_TIME_PARAMETER_NAME));
        Long val = (Long) data.getIntrinsicAttributes().get(AttributeNames.CPU_TIME_PARAMETER_NAME);
        Assert.assertTrue("The cpu should be greater than 0", val > 0);
        Assert.assertTrue("The cpu should be greater than the expeted min value " + minCpu, val > minCpu);

        long cpuTime = 0L;
        Collection<Tracer> tracers = new HashSet<>();
        tracers.add(data.getRootTracer());
        tracers.addAll(data.getTracers());
        Collection<TransactionActivity> txas = new HashSet<>();
        // collect all txas for the transaction
        for (Tracer current : tracers) {
            if (current instanceof TransactionActivityInitiator) {
                txas.add(((DefaultTracer) current).getTransactionActivity());
            }
        }
        for (TransactionActivity txa : txas) {
            cpuTime += txa.getTotalCpuTime();
        }
        Assert.assertEquals("The cpu should be sum of txa cpus ", cpuTime, val.longValue());
    }

    public void verifyTimesSet(int expected) {
        waitForTransactionTimesSet(expected);
        Assert.assertEquals(expected, timesSet);
    }

    public void verifyScopedMetricsPresent(String scope, String... metricsPresent) {
        waitForTransaction();
        verifyScopedMetricsPresent(stats, data, scope, 1, metricsPresent);
    }

    public void verifyScopedMetricsPresent(String scope, int count, String... metricsPresent) {
        waitForTransaction();
        verifyScopedMetricsPresent(stats, data, scope, count, metricsPresent);
    }

    public void verifyScopedMetricsPresent(TransactionStats stats, TransactionData data, String scope,
            String... metricsPresent) {
        verifyScopedMetricsPresent(stats, data, scope, 1, metricsPresent);
    }

    public void verifyScopedMetricsPresent(TransactionStats stats, TransactionData data, String scope, int count,
            String... metricsPresent) {
        Assert.assertNotNull(stats);
        Assert.assertNotNull(data);

        Map<String, StatsBase> scopedStats = stats.getScopedStats().getStatsMap();
        for (String stat : metricsPresent) {
            Assert.assertNotNull(stat + " is not present", scopedStats.get(stat));
        }

        Assert.assertEquals(scope, data.getPriorityTransactionName().getName());

        verifyStatsCount(stats.getScopedStats(), count, metricsPresent);
    }

    public void verifyScopedMetricsNotPresent(String scope, String... metricsPresent) {
        waitForTransaction();
        Assert.assertNotNull(stats);
        Assert.assertNotNull(data);

        Map<String, StatsBase> scopedStats = stats.getScopedStats().getStatsMap();
        for (String stat : metricsPresent) {
            Assert.assertNull(stat + " is present when it should not be", scopedStats.get(stat));
        }

        Assert.assertEquals(scope, data.getPriorityTransactionName().getName());
    }

    public void verifyCustomUnscopedMetric(String name, int count) {
        verifyUnscopedMetricsPresentIgnoringValues(stats, name);
        SimpleStatsEngine engine = stats.getUnscopedStats();
        Assert.assertNotNull(engine);
        Assert.assertEquals(count, engine.getStats(name).getCallCount());
    }

    public void verifyUnscopedMetricsPresent(String... metricsPresent) {
        waitForTransaction();
        verifyUnscopedMetricsPresent(stats, 1, metricsPresent);
    }

    public void verifyUnscopedMetricsPresent(int count, String... metricsPresent) {
        waitForTransaction();
        verifyUnscopedMetricsPresent(stats, count, metricsPresent);
    }

    public void verifyUnscopedMetricsPresent(TransactionStats stats, String... metricsPresent) {
        verifyUnscopedMetricsPresent(stats, 1, metricsPresent);
    }

    public void verifyUnscopedMetricsPresent(TransactionStats stats, int count, String... metricsPresent) {
        verifyUnscopedMetricsPresentIgnoringValues(stats, metricsPresent);
        verifyStatsCount(stats.getUnscopedStats(), count, metricsPresent);
    }

    protected void verifyUnscopedMetricsPresentIgnoringValues(TransactionStats stats, String... metricsPresent) {
        Assert.assertNotNull(stats);

        Map<String, StatsBase> unscoped = stats.getUnscopedStats().getStatsMap();
        for (String stat : metricsPresent) {
            Assert.assertNotNull(stat + " is not present", unscoped.get(stat));
        }
    }

    public void verifyStatsSingleCount(SimpleStatsEngine engine, String... metricsPresent) {
        verifyStatsCount(engine, 1, metricsPresent);
    }

    public void verifyStatsCount(SimpleStatsEngine engine, int count, String... metricsPresent) {
        Assert.assertNotNull(engine);
        for (String current : metricsPresent) {
            Assert.assertEquals(current, count, engine.getOrCreateResponseTimeStats(current).getCallCount());
        }
    }

    public void verifyNoExceptions() {
        Assert.assertNotNull(data);
        Assert.assertNull(data.getThrowable());
        Assert.assertEquals(0, data.getErrorAttributes().size());
    }

    public void verifyTransactionSegmentsBreadthFirst(TransactionData data, String scope,
            String... metricNamesAndThreads) {
        Assert.assertNotNull(data);
        TransactionTrace trace = TransactionTrace.getTransactionTrace(data);
        Assert.assertEquals(scope, trace.getRootMetricName());
        verifyGivenTransactionSegmentBreadthFirst(trace.getRootSegment(), metricNamesAndThreads);
    }

    public void verifyTransactionSegmentsBreadthFirst(String scope, String... metricNamesAndThreads) {
        Assert.assertNotNull(data);

        TransactionTrace trace = TransactionTrace.getTransactionTrace(data);
        Assert.assertEquals(scope, trace.getRootMetricName());
        TransactionSegment root = trace.getRootSegment();
        verifyGivenTransactionSegmentBreadthFirst(root, metricNamesAndThreads);
    }

    private void verifyGivenTransactionSegmentBreadthFirst(TransactionSegment segment, String... metricNamesAndThreads) {
        Assert.assertEquals("ROOT", segment.getMetricName());
        Assert.assertNotNull(segment.getTraceParameters().get("async_context"));

        Queue<TransactionSegment> segments = new LinkedList<>(segment.getChildren());
        int index = 0;
        while (!segments.isEmpty()) {
            TransactionSegment seg = segments.poll();
            Assert.assertEquals(metricNamesAndThreads[index], seg.getMetricName());
            index++;
            String next = metricNamesAndThreads[index];
            if (next.equals(NO_ASYNC_CONTEXT)) {
                Assert.assertNull(seg.getTraceParameters().get("async_context"));
            } else {
                Assert.assertNotNull(seg.getTraceParameters().get("async_context"));
                Assert.assertEquals(metricNamesAndThreads[index], seg.getTraceParameters().get("async_context"));
            }
            index++;
            segments.addAll(seg.getChildren());
        }
        Assert.assertEquals(index, metricNamesAndThreads.length);
    }

    public void verifyTransactionSegmentNodesWithExecContext(Map<String, String> metricToContext) {
        Assert.assertNotNull(data);
        TransactionTrace trace = TransactionTrace.getTransactionTrace(data);
        TransactionSegment root = trace.getRootSegment();

        Queue<TransactionSegment> segments = new LinkedList<>(root.getChildren());
        int index = 0;
        while (!segments.isEmpty()) {
            TransactionSegment seg = segments.poll();
            String context = metricToContext.get(seg.getMetricName());
            if (context != null) {
                Assert.assertNotNull(seg.getTraceParameters().get("async_context"));
                Assert.assertEquals(context, seg.getTraceParameters().get("async_context"));
                index++;
            } else {
                Assert.assertNull(seg.getTraceParameters().get("async_context"));
            }
            segments.addAll(seg.getChildren());
        }
        Assert.assertEquals(index, metricToContext.size());
    }

    public void verifyTransactionSegmentsChildren(String parent, String... children) {
        Assert.assertNotNull(data);
        TransactionTrace trace = TransactionTrace.getTransactionTrace(data);
        TransactionSegment root = trace.getRootSegment();

        Queue<TransactionSegment> segments = new LinkedList<>(root.getChildren());
        int index = 0;
        while (!segments.isEmpty()) {
            TransactionSegment seg = segments.poll();
            if (parent.equals(seg.getMetricName())) {
                Collection<TransactionSegment> actualChildren = seg.getChildren();
                List<String> expectedChildren = Arrays.asList(children);
                for (TransactionSegment current : actualChildren) {
                    Assert.assertTrue(expectedChildren.contains(current.getMetricName()));
                    index++;
                }
            } else {
                segments.addAll(seg.getChildren());
            }
        }
        Assert.assertEquals(index, children.length);
    }

    public void checkStartFinish(int nStarted) {
        Assert.assertNotNull(data);
        Map<String, Object> attributes = data.getUserAttributes();
        Set<String> keys = attributes.keySet();
        for (String key : keys) {
            if (key.startsWith("start-")) {
                Assert.assertNotNull(attributes.get(key.replace("start-", "finish-")));
            }
        }
    }

    protected void dumpData() {
        if (data != null) {
            System.out.println("dumpData(): " + data);
        }
    }

    protected TransactionData getData() {
        return data;
    }

    protected void dumpStats() {
        if (stats != null) {
            System.out.println("dumpStats(): " + stats);
        }
    }

    protected TransactionStats getStats() {
        return stats;
    }

    // return true if s1 and s2 have the same key sets given the application of regex-based fixups.
    protected void assertSameKeys(TransactionStats s1, TransactionStats s2, String[] fixups) {
        assertSameKeys(s1.getUnscopedStats().getStatsMap(), s2.getUnscopedStats().getStatsMap(), fixups);
        assertSameKeys(s1.getScopedStats().getStatsMap(), s2.getScopedStats().getStatsMap(), fixups);
    }

    // return true if m1 and m2 have the same key sets given the application of regex-based fixups.
    protected void assertSameKeys(Map<String, StatsBase> m1, Map<String, StatsBase> m2, String[] fixups) {
        containsAll(m1, m2, fixups);
        containsAll(m2, m1, fixups);
    }

    // return true if m2 contains all the keys in m1, matching by regex when required.
    protected void containsAll(Map<String, StatsBase> m1, Map<String, StatsBase> m2, String[] fixups) {
        for (String key : m1.keySet()) {
            // System.out.println(key + " is a " + m1.get(key));
            if (!m2.containsKey(key)) {
                boolean wasFixed = false;
                for (String fixup : fixups) {
                    if (java.util.regex.Pattern.matches(fixup, key)) {
                        // System.out.println("Fixing up " + key + " using " + fixup);
                        wasFixed = true;
                    }
                }
                Assert.assertTrue(key + " found in m1 but not m2", wasFixed);
            }
        }
    }

    private void waitForTransaction() {
        long start = System.currentTimeMillis();
        // Wait for data to be available
        while ((System.currentTimeMillis() - start) < 5000 && (data == null || stats == null)) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {
            }
        }
    }

    private void waitForTransactionTimesSet(int timesSet) {
        long start = System.currentTimeMillis();
        // Wait for data to be available
        while ((System.currentTimeMillis() - start) < 5000 && (data == null || stats == null || this.timesSet != timesSet)) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {
            }
        }
    }

}
