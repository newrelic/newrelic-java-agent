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
import com.newrelic.agent.TransactionErrorPriority;
import com.newrelic.agent.introspec.Error;
import com.newrelic.agent.introspec.ErrorEvent;
import com.newrelic.agent.introspec.Event;
import com.newrelic.agent.introspec.TracedMetricData;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.agent.tracers.OtherRootTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.metricname.SimpleMetricNameFormat;

public class IntrospectorImplTest {
    private static final int RETURN_OPCODE = 177;

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
        Transaction.getTransaction();
        long startNs = System.nanoTime();
        Tracer rootTracer = createOtherTracer("rootOnly");
        Transaction.getTransaction().getTransactionActivity().tracerStarted(rootTracer);
        rootTracer.finish(RETURN_OPCODE, 0);
        long diffNsRootOnly = System.nanoTime() - startNs;

        // data check
        Collection<String> txNames = impl.getTransactionNames();
        Assert.assertEquals(1, txNames.size());
        Assert.assertEquals("OtherTransaction/rootOnly", txNames.iterator().next());
        Map<String, TracedMetricData> metrics = impl.getMetricsForTransaction("OtherTransaction/rootOnly");
        Assert.assertEquals(1, metrics.size());
        TracedMetricData data = metrics.get("Java/java.lang.Object/rootOnly");
        Assert.assertEquals("Java/java.lang.Object/rootOnly", data.getName());
        Assert.assertEquals(1, data.getCallCount());
        Assert.assertEquals(data.getTotalTimeInSec(), data.getExclusiveTimeInSec(), .000000001);
        Assert.assertTrue(data.getTotalTimeInSec() < (diffNsRootOnly / 1000000000.0));

        Collection<com.newrelic.agent.introspec.TransactionEvent> events = impl.getTransactionEvents("OtherTransaction/rootOnly");
        Assert.assertNotNull(events);
        Assert.assertEquals(1, events.size());
        com.newrelic.agent.introspec.TransactionEvent event = events.iterator().next();
        Assert.assertEquals("OtherTransaction/rootOnly", event.getName());
        Assert.assertEquals(0, event.getDatabaseCallCount(), .00001);
        Assert.assertEquals(0, event.getExternalCallCount(), .00001);
        Assert.assertEquals(event.getDurationInSec(), data.getTotalTimeInSec(), .00000001);
        Assert.assertEquals(event.getDurationInSec(), event.getTotalTimeInSec(), .0000001);

        Map<String, TracedMetricData> unscoped = impl.getUnscopedMetrics();
        Assert.assertNotNull(unscoped);
        TracedMetricData stats = unscoped.get("OtherTransactionTotalTime");
        Assert.assertNotNull(stats);
        Assert.assertEquals(1, stats.getCallCount());
        Assert.assertEquals(event.getDurationInSec(), stats.getTotalTimeInSec(), .0000001);

        stats = unscoped.get("OtherTransactionTotalTime/rootOnly");
        Assert.assertNotNull(stats);
        Assert.assertEquals(1, stats.getCallCount());
        Assert.assertEquals(event.getDurationInSec(), stats.getTotalTimeInSec(), .0000001);

        stats = unscoped.get("OtherTransaction/rootOnly");
        Assert.assertNotNull(stats);
        Assert.assertEquals(1, stats.getCallCount());
        Assert.assertEquals(event.getDurationInSec(), stats.getTotalTimeInSec(), .0000001);

        stats = unscoped.get("OtherTransaction/all");
        Assert.assertNotNull(stats);
        Assert.assertEquals(1, stats.getCallCount());
        Assert.assertEquals(event.getDurationInSec(), stats.getTotalTimeInSec(), .0000001);

        // second transaction
        startNs = System.nanoTime();
        rootTracer = createOtherTracer("secondTransaction");
        Transaction.getTransaction().getTransactionActivity().tracerStarted(rootTracer);
        rootTracer.finish(RETURN_OPCODE, 0);
        long diffNsSecondTransaction = System.nanoTime() - startNs;

        // data check
        txNames = impl.getTransactionNames();
        Assert.assertEquals(2, txNames.size());
        metrics = impl.getMetricsForTransaction("OtherTransaction/rootOnly");
        Assert.assertEquals(1, metrics.size());
        data = metrics.get("Java/java.lang.Object/rootOnly");
        Assert.assertEquals("Java/java.lang.Object/rootOnly", data.getName());
        Assert.assertEquals(1, data.getCallCount());
        Assert.assertEquals(data.getTotalTimeInSec(), data.getExclusiveTimeInSec(), .000000001);

        final double diffInSeconds = diffNsRootOnly / 1000000000.0;
        final String msg = "Total duration in seconds is greater than diff. Duration in sec " + data.getTotalTimeInSec() + " diff: " + diffInSeconds;
        Assert.assertTrue(msg, data.getTotalTimeInSec() < diffInSeconds);

        metrics = impl.getMetricsForTransaction("OtherTransaction/secondTransaction");
        Assert.assertEquals(1, metrics.size());
        data = metrics.get("Java/java.lang.Object/secondTransaction");
        Assert.assertEquals("Java/java.lang.Object/secondTransaction", data.getName());
        Assert.assertEquals(1, data.getCallCount());
        Assert.assertEquals(data.getTotalTimeInSec(), data.getExclusiveTimeInSec(), .000000001);
        Assert.assertTrue(data.getTotalTimeInSec() < (diffNsSecondTransaction / 1000000000.0));

        events = impl.getTransactionEvents("OtherTransaction/secondTransaction");
        Assert.assertNotNull(events);
        Assert.assertEquals(1, events.size());
        event = events.iterator().next();
        Assert.assertEquals("OtherTransaction/secondTransaction", event.getName());
        Assert.assertEquals(0, event.getDatabaseCallCount(), .00001);
        Assert.assertEquals(0, event.getExternalCallCount(), .00001);
        Assert.assertEquals(event.getDurationInSec(), data.getTotalTimeInSec(), .00000001);
        Assert.assertEquals(event.getDurationInSec(), event.getTotalTimeInSec(), .0000001);

    }

    @Test
    public void testMultipleTracersInTransaction() {

        // transaction
        Transaction.getTransaction();
        long startNs = System.nanoTime();
        Tracer rootTracer = createOtherTracer("rootOnly1");
        Transaction.getTransaction().getTransactionActivity().tracerStarted(rootTracer);

        Tracer child1 = createDefaultTracer("default1");
        Transaction.getTransaction().getTransactionActivity().tracerStarted(child1);
        child1.finish(RETURN_OPCODE, 0);

        child1 = createDefaultTracer("default1");
        Transaction.getTransaction().getTransactionActivity().tracerStarted(child1);
        child1.finish(RETURN_OPCODE, 0);

        rootTracer.finish(RETURN_OPCODE, 0);
        long diffNs = System.nanoTime() - startNs;

        // data check
        Collection<String> txNames = impl.getTransactionNames();
        Assert.assertEquals(1, txNames.size());
        Assert.assertEquals("OtherTransaction/rootOnly1", txNames.iterator().next());
        Map<String, TracedMetricData> metrics = impl.getMetricsForTransaction("OtherTransaction/rootOnly1");
        Assert.assertEquals(2, metrics.size());

        TracedMetricData data = metrics.get("Java/java.lang.Object/rootOnly1");
        Assert.assertNotNull(data);
        Assert.assertEquals("Java/java.lang.Object/rootOnly1", data.getName());
        Assert.assertEquals(1, data.getCallCount());
        Assert.assertTrue(data.getTotalTimeInSec() > data.getExclusiveTimeInSec());
        Assert.assertTrue(data.getTotalTimeInSec() < (diffNs / 1000000000.0));

        data = metrics.get("Custom/default1");
        Assert.assertNotNull(data);
        Assert.assertEquals("Custom/default1", data.getName());
        Assert.assertEquals(2, data.getCallCount());
        Assert.assertEquals(data.getTotalTimeInSec(), data.getExclusiveTimeInSec(), .000000001);
        Assert.assertTrue(data.getTotalTimeInSec() < (diffNs / 1000000000.0));

        // second transaction - same as first
        Transaction.clearTransaction();
        Transaction.getTransaction();
        rootTracer = createOtherTracer("rootOnly1");
        Transaction.getTransaction().getTransactionActivity().tracerStarted(rootTracer);

        child1 = createDefaultTracer("default1");
        Transaction.getTransaction().getTransactionActivity().tracerStarted(child1);
        child1.finish(RETURN_OPCODE, 0);

        child1 = createDefaultTracer("default1");
        Transaction.getTransaction().getTransactionActivity().tracerStarted(child1);
        child1.finish(RETURN_OPCODE, 0);

        rootTracer.finish(RETURN_OPCODE, 0);

        // data check
        Assert.assertEquals(2, impl.getFinishedTransactionCount());
        txNames = impl.getTransactionNames();
        Assert.assertEquals(1, txNames.size());
        Assert.assertEquals("OtherTransaction/rootOnly1", txNames.iterator().next());
        metrics = impl.getMetricsForTransaction("OtherTransaction/rootOnly1");
        Assert.assertEquals(2, metrics.size());

        data = metrics.get("Java/java.lang.Object/rootOnly1");
        Assert.assertNotNull(data);
        Assert.assertEquals("Java/java.lang.Object/rootOnly1", data.getName());
        Assert.assertEquals(2, data.getCallCount());
        Assert.assertTrue(data.getTotalTimeInSec() > data.getExclusiveTimeInSec());

        data = metrics.get("Custom/default1");
        Assert.assertNotNull(data);
        Assert.assertEquals("Custom/default1", data.getName());
        Assert.assertEquals(4, data.getCallCount());
        Assert.assertEquals(data.getTotalTimeInSec(), data.getExclusiveTimeInSec(), .000000001);

    }

    @Test
    public void testMultChildrenTransaction() {

        // transaction
        Transaction.getTransaction();
        long startNs = System.nanoTime();
        Tracer rootTracer = createOtherTracer("rootOnly");
        Transaction.getTransaction().getTransactionActivity().tracerStarted(rootTracer);

        Tracer child1 = createDefaultTracer("default1");
        Transaction.getTransaction().getTransactionActivity().tracerStarted(child1);

        Tracer child2 = createDefaultTracer("default2");
        Transaction.getTransaction().getTransactionActivity().tracerStarted(child2);
        child2.finish(RETURN_OPCODE, 0);

        child1.finish(RETURN_OPCODE, 0);

        rootTracer.finish(RETURN_OPCODE, 0);
        long diffNs = System.nanoTime() - startNs;

        // data check
        Collection<String> txNames = impl.getTransactionNames();
        Assert.assertEquals(1, txNames.size());
        Assert.assertEquals("OtherTransaction/rootOnly", txNames.iterator().next());
        Map<String, TracedMetricData> metrics = impl.getMetricsForTransaction("OtherTransaction/rootOnly");
        Assert.assertEquals(3, metrics.size());

        TracedMetricData init = metrics.get("Java/java.lang.Object/rootOnly");
        Assert.assertNotNull(init);
        Assert.assertEquals("Java/java.lang.Object/rootOnly", init.getName());
        Assert.assertEquals(1, init.getCallCount());
        Assert.assertTrue(init.getTotalTimeInSec() > init.getExclusiveTimeInSec());
        Assert.assertTrue(init.getTotalTimeInSec() < (diffNs / 1000000000.0));

        TracedMetricData data1 = metrics.get("Custom/default1");
        Assert.assertNotNull(data1);
        Assert.assertEquals("Custom/default1", data1.getName());
        Assert.assertEquals(1, data1.getCallCount());
        Assert.assertNotEquals(data1.getTotalTimeInSec(), data1.getExclusiveTimeInSec(), .000000001);
        Assert.assertTrue(data1.getTotalTimeInSec() < init.getTotalTimeInSec());

        TracedMetricData data2 = metrics.get("Custom/default2");
        Assert.assertNotNull(data2);
        Assert.assertEquals("Custom/default2", data2.getName());
        Assert.assertEquals(1, data2.getCallCount());
        Assert.assertEquals(data2.getTotalTimeInSec(), data2.getExclusiveTimeInSec(), .000000001);
        Assert.assertEquals(data2.getTotalTimeInSec(), (data1.getTotalTimeInSec() - data1.getExclusiveTimeInSec()),
                .00000001);
    }

    @Test
    public void testErrorInTx() {
        // transaction
        Transaction.getTransaction();
        Tracer rootTracer = createOtherTracer("rootOnly");
        Transaction.getTransaction().getTransactionActivity().tracerStarted(rootTracer);
        Throwable throwable = new Throwable("MyTest");
        Transaction.getTransaction().setThrowable(throwable, TransactionErrorPriority.TRACER, false);
        rootTracer.finish(RETURN_OPCODE, 0);

        // data check
        Collection<String> txNames = impl.getTransactionNames();
        Assert.assertEquals(1, txNames.size());
        Assert.assertEquals("OtherTransaction/rootOnly", txNames.iterator().next());
        Collection<Error> errors1 = impl.getErrors();
        Collection<Error> errors2 = impl.getErrorsForTransaction("OtherTransaction/rootOnly");
        Assert.assertEquals(1, errors1.size());
        Assert.assertEquals(1, errors2.size());
        Error error1 = errors1.iterator().next();
        Assert.assertEquals(error1, errors2.iterator().next());
        Assert.assertEquals("MyTest", error1.getErrorMessage());
        Assert.assertEquals(throwable, error1.getThrowable());
        Assert.assertEquals(1, impl.getTransactionEvents(txNames.iterator().next()).size());

        Collection<ErrorEvent> events = impl.getErrorEvents();
        Assert.assertEquals(1, events.size());
        ErrorEvent event1 = events.iterator().next();
        Assert.assertEquals("TransactionError", event1.getType());
        Assert.assertEquals("MyTest", event1.getErrorMessage());
        Assert.assertEquals("java.lang.Throwable", event1.getErrorClass());
        Assert.assertEquals("OtherTransaction/rootOnly", event1.getTransactionName());

        Collection<ErrorEvent> txEvents = impl.getErrorEventsForTransaction("OtherTransaction/rootOnly");
        Assert.assertEquals(1, txEvents.size());
        Event event2 = events.iterator().next();
        Assert.assertEquals("TransactionError", event2.getType());
        Assert.assertEquals(event1, event2);

    }

    @Test
    public void testErrorOutsideTransaction() {
        Throwable throwable = new Throwable("MyTest");
        ServiceFactory.getRPMService().getErrorService().reportException(throwable);

        // data check
        Collection<String> txNames = impl.getTransactionNames();
        Assert.assertEquals(0, txNames.size());
        Collection<Error> errors1 = impl.getErrors();
        Assert.assertEquals(1, errors1.size());
        Error error1 = errors1.iterator().next();
        Assert.assertEquals("MyTest", error1.getErrorMessage());
        Assert.assertEquals(throwable, error1.getThrowable());

        Collection<ErrorEvent> events = impl.getErrorEvents();
        Assert.assertEquals(1, events.size());
        ErrorEvent event = events.iterator().next();
        // well that seems wrong - but that seems to be what we name all errors
        Assert.assertEquals("TransactionError", event.getType());
        Assert.assertEquals("MyTest", event.getErrorMessage());
        Assert.assertEquals("java.lang.Throwable", event.getErrorClass());
        Assert.assertEquals("Unknown", event.getTransactionName());

    }

    @Test
    public void testUnscopedInTransaction() {
        // transaction
        Transaction.getTransaction();
        Tracer rootTracer = createOtherTracer("rootOnly");
        Transaction.getTransaction().getTransactionActivity().tracerStarted(rootTracer);
        Transaction.getTransaction().getMetricAggregator().incrementCounter("MyCounter");
        ServiceFactory.getStatsService().getMetricAggregator().incrementCounter("MySecondCounter");
        rootTracer.finish(RETURN_OPCODE, 0);

        // data check
        Map<String, TracedMetricData> unscoped = impl.getUnscopedMetrics();
        Assert.assertNotNull(unscoped);
        TracedMetricData stats = unscoped.get("MyCounter");
        Assert.assertNotNull(stats);
        Assert.assertEquals(1, stats.getCallCount());
        Assert.assertEquals(0, stats.getTotalTimeInSec(), .0000001);

        stats = unscoped.get("MySecondCounter");
        Assert.assertNotNull(stats);
        Assert.assertEquals(1, stats.getCallCount());
        Assert.assertEquals(0, stats.getTotalTimeInSec(), .0000001);
    }

    @Test
    public void testUnscopedOutsideTransaction() {
        ServiceFactory.getStatsService().getMetricAggregator().incrementCounter("MyCounter11");
        ServiceFactory.getStatsService().getMetricAggregator().recordMetric("MyRecordMetric", 5.33f);
        ServiceFactory.getStatsService().getMetricAggregator().recordResponseTimeMetric("MyResponse/Time", 5000);

        // data check
        Map<String, TracedMetricData> unscoped = impl.getUnscopedMetrics();
        Assert.assertNotNull(unscoped);
        TracedMetricData stats = unscoped.get("MyCounter11");
        Assert.assertNotNull(stats);
        Assert.assertEquals(1, stats.getCallCount());
        Assert.assertEquals(0, stats.getTotalTimeInSec(), .0000001);

        stats = unscoped.get("MyRecordMetric");
        Assert.assertNotNull(stats);
        Assert.assertEquals(1, stats.getCallCount());
        Assert.assertEquals(5.33f, stats.getTotalTimeInSec(), .001);

        stats = unscoped.get("MyResponse/Time");
        Assert.assertNotNull(stats);
        Assert.assertEquals(1, stats.getCallCount());
        Assert.assertEquals(5, stats.getTotalTimeInSec(), .001);

        // add data again
        ServiceFactory.getStatsService().getMetricAggregator().incrementCounter("MyCounter11");
        ServiceFactory.getStatsService().getMetricAggregator().recordMetric("MyRecordMetric", 5.22f);
        ServiceFactory.getStatsService().getMetricAggregator().recordResponseTimeMetric("MyResponse/Time", 7000);
        ServiceFactory.getStatsService().getMetricAggregator().recordResponseTimeMetric("MyResponse/Time", 7000);

        // data check
        unscoped = impl.getUnscopedMetrics();
        Assert.assertNotNull(unscoped);
        stats = unscoped.get("MyCounter11");
        Assert.assertNotNull(stats);
        Assert.assertEquals(2, stats.getCallCount());
        Assert.assertEquals(0, stats.getTotalTimeInSec(), .0000001);

        stats = unscoped.get("MyRecordMetric");
        Assert.assertNotNull(stats);
        Assert.assertEquals(2, stats.getCallCount());
        Assert.assertEquals(10.55f, stats.getTotalTimeInSec(), .001);

        stats = unscoped.get("MyResponse/Time");
        Assert.assertNotNull(stats);
        Assert.assertEquals(3, stats.getCallCount());
        Assert.assertEquals(19, stats.getTotalTimeInSec(), .001);
    }

    public static OtherRootTracer createOtherTracer(String methodName) {
        Transaction tx = Transaction.getTransaction();
        ClassMethodSignature sig = new ClassMethodSignature("MyClass", methodName, "()V");
        OtherRootTracer brrt = new OtherRootTracer(tx, sig, new Object(), new SimpleMetricNameFormat(methodName));
        return brrt;
    }

    // Create a Tracer for tests that require one.
    public static DefaultTracer createDefaultTracer(String methodName) {
        Transaction tx = Transaction.getTransaction();
        ClassMethodSignature sig = new ClassMethodSignature("MyClass", methodName, "()V");
        DefaultTracer brrt = new DefaultTracer(tx, sig, new Object(),
                new SimpleMetricNameFormat("Custom/" + methodName));
        return brrt;
    }
}
