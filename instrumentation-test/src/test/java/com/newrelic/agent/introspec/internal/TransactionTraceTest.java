/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.introspec.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.deps.org.objectweb.asm.Opcodes;
import com.newrelic.agent.introspec.TraceSegment;
import com.newrelic.agent.introspec.TracedMetricData;
import com.newrelic.agent.introspec.TransactionTrace;
import com.newrelic.agent.tracers.Tracer;

public class TransactionTraceTest {

    private IntrospectorImpl impl = IntrospectorImpl.createIntrospector(Collections.<String, Object>emptyMap());

    @Before
    public void setup() {
        impl.clear();
    }

    @After
    public void afterTest() {
        Transaction.clearTransaction();
    }

    @Test
    public void testOneTracerInTransaction() {
        // transaction
        long start = System.currentTimeMillis();
        Transaction.getTransaction();
        Tracer rootTracer = IntrospectorImplTest.createOtherTracer("rootOnly");
        Transaction.getTransaction().getTransactionActivity().tracerStarted(rootTracer);
        rootTracer.finish(Opcodes.RETURN, 0);
        long end = System.currentTimeMillis();

        // data check
        Map<String, TracedMetricData> metrics = impl.getUnscopedMetrics();
        TracedMetricData txMetric = metrics.get("OtherTransaction/rootOnly");
        Collection<TransactionTrace> traces = impl.getTransactionTracesForTransaction("OtherTransaction/rootOnly");
        Assert.assertEquals(1, traces.size());
        TransactionTrace trace = traces.iterator().next();
        Assert.assertEquals(txMetric.getTotalTimeInSec(), trace.getResponseTimeInSec(), .01);
        Assert.assertEquals(trace.getWallClockDurationInSec(), trace.getResponseTimeInSec(), .01);

        Assert.assertTrue(trace.getStartTime() >= start);
        Assert.assertTrue(trace.getStartTime() <= end);

        TraceSegment segment = trace.getInitialTraceSegment();
        Assert.assertEquals(0, segment.getChildren().size());
        Map<String, Object> info = segment.getTracerAttributes();
        Assert.assertNotNull(info.get("async_context"));
        Assert.assertNotNull(info.get("exclusive_duration_millis"));
    }

    @Test
    public void testOneTracerInTransactionWithChildren() {
        // transaction
        long start = System.currentTimeMillis();
        Transaction.getTransaction();
        Tracer rootTracer = IntrospectorImplTest.createOtherTracer("rootOnly");

        Transaction.getTransaction().getTransactionActivity().tracerStarted(rootTracer);

        Tracer child1 = IntrospectorImplTest.createDefaultTracer("default1");
        Transaction.getTransaction().getTransactionActivity().tracerStarted(child1);
        child1.finish(Opcodes.RETURN, 0);

        Tracer child2 = IntrospectorImplTest.createDefaultTracer("default2");
        Transaction.getTransaction().getTransactionActivity().tracerStarted(child2);
        child2.finish(Opcodes.RETURN, 0);

        Tracer child3 = IntrospectorImplTest.createDefaultTracer("default3");
        Transaction.getTransaction().getTransactionActivity().tracerStarted(child3);
        child3.setAgentAttribute("myatt", "hello");
        child3.setAgentAttribute("myNumber", 99);
        child3.finish(Opcodes.RETURN, 0);

        rootTracer.finish(Opcodes.RETURN, 0);
        long end = System.currentTimeMillis();

        // data check
        Assert.assertEquals(1, impl.getFinishedTransactionCount());
        Map<String, TracedMetricData> metrics = impl.getUnscopedMetrics();
        TracedMetricData txMetric = metrics.get("OtherTransaction/rootOnly");
        Collection<TransactionTrace> traces = impl.getTransactionTracesForTransaction("OtherTransaction/rootOnly");
        Assert.assertEquals(1, traces.size());
        TransactionTrace trace = traces.iterator().next();
        Assert.assertEquals(txMetric.getTotalTimeInSec(), trace.getResponseTimeInSec(), .01);
        Assert.assertEquals(trace.getWallClockDurationInSec(), trace.getResponseTimeInSec(), .01);

        Assert.assertTrue(trace.getStartTime() >= start);
        Assert.assertTrue(trace.getStartTime() <= end);

        TraceSegment segment = trace.getInitialTraceSegment();
        Assert.assertEquals(3, segment.getChildren().size());
        long relativeStart = segment.getRelativeStartTime();
        long relativeEnd = segment.getRelativeEndTime();

        for (TraceSegment current : segment.getChildren()) {
            Map<String, Object> info = current.getTracerAttributes();
            Assert.assertNull(info.get("async_context"));
            Assert.assertNotNull(info.get("exclusive_duration_millis"));
            Assert.assertEquals(0, current.getChildren().size());
            Assert.assertTrue(current.getMethodName().startsWith("default"));
            Assert.assertTrue(current.getName().startsWith("Custom/default"));
            Assert.assertTrue(current.getRelativeStartTime() >= relativeStart);
            Assert.assertTrue(current.getRelativeEndTime() <= relativeEnd);
            Assert.assertTrue(current.getRelativeStartTime() <= current.getRelativeEndTime());
            Assert.assertEquals("MyClass", current.getClassName());
            if (current.getName().equals("Custom/default3")) {
                Assert.assertEquals("hello", info.get("myatt"));
                Assert.assertEquals(99, info.get("myNumber"));
            }
        }
    }

    @Test
    public void testOneTracerInTransactionWithDepthChildren() {
        // transaction
        long start = System.currentTimeMillis();
        Transaction.getTransaction();
        Tracer rootTracer = IntrospectorImplTest.createOtherTracer("rootOnly");

        Transaction.getTransaction().getTransactionActivity().tracerStarted(rootTracer);

        Tracer child1 = IntrospectorImplTest.createDefaultTracer("default1");
        Transaction.getTransaction().getTransactionActivity().tracerStarted(child1);

        Tracer child2 = IntrospectorImplTest.createDefaultTracer("default2");
        Transaction.getTransaction().getTransactionActivity().tracerStarted(child2);

        Tracer child3 = IntrospectorImplTest.createDefaultTracer("default3");
        Transaction.getTransaction().getTransactionActivity().tracerStarted(child3);
        child3.setAgentAttribute("myatt", "hello");
        child3.setAgentAttribute("myNumber", 99);
        child3.finish(Opcodes.RETURN, 0);

        child2.finish(Opcodes.RETURN, 0);
        child1.finish(Opcodes.RETURN, 0);
        rootTracer.finish(Opcodes.RETURN, 0);
        long end = System.currentTimeMillis();

        // data check
        Assert.assertEquals(1, impl.getFinishedTransactionCount());
        Map<String, TracedMetricData> metrics = impl.getUnscopedMetrics();
        TracedMetricData txMetric = metrics.get("OtherTransaction/rootOnly");
        Collection<TransactionTrace> traces = impl.getTransactionTracesForTransaction("OtherTransaction/rootOnly");
        Assert.assertEquals(1, traces.size());
        TransactionTrace trace = traces.iterator().next();
        Assert.assertEquals(txMetric.getTotalTimeInSec(), trace.getResponseTimeInSec(), .01);
        Assert.assertEquals(trace.getWallClockDurationInSec(), trace.getResponseTimeInSec(), .01);

        Assert.assertTrue(trace.getStartTime() >= start);
        Assert.assertTrue(trace.getStartTime() <= end);

        TraceSegment segment = trace.getInitialTraceSegment();
        Assert.assertEquals(1, segment.getChildren().size());
        TraceSegment seg = segment.getChildren().get(0);
        long relativeStart = segment.getRelativeStartTime();
        long relativeEnd = segment.getRelativeEndTime();
        int count = 0;
        while (seg != null) {
            count++;
            Assert.assertTrue(seg.getRelativeStartTime() >= relativeStart);
            Assert.assertTrue(seg.getRelativeEndTime() <= relativeEnd);
            Assert.assertTrue(seg.getRelativeStartTime() <= seg.getRelativeEndTime());
            relativeStart = seg.getRelativeStartTime();
            relativeEnd = seg.getRelativeEndTime();
            Map<String, Object> info = seg.getTracerAttributes();
            Assert.assertNull(info.get("async_context"));
            Assert.assertNotNull(info.get("exclusive_duration_millis"));
            Assert.assertTrue(seg.getMethodName().startsWith("default"));
            Assert.assertTrue(seg.getName().startsWith("Custom/default"));
            Assert.assertEquals("MyClass", seg.getClassName());
            if (seg.getName().equals("Custom/default3")) {
                Assert.assertEquals("hello", info.get("myatt"));
                Assert.assertEquals(99, info.get("myNumber"));
                Assert.assertEquals(0, seg.getChildren().size());
                seg = null;
            } else {
                Assert.assertEquals(1, seg.getChildren().size());
                seg = seg.getChildren().get(0);
            }
        }
        Assert.assertEquals(3, count);
    }
}
