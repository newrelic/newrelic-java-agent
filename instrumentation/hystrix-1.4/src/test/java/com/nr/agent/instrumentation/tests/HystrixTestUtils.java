/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.tests;

import static rx.Observable.zip;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import org.junit.Assert;

import com.netflix.hystrix.HystrixCommand;
import com.newrelic.agent.MetricNames;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.MetricsHelper;
import com.newrelic.agent.introspec.TraceSegment;
import com.newrelic.agent.introspec.TracedMetricData;
import com.newrelic.agent.introspec.TransactionEvent;
import com.newrelic.agent.introspec.TransactionTrace;
import com.newrelic.api.agent.Trace;
import com.nr.agent.instrumentation.commands.CommandUsingSemaphoreIsolation;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func2;

public class HystrixTestUtils {

    private static final long TIMEOUT = 1000;
    private static int RETRIES = 7;

    public enum CallType {
        QUEUE, EXECUTE, TO_OBSERVABLE, OBSERVE, TO_OBSERVABLE_SWITCH_IF_EMPTY, TO_OBSERVABLE_ZIP
    }

    public enum Failure {
        EXCEPTION, TIMEOUT
    }

    // Total time is less than, greater than, or equal to the response time.
    public enum TimeEvaluation {
        TOTAL_TIME_EQUAL, TOTAL_TIME_LESS, TOTAL_TIME_GREATER
    }

    @Trace(dispatcher = true)
    public static Object runCommand(HystrixCommand command, CallType callType) throws Exception {
        switch (callType) {
        case EXECUTE:
            return command.execute();
        case QUEUE:
            Future<String> myFuture = command.queue();
            return myFuture.get();
        case TO_OBSERVABLE:
            Observable<Object> observe = command.toObservable();
            final StringBuilder sb = new StringBuilder();
            observe.subscribe(new Action1<Object>() {
                @Override
                public void call(Object v) {
                    sb.append(v);
                }
            });
            // wait for the result
            int count = 0;
            while (sb.length() == 0 && count < TIMEOUT) {
                Thread.sleep(10);
                count += 10;
            }
            return sb.toString();
        case TO_OBSERVABLE_SWITCH_IF_EMPTY:
            Observable<Object> observable1 = command.toObservable();
            Observable<Object> observable2 = command.toObservable();

            final StringBuilder sb2 = new StringBuilder();
            observable1.switchIfEmpty(observable2).subscribe(new Action1<Object>() {
                @Override
                public void call(Object v) {
                    sb2.append(v);
                }
            });

            // wait for the result
            int count2 = 0;
            while (sb2.length() == 0 && count2 < TIMEOUT) {
                Thread.sleep(10);
                count2 += 10;
            }
            return sb2.toString();
        case TO_OBSERVABLE_ZIP:
            Observable<String> observableZip1 = command.toObservable();
            Observable<String> observableZip2 = command.toObservable();

            final StringBuilder builder = new StringBuilder();
            zip(observableZip1, observableZip2, new Func2<String, String, String>() {
                @Override
                public String call(String s, String s2) {
                    return builder.append(s).append(s2).toString();
                }
            });

            // wait for the result
            int count3 = 0;
            while (builder.length() == 0 && count3 < TIMEOUT) {
                Thread.sleep(10);
                count3 += 10;
            }
            return builder.toString();
        case OBSERVE:
            Observable<Object> obs = command.observe();
            final StringBuilder sb1 = new StringBuilder();
            obs.subscribe(new Action1<Object>() {
                @Override
                public void call(Object v) {
                    sb1.append(v);
                }
            });
            count = 0;
            while (sb1.length() == 0 && count < TIMEOUT) {
                Thread.sleep(10);
                count += 10;
            }
            return sb1.toString();
        }

        return "";
    }

    public static void verifyEvent(String txClass, TimeEvaluation eval) {
        final String txName = InstrumentationTestRunner.getIntrospector().getTransactionNames().iterator().next();
        Assert.assertEquals("OtherTransaction/Custom/" + txClass + "/runCommand", txName);

        // events
        Assert.assertEquals(0, InstrumentationTestRunner.getIntrospector().getCustomEventTypes().size());
        Collection<TransactionEvent> events = InstrumentationTestRunner.getIntrospector().getTransactionEvents(txName);
        Assert.assertEquals(1, events.size());
        TransactionEvent event = events.iterator().next();
        Assert.assertEquals(txName, event.getName());

        switch (eval) {
        case TOTAL_TIME_EQUAL:
            Assert.assertEquals(event.getDurationInSec(), event.getTotalTimeInSec(), .0001);
            break;
        case TOTAL_TIME_LESS:
            Assert.assertTrue(event.getDurationInSec() > event.getTotalTimeInSec());
            break;
        case TOTAL_TIME_GREATER:
            Assert.assertTrue(event.getDurationInSec() < event.getTotalTimeInSec());
        }
    }

    public static void verifyHystrixMetricsOneCommand(String command, CallType type, boolean isFailure) {
        verifyHystrixMetrics(command, type, isFailure, 1, HystrixTestUtils.class.getName());
    }

    public static void verifyHystrixMetrics(String command, CallType type, boolean failure, int count, String txClass) {
        boolean txnExists = false;
        for (int i = 0; i < RETRIES && !txnExists; i++) {
            txnExists = InstrumentationTestRunner.getIntrospector().getFinishedTransactionCount(TIMEOUT) >= 1;
        }

        if (!txnExists) {
            Assert.fail("Expected at least one transaction");
        }

        boolean foundTx = false;
        String txName = null;
        for (String s : InstrumentationTestRunner.getIntrospector().getTransactionNames()) {
            txName = s;
            if (txName.equals("OtherTransaction/Custom/" + txClass + "/runCommand")) {
                foundTx = true;
            }
        }
        Assert.assertTrue(foundTx);

        // scoped and unscoped metrics
        switch (type) {
        case EXECUTE:
            Assert.assertEquals(count, MetricsHelper.getScopedMetricCount(txName, "Java/" + command + "/execute"));
            Assert.assertEquals(count, MetricsHelper.getUnscopedMetricCount("Java/" + command + "/execute"));
            // purposely fall through
        case QUEUE:
            Assert.assertEquals(count, MetricsHelper.getScopedMetricCount(txName, "Java/" + command + "/queue"));
            Assert.assertEquals(count, MetricsHelper.getUnscopedMetricCount("Java/" + command + "/queue"));
            // purposely fall through
        case TO_OBSERVABLE:
        case OBSERVE:
            Assert.assertEquals(count, MetricsHelper.getScopedMetricCount(txName, "Java/" + command + "/toObservable"));
            Assert.assertEquals(count,
                    MetricsHelper.getScopedMetricCount(txName, "Java/" + command + "/run"));

            Assert.assertEquals(count, MetricsHelper.getUnscopedMetricCount("Java/" + command + "/run"));
            Assert.assertEquals(count, MetricsHelper.getUnscopedMetricCount("Java/" + command + "/toObservable"));
            if (!command.equals(CommandUsingSemaphoreIsolation.class.getName())) {
                Assert.assertEquals(count, MetricsHelper.getScopedMetricCount(txName,
                        "Java/com.netflix.hystrix.strategy.concurrency.HystrixContexSchedulerAction/call"));
                Assert.assertEquals(
                        count,
                        MetricsHelper.getUnscopedMetricCount("Java/com.netflix.hystrix.strategy.concurrency.HystrixContexSchedulerAction/call"));
            }

            if (failure) {
                Assert.assertEquals(count, MetricsHelper.getScopedMetricCount(txName, "Java/" + command
                        + "/getFallback"));
                Assert.assertEquals(count, MetricsHelper.getUnscopedMetricCount("Java/" + command + "/getFallback"));
            } else {
                Assert.assertNull(InstrumentationTestRunner.getIntrospector().getMetricsForTransaction(txName).get(
                        "Java/" + command + "/getFallback"));
                Assert.assertNull(InstrumentationTestRunner.getIntrospector().getUnscopedMetrics().get(
                        "Java/" + command + "/getFallback"));
            }

            if (type == CallType.OBSERVE) {
                Assert.assertEquals(count, MetricsHelper.getScopedMetricCount(txName, "Java/" + command + "/observe"));
                Assert.assertEquals(count, MetricsHelper.getUnscopedMetricCount("Java/" + command + "/observe"));
            }
        }

        Assert.assertNull(InstrumentationTestRunner.getIntrospector().getUnscopedMetrics().get(
                MetricNames.SUPPORTABILITY_ASYNC_TOKEN_TIMEOUT));
    }

    public static void verifyHystrixObservableMetrics(String command, CallType type, boolean failure, int count,
            String txClass) {
        Assert.assertEquals(1, InstrumentationTestRunner.getIntrospector().getFinishedTransactionCount(TIMEOUT));

        // tx name
        final String txName = InstrumentationTestRunner.getIntrospector().getTransactionNames().iterator().next();
        Assert.assertEquals("OtherTransaction/Custom/" + txClass + "/runCommand", txName);

        // scoped metrics
        switch (type) {
        case OBSERVE:
            Assert.assertEquals(count, MetricsHelper.getScopedMetricCount(txName, "Java/" + command + "/observe"));
            // purposely fall through
        case TO_OBSERVABLE:
        case EXECUTE:
        case QUEUE:
            Assert.assertEquals(count, MetricsHelper.getScopedMetricCount(txName, "Java/" + command + "/toObservable"));
            Assert.assertEquals(count, MetricsHelper.getScopedMetricCount(txName, "Java/" + command + "/construct"));
            if (failure) {
                Assert.assertEquals(count, MetricsHelper.getScopedMetricCount(txName, "Java/" + command
                        + "/resumeWithFallback"));
            } else {
                Assert.assertNull(InstrumentationTestRunner.getIntrospector().getMetricsForTransaction(txName).get(
                        "Java/" + command + "/resumeWithFallback"));
            }
            break;
        case TO_OBSERVABLE_SWITCH_IF_EMPTY:
            Assert.assertEquals(count, MetricsHelper.getScopedMetricCount(txName, "Java/" + command + "/toObservable"));
            Assert.assertEquals(count - 1, MetricsHelper.getScopedMetricCount(txName, "Java/" + command + "/construct"));
            if (failure) {
                Assert.assertEquals(count, MetricsHelper.getScopedMetricCount(txName, "Java/" + command
                        + "/resumeWithFallback"));
            } else {
                Assert.assertNull(InstrumentationTestRunner.getIntrospector().getMetricsForTransaction(txName).get(
                        "Java/" + command + "/resumeWithFallback"));
            }
        }

        // unscoped
        switch (type) {
        case OBSERVE:
            Assert.assertEquals(count, MetricsHelper.getScopedMetricCount(txName, "Java/" + command + "/observe"));
            // purposely fall through
        case QUEUE:
        case TO_OBSERVABLE:
        case EXECUTE:
            Assert.assertEquals(count, MetricsHelper.getUnscopedMetricCount("Java/" + command + "/construct"));
            Assert.assertEquals(count, MetricsHelper.getUnscopedMetricCount("Java/" + command + "/toObservable"));
            if (failure) {
                Assert.assertEquals(count, MetricsHelper.getUnscopedMetricCount("Java/" + command
                        + "/resumeWithFallback"));
            } else {
                Assert.assertNull(InstrumentationTestRunner.getIntrospector().getUnscopedMetrics().get(
                        "Java/" + command + "/resumeWithFallback"));
            }
            break;
        case TO_OBSERVABLE_SWITCH_IF_EMPTY:
            Assert.assertEquals(count - 1, MetricsHelper.getUnscopedMetricCount("Java/" + command + "/construct"));
            Assert.assertEquals(count, MetricsHelper.getUnscopedMetricCount("Java/" + command + "/toObservable"));
            if (failure) {
                Assert.assertEquals(count, MetricsHelper.getUnscopedMetricCount("Java/" + command
                        + "/resumeWithFallback"));
            } else {
                Assert.assertNull(InstrumentationTestRunner.getIntrospector().getUnscopedMetrics().get(
                        "Java/" + command + "/resumeWithFallback"));
            }
        }

        Assert.assertNull(InstrumentationTestRunner.getIntrospector().getUnscopedMetrics().get(
                MetricNames.SUPPORTABILITY_ASYNC_TOKEN_TIMEOUT));
    }

    public static void verifyHystrixCacheMetrics(String command, CallType type, int totalCount, int cacheHits,
            String txClass) {
        Assert.assertEquals(1, InstrumentationTestRunner.getIntrospector().getFinishedTransactionCount(TIMEOUT));

        // tx name
        final String txName = InstrumentationTestRunner.getIntrospector().getTransactionNames().iterator().next();
        Assert.assertEquals("OtherTransaction/Custom/" + txClass + "/runCommand", txName);

        // scoped metrics
        switch (type) {
        case EXECUTE:
            Assert.assertEquals(totalCount, MetricsHelper.getScopedMetricCount(txName, "Java/" + command + "/execute"));
            // purposely fall through
        case QUEUE:
            Assert.assertEquals(totalCount, MetricsHelper.getScopedMetricCount(txName, "Java/" + command + "/queue"));
            // purposely fall through
        case TO_OBSERVABLE:
        case OBSERVE:
            Assert.assertEquals((totalCount - cacheHits), MetricsHelper.getScopedMetricCount(txName, "Java/" + command
                    + "/toObservable"));
            if (!command.equals(CommandUsingSemaphoreIsolation.class.getName())) {
                Assert.assertEquals((totalCount - cacheHits), MetricsHelper.getScopedMetricCount(txName,
                        "Java/com.netflix.hystrix.strategy.concurrency.HystrixContexSchedulerAction/call"));
            }
            Assert.assertEquals((totalCount - cacheHits), MetricsHelper.getScopedMetricCount(txName, "Java/" + command
                    + "/run"));
        }

        // unscoped
        switch (type) {
        case EXECUTE:
            Assert.assertEquals(totalCount, MetricsHelper.getUnscopedMetricCount("Java/" + command + "/execute"));
            // purposely fall through
        case QUEUE:
            Assert.assertEquals(totalCount, MetricsHelper.getUnscopedMetricCount("Java/" + command + "/queue"));
            // purposely fall through

        case TO_OBSERVABLE:
        case OBSERVE:
            Assert.assertEquals((totalCount - cacheHits), MetricsHelper.getUnscopedMetricCount("Java/" + command
                    + "/run"));
            Assert.assertEquals((totalCount - cacheHits), MetricsHelper.getUnscopedMetricCount("Java/" + command
                    + "/toObservable"));
            if (!command.equals(CommandUsingSemaphoreIsolation.class.getName())) {
                Assert.assertEquals(
                        (totalCount - cacheHits),
                        MetricsHelper.getUnscopedMetricCount("Java/com.netflix.hystrix.strategy.concurrency.HystrixContexSchedulerAction/call"));
            }
        }

        Assert.assertNull(InstrumentationTestRunner.getIntrospector().getUnscopedMetrics().get(
                MetricNames.SUPPORTABILITY_ASYNC_TOKEN_TIMEOUT));
    }

    public static void verifyCollapseMetrics(String command, String txClass) {
        int txCount = InstrumentationTestRunner.getIntrospector().getFinishedTransactionCount(TIMEOUT);
        Assert.assertTrue(txCount >= 2);

        Collection<String> names = InstrumentationTestRunner.getIntrospector().getTransactionNames();

        String generalTxName = "OtherTransaction/Custom/" + txClass + "/runCommand";
        String batchName = "OtherTransaction/Java/" + command + "$BatchCommand/toObservable";
        String contexSchedulerName = "OtherTransaction/Java/com.netflix.hystrix.strategy.concurrency.HystrixContexSchedulerAction/call";

        Assert.assertEquals(3, names.size());
        Assert.assertTrue(names.contains(generalTxName));
        Assert.assertTrue(names.contains(batchName));
        Assert.assertTrue(names.contains(contexSchedulerName));

        // general scoped and unscoped
        Assert.assertEquals(4, MetricsHelper.getScopedMetricCount(generalTxName, "Java/" + command + "/queue"));
        Assert.assertEquals(4, MetricsHelper.getScopedMetricCount(generalTxName, "Java/" + command + "/toObservable"));
        Assert.assertEquals(4, MetricsHelper.getUnscopedMetricCount("Java/" + command + "/queue"));
        Assert.assertEquals(4, MetricsHelper.getUnscopedMetricCount("Java/" + command + "/toObservable"));

        // batch scoped and unscoped
        int count = MetricsHelper.getScopedMetricCount(contexSchedulerName,
                "Java/com.netflix.hystrix.strategy.concurrency.HystrixContexSchedulerAction/call");
        Assert.assertTrue(1 <= count);
        Assert.assertTrue(4 >= count);
        Assert.assertEquals(
                count,
                MetricsHelper.getUnscopedMetricCount("Java/com.netflix.hystrix.strategy.concurrency.HystrixContexSchedulerAction/call"));

        count = MetricsHelper.getScopedMetricCount(batchName, "Java/" + command + "$BatchCommand/toObservable");
        Assert.assertTrue(1 <= count);
        Assert.assertTrue(4 >= count);
        Assert.assertEquals(count, MetricsHelper.getUnscopedMetricCount("Java/" + command
                + "$BatchCommand/toObservable"));
        count = MetricsHelper.getScopedMetricCount(contexSchedulerName, "Java/" + command + "$BatchCommand/run");
        Assert.assertEquals(count, MetricsHelper.getUnscopedMetricCount("Java/" + command + "$BatchCommand/run"));

        Assert.assertNull(InstrumentationTestRunner.getIntrospector().getUnscopedMetrics().get(
                MetricNames.SUPPORTABILITY_ASYNC_TOKEN_TIMEOUT));
    }

    public static void verifyOneHystrixTransactionTrace(String command, CallType type) {
        final String txName = InstrumentationTestRunner.getIntrospector().getTransactionNames().iterator().next();

        // transaction trace
        Collection<TransactionTrace> traces = InstrumentationTestRunner.getIntrospector().getTransactionTracesForTransaction(txName);
        Assert.assertEquals(1, traces.size());
        TransactionTrace trace = traces.iterator().next();
        TraceSegment segment = trace.getInitialTraceSegment();

        Assert.assertEquals("Java/" + HystrixTestUtils.class.getName() + "/runCommand", segment.getName());
        Assert.assertEquals(1, segment.getCallCount());
        List<TraceSegment> children = segment.getChildren();
        TraceSegment contexSchedulerActionSegment = null;

        switch (type) {
        case EXECUTE:
            Assert.assertEquals(1, children.size());
            segment = children.get(0);

            Assert.assertEquals("Java/" + command + "/execute", segment.getName());
            Assert.assertEquals(1, segment.getCallCount());
            children = segment.getChildren();
            Assert.assertEquals(1, children.size());
            segment = children.get(0);
            // purposely fall through
        case QUEUE:
            Assert.assertEquals(1, children.size());
            segment = children.get(0);

            Assert.assertEquals("Java/" + command + "/queue", segment.getName());
            Assert.assertEquals(1, segment.getCallCount());
            children = segment.getChildren();
            // purposely fall through
        case TO_OBSERVABLE:
            Assert.assertEquals(2, children.size());
            segment = children.get(0);
            contexSchedulerActionSegment = children.get(1);

            Assert.assertEquals("Java/" + command + "/toObservable", segment.getName());
            Assert.assertEquals(1, segment.getCallCount());
            children = segment.getChildren();
            Assert.assertEquals(0, children.size());

            Assert.assertEquals("Java/com.netflix.hystrix.strategy.concurrency.HystrixContexSchedulerAction/call",
                    contexSchedulerActionSegment.getName());
            Assert.assertEquals(1, contexSchedulerActionSegment.getCallCount());
            children = contexSchedulerActionSegment.getChildren();
            Assert.assertEquals(1, children.size());

            segment = children.get(0);
            // JAVA-2383: Subclasses are naming the tx
            // Assert.assertEquals("Java/com.netflix.hystrix.strategy.concurrency.HystrixContexSchedulerAction/call", segment.getName());
            Assert.assertEquals(1, segment.getCallCount());
            children = segment.getChildren();
        }
    }

    public static void verifyTransactionTraceFailure(String command, CallType type, Failure output) {
        final String txName = InstrumentationTestRunner.getIntrospector().getTransactionNames().iterator().next();

        // transaction trace
        Collection<TransactionTrace> traces = InstrumentationTestRunner.getIntrospector().getTransactionTracesForTransaction(
                txName);
        Assert.assertEquals(1, traces.size());
        TransactionTrace trace = traces.iterator().next();
        TraceSegment segment = trace.getInitialTraceSegment();

        Assert.assertEquals("Java/" + HystrixTestUtils.class.getName() + "/runCommand", segment.getName());
        Assert.assertEquals(1, segment.getCallCount());
        List<TraceSegment> children = segment.getChildren();
        if (type == CallType.TO_OBSERVABLE) {
            if (output == Failure.TIMEOUT) {
                Assert.assertEquals(3, children.size());
            } else {
                Assert.assertEquals(2, children.size());
            }
        } else {
            Assert.assertEquals(1, children.size());
        }
        segment = children.remove(0);

        switch (type) {
        case EXECUTE:
            Assert.assertEquals("Java/" + command + "/execute", segment.getName());
            Assert.assertEquals(1, segment.getCallCount());
            children = segment.getChildren();
            Assert.assertEquals(1, children.size());
            segment = children.get(0);
            // purposely fall through
        case QUEUE:
            Assert.assertEquals("Java/" + command + "/queue", segment.getName());
            Assert.assertEquals(1, segment.getCallCount());
            children = segment.getChildren();
            if (output == Failure.TIMEOUT) {
                Assert.assertEquals(3, children.size());
            } else {
                Assert.assertEquals(2, children.size());
            }
            segment = children.remove(0);
            // purposely fall through
        case TO_OBSERVABLE:
            Assert.assertEquals("Java/" + command + "/toObservable", segment.getName());
            Assert.assertEquals(1, segment.getCallCount());

            if (output == Failure.TIMEOUT) {
                Assert.assertEquals(2, children.size());

                for (TraceSegment seg : children) {
                    if (seg.getName().equals(
                            "Java/com.netflix.hystrix.strategy.concurrency.HystrixContexSchedulerAction/call")) {
                        Assert.assertEquals(1, seg.getCallCount());
                        children = seg.getChildren();
                        Assert.assertEquals(1, children.size());
                        seg = children.get(0);

                        Assert.assertEquals("Java/" + command + "/run", seg.getName());
                        Assert.assertEquals(1, seg.getCallCount());
                        children = seg.getChildren();
                        Assert.assertEquals(0, children.size());
                    } else {
                        Assert.assertEquals("Java/" + command + "/getFallback", seg.getName());
                        Assert.assertEquals(1, seg.getCallCount());
                        children = seg.getChildren();
                        Assert.assertEquals(0, children.size());
                    }
                }
            } else {
                Assert.assertEquals(1, children.size());
                segment = children.get(0);

                Assert.assertEquals("Java/com.netflix.hystrix.strategy.concurrency.HystrixContexSchedulerAction/call",
                        segment.getName());
                Assert.assertEquals(1, segment.getCallCount());
                children = segment.getChildren();
                Assert.assertEquals(2, children.size());

                for (TraceSegment seg : children) {
                    if (seg.getName().equals("Java/" + command + "/run")) {
                        Assert.assertEquals(1, seg.getCallCount());
                        children = seg.getChildren();
                        Assert.assertEquals(0, children.size());

                    } else {
                        Assert.assertEquals("Java/" + command + "/getFallback", seg.getName());
                        Assert.assertEquals(1, seg.getCallCount());
                        children = seg.getChildren();
                        Assert.assertEquals(0, children.size());
                    }
                }
            }
        }
    }
}
