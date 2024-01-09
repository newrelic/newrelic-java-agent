/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Queues;
import com.newrelic.agent.attributes.AttributesService;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.ExitTracer;
import com.newrelic.agent.bridge.Instrumentation;
import com.newrelic.agent.bridge.TracedActivity;
import com.newrelic.agent.bridge.TracedMethod;
import com.newrelic.agent.config.AgentConfigFactory;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.ConfigServiceFactory;
import com.newrelic.agent.config.TransactionTracerConfigImpl;
import com.newrelic.agent.environment.EnvironmentService;
import com.newrelic.agent.environment.EnvironmentServiceImpl;
import com.newrelic.agent.instrumentation.InstrumentationImpl;
import com.newrelic.agent.interfaces.SamplingPriorityQueue;
import com.newrelic.agent.model.SpanEvent;
import com.newrelic.agent.model.TimeoutCause;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.SpanEventsServiceFactory;
import com.newrelic.agent.service.analytics.SpanEventCreationDecider;
import com.newrelic.agent.service.analytics.SpanEventsService;
import com.newrelic.agent.service.analytics.SpanEventsServiceImpl;
import com.newrelic.agent.service.analytics.TransactionDataToDistributedTraceIntrinsics;
import com.newrelic.agent.service.analytics.TransactionEvent;
import com.newrelic.agent.service.analytics.TransactionEventsService;
import com.newrelic.agent.service.async.AsyncTransactionService;
import com.newrelic.agent.sql.SqlTraceService;
import com.newrelic.agent.sql.SqlTraceServiceImpl;
import com.newrelic.agent.stats.ResponseTimeStats;
import com.newrelic.agent.stats.StatsService;
import com.newrelic.agent.stats.StatsServiceImpl;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.agent.trace.TransactionTraceService;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.agent.tracers.OtherRootTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracing.DistributedTraceServiceImpl;
import com.newrelic.agent.transaction.TransactionCounts;
import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.OutboundHeaders;
import com.newrelic.api.agent.TransactionNamePriority;
import com.newrelic.test.marker.RequiresFork;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@Category(RequiresFork.class)
public class SegmentTest implements ExtendedTransactionListener {

    private static InlineExpirationService inlineExpirationService;
    private Queue<Transaction> runningTransactions;

    private static Instrumentation savedInstrumentation;
    private static com.newrelic.agent.bridge.Agent savedAgent;

    private static final String APP_NAME = "Unit Test";

    // Wrap the ExpirationService so we can know when a segment has fully ended
    private static class InlineExpirationService extends ExpirationService {

        private static final AtomicLong timeout = new AtomicLong(1);

        public void setTimeout(long timeoutInSeconds) {
            timeout.set(timeoutInSeconds);
        }

        public void clearTimeout() {
            timeout.set(1);
        }

        @Override
        public Future<?> expireSegment(Runnable runnable) {
            Future<?> result = super.expireSegment(runnable);
            if (timeout.get() > 0) {
                try {
                    result.get(timeout.get(), TimeUnit.SECONDS);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return result;
        }
    }

    @BeforeClass
    public static void setup() throws Exception {
        inlineExpirationService = new InlineExpirationService();
        createServiceManager(createConfigMap(), inlineExpirationService);

        savedInstrumentation = AgentBridge.instrumentation;
        savedAgent = AgentBridge.agent;

        AgentBridge.instrumentation = new InstrumentationImpl(Agent.LOG);
        AgentBridge.agent = new AgentImpl(Agent.LOG);
    }

    @AfterClass
    public static void afterClass() {
        AgentBridge.instrumentation = savedInstrumentation;
        AgentBridge.agent = savedAgent;
    }

    @Before
    public void before() {
        runningTransactions = null;
        ServiceFactory.getTransactionService().addTransactionListener(this);
    }

    @After
    public void cleanup() {
        TransactionActivity.clear();
        SpanEventsServiceImpl spanEventsService = (SpanEventsServiceImpl) ServiceFactory.getSpanEventService();
        spanEventsService.clearReservoir();

        Transaction.clearTransaction();
        ServiceFactory.getTransactionService().removeTransactionListener(this);
    }

    /**
     * Creates a transaction with one tracer and returns the root tracer (unfinished).
     */
    private static Tracer makeTransaction() {
        Transaction tx = Transaction.getTransaction(true);
        TransactionActivity txa = TransactionActivity.get();
        Assert.assertNotNull(txa);
        Tracer root = new OtherRootTracer(tx, new ClassMethodSignature("com.newrelic.agent.SegmentTest",
                "makeTransaction", "()V"), null, DefaultTracer.NULL_METRIC_NAME_FORMATTER);
        txa.tracerStarted(root);
        tx.setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH, true, "FOO",
                "BAR", "BAZ");
        return root;
    }

    private static int getNumTracers(Transaction tx) {
        TransactionCounts counts = tx.getTransactionCounts();
        TransactionData data = new TransactionData(tx, counts.getTransactionSize());
        // TransactionData does not include the root tracer in the tracer list.
        // Not sure why, but it seems deliberate.
        // If someone changes the behavior of TransactionData to include the root tracer,
        // this method can fixed by remove the +1.
        return data.getTracers().size() + 1;
    }

    /**
     * tracer(dispatcher) start
     * --segment start
     * --segment finish
     * tracer finish
     * <p>
     * or
     * <p>
     * tracer(dispatcher) start
     * --segment start
     * tracer finish
     * --segment finish
     */
    @Test
    public void testEndSameThread() throws InterruptedException {
        Tracer root = makeTransaction();
        Assert.assertNotNull(root);
        Assert.assertNotNull(root.getTransactionActivity().getTransaction());
        Thread.sleep(1);
        Segment segment = root.getTransactionActivity()
                .getTransaction()
                .startSegment(MetricNames.CUSTOM, "Custom Sync Segment");
        Assert.assertNotNull(segment);
        Thread.sleep(1);

        // Need to check this before the segment ends
        Assert.assertSame("Segment must be child of root tracer", root,
                segment.getTracedMethod().getParentTracedMethod());

        segment.end();
        root.finish(Opcodes.ARETURN, null);

        assertTrue(root.getTransactionActivity().getTransaction().isFinished());
        assertEquals(2,
                root.getTransactionActivity().getTransaction().getCountOfRunningAndFinishedTransactionActivities());
        assertEquals(2, getNumTracers(root.getTransactionActivity().getTransaction()));
    }

    /**
     * tracer(dispatcher) start --tracedActivity start --tracedActivity finish (On a different thread)
     * tracer finish
     * <p>
     * Transaction must have two txas due to the Segment being finished on another thread.
     * Segment tracer must correctly link to root tracer.
     */
    @Test
    public void testAsync() throws InterruptedException {
        final Tracer root = makeTransaction();
        Assert.assertNotNull(root);
        Assert.assertNotNull(root.getTransactionActivity().getTransaction());
        Thread.sleep(1);
        final Segment segment = root.getTransactionActivity()
                .getTransaction()
                .startSegment(MetricNames.CUSTOM, "Custom Async Segment");
        Assert.assertNotNull(segment);
        Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // Need to check this before the segment ends
                Assert.assertSame("Segment must be child of root tracer", root,
                        segment.getTracedMethod().getParentTracedMethod());

                segment.end();
            }
        };
        t.start();
        t.join();

        root.finish(Opcodes.ARETURN, null);

        ServiceFactory.getTransactionService().processQueue();

        // assert num children == 2
        assertTrue(root.getTransactionActivity().getTransaction().isFinished());
        assertEquals(2,
                root.getTransactionActivity().getTransaction().getCountOfRunningAndFinishedTransactionActivities());
        assertEquals(2, getNumTracers(root.getTransactionActivity().getTransaction()));
    }

    /**
     * tracer(dispatcher) start
     * --tracedActivity start
     * --tracedActivity finish (On a different thread)
     * tracer finish
     * <p>
     * Transaction must have two txas due to the Segment being finished on another thread.
     * Segment tracer must correctly link to root tracer.
     */
    @Test
    public void testAsyncWithTimeout() throws InterruptedException {
        inlineExpirationService.setTimeout(0);
        try {
            final Tracer root = makeTransaction();
            Assert.assertNotNull(root);
            Assert.assertNotNull(root.getTransactionActivity().getTransaction());
            Thread.sleep(1);
            final com.newrelic.agent.bridge.TracedActivity tracedActivity = AgentBridge.getAgent()
                    .getTransaction()
                    .createAndStartTracedActivity();
            Assert.assertNotNull(tracedActivity);

            Assert.assertSame("Segment must be child of root tracer", root,
                    tracedActivity.getTracedMethod().getParentTracedMethod());

            // Don't start the thread. The timeout is configured to 3 seconds.
            // Allow it to expire and then run the code that implements it.
            TransactionService transactionService = ServiceFactory.getTransactionService();
            for (int i = 0; i < 30 && transactionService.getExpiredTransactionCount() <= 0; i++) {
                ServiceFactory.getTransactionService().processQueue();
                Thread.sleep(500); // Wait for up to ~15 seconds or until an expired transaction appears
            }

            root.finish(Opcodes.ARETURN, null);

            assertTrue(root.getTransactionActivity().getTransaction().isFinished());
            assertEquals(2,
                    root.getTransactionActivity().getTransaction().getCountOfRunningAndFinishedTransactionActivities());
            assertEquals(2, getNumTracers(root.getTransactionActivity().getTransaction()));
        } finally {
            inlineExpirationService.clearTimeout();
        }
    }

    @Test(timeout = 30000)
    public void testAsyncSegmentWithTimeout() throws InterruptedException {
        final Tracer root = makeTransaction();
        Assert.assertNotNull(root);
        Assert.assertNotNull(root.getTransactionActivity().getTransaction());
        Thread.sleep(1);
        final Segment segment = root.getTransactionActivity().getTransaction().startSegment("Category", "segmentName");

        Assert.assertNotNull(segment);

        Tracer segmentTracer = segment.getTracer();
        Assert.assertSame("Segment must be child of root tracer", root,
                segment.getTracedMethod().getParentTracedMethod());

        // Don't start the thread. The timeout is configured to 3 seconds.
        // Allow it to expire and then run the code that implements it.

        Thread.sleep(5000);
        ServiceFactory.getTransactionService().processQueue();

        root.finish(Opcodes.ARETURN, null);

        Thread.sleep(2000);

        assertTrue(root.getTransactionActivity().getTransaction().isFinished());
        assertEquals(2, getNumTracers(root.getTransactionActivity().getTransaction()));

        // Metric name must not be prefixed with "Truncated", only the segment name
        assertEquals("Category/segmentName", segmentTracer.getMetricName());
        assertEquals("Truncated/Category/segmentName", segmentTracer.getTransactionSegmentName());

        assertEquals(TimeoutCause.SEGMENT, root.getTransactionActivity().getTransaction().getTimeoutCause());
    }

    /**
     * tracer(dispatcher) start
     * --tracedActivity start
     * (harvest)
     * --tracedActivity finish
     * tracer finish
     * <p>
     * Transaction should not timeout the segment
     */
    @Test
    public void testAsyncWithoutTimeout() throws InterruptedException {
        final long configTimeoutMillis =
                ServiceFactory.getConfigService().getDefaultAgentConfig().getValue("traced_activity_timeout", 10 * 60) *
                        1000;
        final Tracer root = makeTransaction();
        Assert.assertNotNull(root);
        Assert.assertNotNull(root.getTransactionActivity().getTransaction());
        Thread.sleep(1);
        final long startTs = System.currentTimeMillis();
        final Segment segment = root.getTransactionActivity()
                .getTransaction()
                .startSegment(MetricNames.CUSTOM, "Custom Async Without Timeout");
        Assert.assertNotNull(segment);

        ServiceFactory.getTransactionService().processQueue();

        final long durationMs = System.currentTimeMillis() - startTs;
        root.finish(Opcodes.ARETURN, null);

        // this will almost always be true since the configTimeout is 3 seconds.
        if (durationMs < configTimeoutMillis) {
            // the TA was running less than the timeout when the harvest completed. Should not have been timed out.
            assertEquals(2,
                    root.getTransactionActivity().getTransaction().getCountOfRunningAndFinishedTransactionActivities());
        } else {
            // the TA was running more than the timeout when the harvest completed. Could have been timed out correctly.
            System.err.println(
                    "Skipping timeout assert. duration " + durationMs + " exceeds timeout " + configTimeoutMillis);
        }
        Assert.assertSame("Segment must be child of root tracer", root,
                segment.getTracedMethod().getParentTracedMethod());
        segment.end(); // now let's finish
        assertTrue(root.getTransactionActivity().getTransaction().isFinished());
        assertEquals(2, getNumTracers(root.getTransactionActivity().getTransaction()));
    }

    /**
     * tracer(dispatcher) start
     * --tracedActivity start
     * --tracedActivity finish (On a different thread)
     * tracer finish
     * <p>
     * Transaction must have two txas due to the Segment being finished on another thread.
     * Segment tracer must correctly link to root tracer.
     */
    @Test
    public void testAsyncNamedTracedActivity() throws InterruptedException {
        final Tracer root = makeTransaction();
        Assert.assertNotNull(root);
        Assert.assertNotNull(root.getTransactionActivity().getTransaction());
        Thread.sleep(1);

        final TracedActivity activity = AgentBridge.getAgent().getTransaction().createAndStartTracedActivity();
        Assert.assertNotNull(activity);
        final AtomicReference<TracedMethod> tracedMethod = new AtomicReference<>();
        Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                tracedMethod.set(activity.getTracedMethod());
                activity.end();
            }
        };
        t.start();
        t.join();
        root.finish(Opcodes.ARETURN, null);

        // assert num children == 2
        assertTrue(root.getTransactionActivity().getTransaction().isFinished());
        assertTrue(tracedMethod.get() instanceof Tracer);
        assertEquals("Transaction activity context was not properly set",
                "activity",
                ((Tracer) tracedMethod.get()).getTransactionActivity().getAsyncContext());
        assertEquals("Segment name not set properly",
                "Custom/Unnamed Segment",
                ((Tracer) tracedMethod.get()).getMetricName());
        assertEquals(2,
                root.getTransactionActivity().getTransaction().getCountOfRunningAndFinishedTransactionActivities());
        assertEquals(2, getNumTracers(root.getTransactionActivity().getTransaction()));
        Assert.assertSame("Segment must be child of root tracer", root,
                tracedMethod.get().getParentTracedMethod());
    }

    /**
     * tracer(dispatcher) start
     * --tracedActivity start
     * tracer finish
     * --tracedActivity finish
     * <p>
     * Transaction must have two txas due to the tracedActivity being finished after its parent tracer is finished.
     * Segment tracer must correctly link to root tracer.
     */
    @Test
    public void testPsuedoAsync1() throws InterruptedException {
        Tracer root = makeTransaction();
        Assert.assertNotNull(root);
        Assert.assertNotNull(root.getTransactionActivity().getTransaction());
        Thread.sleep(1);

        final Segment segment = root.getTransactionActivity()
                .getTransaction()
                .startSegment(MetricNames.CUSTOM, "Custom Psuedo Async 1");
        Assert.assertNotNull(segment);
        root.finish(Opcodes.ARETURN, null);
        Thread.sleep(1);

        // Need to check this before the segment ends
        Assert.assertSame("Segment must be child of root tracer", root,
                segment.getTracedMethod().getParentTracedMethod());

        segment.end();

        assertTrue(root.getTransactionActivity().getTransaction().isFinished());
        assertEquals(2,
                root.getTransactionActivity().getTransaction().getCountOfRunningAndFinishedTransactionActivities());
        assertEquals(2, getNumTracers(root.getTransactionActivity().getTransaction()));
    }

    /**
     * tracer(dispatcher) start
     * --tracedActivity start
     * tracer finish
     * --tracedActivity finish
     * <p>
     * Transaction must have two txas due to the tracedActivity being finished after its parent tracer is finished.
     * Segment tracer must correctly link to root tracer.
     */
    @Test
    public void testSyncNamedTracedActivity() throws InterruptedException {
        Tracer root = makeTransaction();
        Assert.assertNotNull(root);
        Assert.assertNotNull(root.getTransactionActivity().getTransaction());
        Thread.sleep(1);
        final Segment segment = root.getTransactionActivity()
                .getTransaction()
                .startSegment(MetricNames.CUSTOM, "Custom Segment");
        Assert.assertNotNull(segment);
        root.finish(Opcodes.ARETURN, null);
        Thread.sleep(1);
        final AtomicReference<TracedMethod> tracedMethod = new AtomicReference<>();
        tracedMethod.set(segment.getTracedMethod());
        segment.end();

        assertTrue(root.getTransactionActivity().getTransaction().isFinished());
        assertTrue(tracedMethod.get() instanceof Tracer);
        assertEquals("Segment name was not properly set",
                "activity",
                ((Tracer) tracedMethod.get()).getTransactionActivity().getAsyncContext());
        assertEquals(2,
                root.getTransactionActivity().getTransaction().getCountOfRunningAndFinishedTransactionActivities());
        assertEquals(2, getNumTracers(root.getTransactionActivity().getTransaction()));
        Assert.assertSame("Segment must be child of root tracer", root,
                tracedMethod.get().getParentTracedMethod());
    }

    /**
     * tracer(dispatcher) start
     * --tracedActivity start
     * --tracer2 start
     * --tracer2 end
     * --tracedActivity finish
     * tracer finish
     * <p>
     * Transaction must have two txas due to the creation of tracer2 between the tracedActivity start and finish
     * Segment tracer must correctly link to root tracer.
     */
    @Test
    public void testPsuedoAsync2() throws InterruptedException {
        Tracer root = makeTransaction();
        Assert.assertNotNull(root);
        Assert.assertNotNull(root.getTransactionActivity().getTransaction());
        Thread.sleep(1);
        final Segment segment = root.getTransactionActivity()
                .getTransaction()
                .startSegment(MetricNames.CUSTOM, "Custom Psuedo Async2");
        Assert.assertNotNull(segment);
        ExitTracer child = AgentBridge.instrumentation.createTracer(null, 0, "iamyourchild",
                DefaultTracer.DEFAULT_TRACER_FLAGS);
        child.finish(Opcodes.ARETURN, null);
        assertTrue(child.getParentTracedMethod() == root);
        Thread.sleep(1);

        // Need to check this before the segment ends
        Assert.assertSame("Segment must be child of root tracer", root,
                segment.getTracedMethod().getParentTracedMethod());

        segment.end();
        root.finish(Opcodes.ARETURN, null);

        assertTrue(root.getTransactionActivity().getTransaction().isFinished());
        assertEquals(2,
                root.getTransactionActivity().getTransaction().getCountOfRunningAndFinishedTransactionActivities());
        assertEquals(3, getNumTracers(root.getTransactionActivity().getTransaction()));
    }

    /**
     * Unfinished tracedActivities should not allow the transaction to finish.
     */
    @Test
    public void testTracedActivityHoldsTransactionOpen() throws InterruptedException {
        Tracer root = makeTransaction();
        Assert.assertNotNull(root);
        Assert.assertNotNull(root.getTransactionActivity().getTransaction());
        Thread.sleep(1);
        final Segment segment = root.getTransactionActivity()
                .getTransaction()
                .startSegment(MetricNames.CUSTOM, Segment.UNNAMED_SEGMENT);
        Assert.assertNotNull(segment);
        Thread.sleep(1);
        final Segment segment2 = root.getTransactionActivity()
                .getTransaction()
                .startSegment(MetricNames.CUSTOM, Segment.UNNAMED_SEGMENT);
        root.finish(Opcodes.ARETURN, null);
        Thread.sleep(1);
        assertFalse(root.getTransactionActivity().getTransaction().isFinished());
        Thread.sleep(1);

        // Need to check this before the segment ends
        Assert.assertSame("Segment must be child of root tracer", root,
                segment.getTracedMethod().getParentTracedMethod());
        Assert.assertSame("Segment2 must be child of root tracer", root,
                segment2.getTracedMethod().getParentTracedMethod());

        segment.end();
        assertFalse(root.getTransactionActivity().getTransaction().isFinished());
        Thread.sleep(1);
        segment2.end();
        Thread.sleep(1);

        assertTrue(root.getTransactionActivity().getTransaction().isFinished());
        assertEquals(3,
                root.getTransactionActivity().getTransaction().getCountOfRunningAndFinishedTransactionActivities());

        assertEquals(3, getNumTracers(root.getTransactionActivity().getTransaction()));
    }

    /**
     * Segments have their own txa since they're all ended async.
     */
    @Test
    public void testCorrectParenting() throws InterruptedException {
        Tracer root = makeTransaction();
        Assert.assertNotNull(root);
        Assert.assertNotNull(root.getTransactionActivity().getTransaction());
        {
            final Segment segmentUnderRoot = root.getTransactionActivity()
                    .getTransaction()
                    .startSegment(MetricNames.CUSTOM, "Under Root");
            Assert.assertNotNull(segmentUnderRoot);
            Assert.assertSame("Segment has the wrong parent", root,
                    segmentUnderRoot.getTracedMethod().getParentTracedMethod());
            segmentUnderRoot.end();
        }
        {
            ExitTracer child1 = AgentBridge.instrumentation.createTracer(null, 0, "iamyourchild1",
                    DefaultTracer.DEFAULT_TRACER_FLAGS);
            {
                final Segment underChild1 = root.getTransactionActivity()
                        .getTransaction()
                        .startSegment(MetricNames.CUSTOM, "Under Child");
                Assert.assertNotNull(underChild1);
                Assert.assertSame("Segment has the wrong parent", child1,
                        underChild1.getTracedMethod().getParentTracedMethod());
                underChild1.end();
            }
            {
                ExitTracer child2 = AgentBridge.instrumentation.createTracer(null, 0, "iamyourchild2",
                        DefaultTracer.DEFAULT_TRACER_FLAGS);
                {
                    final Segment underChild2 = root.getTransactionActivity()
                            .getTransaction()
                            .startSegment(MetricNames.CUSTOM, "Under Child 2");
                    Assert.assertNotNull(underChild2);
                    Assert.assertSame("Segment has the wrong parent", child2,
                            underChild2.getTracedMethod().getParentTracedMethod());
                    underChild2.end();
                }
                child2.finish(Opcodes.ARETURN, null);
            }
            child1.finish(Opcodes.ARETURN, null);
        }
        assertFalse(root.getTransactionActivity().getTransaction().isFinished());
        root.finish(Opcodes.ARETURN, null);

        assertTrue(root.getTransactionActivity().getTransaction().isFinished());
        // 4 txas: 1 for each of the 3 segments + 1 tracer
        assertEquals(4,
                root.getTransactionActivity().getTransaction().getCountOfRunningAndFinishedTransactionActivities());
        assertEquals(6, getNumTracers(root.getTransactionActivity().getTransaction()));
    }

    @Test
    public void testMetricMigration() throws InterruptedException {
        Tracer root = makeTransaction();
        Assert.assertNotNull(root);
        Assert.assertNotNull(root.getTransactionActivity().getTransaction());
        final Segment segment = root.getTransactionActivity()
                .getTransaction()
                .startSegment(MetricNames.CUSTOM, "Custom Segment");
        segment.getTracedMethod().addRollupMetricName("rollupMetric");
        segment.getTracedMethod().addExclusiveRollupMetricName("exclusiveRollupMetric");
        segment.end();
        root.finish(Opcodes.ARETURN, null);
        assertTrue(root.getTransactionActivity().getTransaction().isFinished());

        ResponseTimeStats rollupMetric = root.getTransactionActivity()
                .getTransactionStats()
                .getUnscopedStats()
                .getOrCreateResponseTimeStats(
                        "rollupMetric");
        assertTrue(rollupMetric.getCallCount() == 1);

        ResponseTimeStats exclusiveRollupMetric = root.getTransactionActivity()
                .getTransactionStats()
                .getUnscopedStats()
                .getOrCreateResponseTimeStats(
                        "exclusiveRollupMetric");
        assertTrue(exclusiveRollupMetric.getCallCount() == 1);
    }

    @Test
    public void testMetricMigrationAsync() throws InterruptedException {
        Tracer root = makeTransaction();
        Assert.assertNotNull(root);
        Assert.assertNotNull(root.getTransactionActivity().getTransaction());
        final Segment segment = root.getTransactionActivity()
                .getTransaction()
                .startSegment(MetricNames.CUSTOM, "Custom Segment");
        segment.getTracedMethod().addRollupMetricName("rollupMetric");

        Thread finishThread = new Thread(new Runnable() {

            @Override
            public void run() {
                segment.getTracedMethod().addRollupMetricName("rollupMetric2");
                segment.end();

            }
        });

        finishThread.start();
        finishThread.join();

        root.finish(Opcodes.ARETURN, null);
        assertTrue(root.getTransactionActivity().getTransaction().isFinished());

        ResponseTimeStats rollupMetric = root.getTransactionActivity()
                .getTransactionStats()
                .getUnscopedStats()
                .getOrCreateResponseTimeStats(
                        "rollupMetric");
        assertEquals(1, rollupMetric.getCallCount());

        ResponseTimeStats exclusiveRollupMetric = root.getTransactionActivity()
                .getTransactionStats()
                .getUnscopedStats()
                .getOrCreateResponseTimeStats(
                        "rollupMetric2");
        assertEquals(1, exclusiveRollupMetric.getCallCount());
    }

    @Test
    public void testExclusiveTime() throws InterruptedException {
        DefaultTracer root = (DefaultTracer) makeTransaction();
        Assert.assertNotNull(root);
        Assert.assertNotNull(root.getTransactionActivity().getTransaction());

        final com.newrelic.agent.bridge.TracedActivity tracedActivity = AgentBridge.getAgent()
                .getTransaction()
                .createAndStartTracedActivity();
        DefaultTracer underlyingTracer = (DefaultTracer) tracedActivity.getTracedMethod();
        Thread.sleep(10);
        tracedActivity.end();

        // Assuming the exclusive duration of the tracedActivity tracer is correct, verify the parent's exclusive
        // duration is calculated correctly
        root.performFinishWork(root.getStartTime() + 100, Opcodes.ARETURN, null);
        assertTrue(root.getTransactionActivity().getTransaction().isFinished());
        assertEquals(100, root.getExclusiveDuration());

        assertTrue(underlyingTracer.getExclusiveDuration() >= TimeUnit.MILLISECONDS.toNanos(10));
    }

    @Test
    public void testExclusiveTimeAsync() throws InterruptedException {
        DefaultTracer root = (DefaultTracer) makeTransaction();
        Assert.assertNotNull(root);
        Assert.assertNotNull(root.getTransactionActivity().getTransaction());

        final Segment segment = root.getTransactionActivity()
                .getTransaction()
                .startSegment(MetricNames.CUSTOM, "Custom Segment");
        Thread finishThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                segment.end();
            }
        });

        finishThread.start();

        root.performFinishWork(root.getStartTime() + 100, Opcodes.ARETURN, null);
        finishThread.join();

        // exclusive duration of async tracedActivity tracer should not affect root tracer's exclusive duration
        assertTrue(root.getTransactionActivity().getTransaction().isFinished());
        assertEquals(100, root.getExclusiveDuration());
    }

    /**
     * Do not allow tracedActivity creation for ignored transactions.
     */
    @Test
    public void testIgnoreTransaction() throws InterruptedException {
        Tracer root = makeTransaction();
        Assert.assertNotNull(root);
        Assert.assertNotNull(root.getTransactionActivity().getTransaction());
        root.getTransactionActivity().getTransaction().ignore();
        Thread.sleep(1);

        final Segment segment = root.getTransactionActivity()
                .getTransaction()
                .startSegment(MetricNames.CUSTOM, "Custom Segment");
        assertNull(segment);
        Thread.sleep(1);
        root.finish(Opcodes.ARETURN, null);

        assertTrue(root.getTransactionActivity().getTransaction().isFinished());
        //I think reporting a 0 instead of a 1 is fine here
        assertEquals(0,
                root.getTransactionActivity().getTransaction().getCountOfRunningAndFinishedTransactionActivities());
        assertEquals(1, getNumTracers(root.getTransactionActivity().getTransaction()));
    }

    /**
     * Ignored tracedActivities should not be included in the transaction.
     */
    @Test
    public void testIgnore() throws InterruptedException {
        Tracer root = makeTransaction();
        Assert.assertNotNull(root);
        Assert.assertNotNull(root.getTransactionActivity().getTransaction());
        Thread.sleep(1);
        final Segment segment = root.getTransactionActivity()
                .getTransaction()
                .startSegment(MetricNames.CUSTOM, "Custom Segment");
        Assert.assertNotNull(segment);
        Thread.sleep(1);
        segment.ignoreIfUnfinished();
        root.finish(Opcodes.ARETURN, null);

        assertTrue(root.getTransactionActivity().getTransaction().isFinished());
        assertEquals(1,
                root.getTransactionActivity().getTransaction().getCountOfRunningAndFinishedTransactionActivities());
        assertEquals(1, getNumTracers(root.getTransactionActivity().getTransaction()));
    }

    /**
     * Ignored async segment should not be included in the transaction and not prevent the transaction from
     * finishing.
     */
    @Test
    public void testIgnoreAsync() throws InterruptedException {
        final Tracer root = makeTransaction();
        Assert.assertNotNull(root);
        Assert.assertNotNull(root.getTransactionActivity().getTransaction());
        Thread.sleep(1);
        final Segment segment = root.getTransactionActivity()
                .getTransaction()
                .startSegment(MetricNames.CUSTOM, "Custom Segment");
        root.finish(Opcodes.ARETURN, null);
        Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(5);
                    assertFalse(root.getTransactionActivity().getTransaction().isFinished());
                    segment.ignoreIfUnfinished();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                segment.end();
            }
        };
        t.start();
        t.join();

        assertTrue(root.getTransactionActivity().getTransaction().isFinished());
        assertEquals(1,
                root.getTransactionActivity().getTransaction().getCountOfRunningAndFinishedTransactionActivities());
        assertEquals(1, getNumTracers(root.getTransactionActivity().getTransaction()));
    }

    @Test
    public void testAsyncContextAttributeEndSameThread() {
        Tracer root = makeTransaction();
        Assert.assertNotNull(root);
        Assert.assertNotNull(root.getTransactionActivity().getTransaction());

        Segment segment = root.getTransactionActivity()
                .getTransaction()
                .startSegment(MetricNames.CUSTOM, "Custom Segment");
        Assert.assertNotNull(segment);

        segment.end();
        root.finish(Opcodes.ARETURN, null);

        assertTrue(root.getTransactionActivity().getTransaction().isFinished());

        List<Tracer> tracers = root.getTransactionActivity().getTracers();
        // Segment shouldn't be a child of root, it should have its own txa
        assertEquals(0, tracers.size());

        TransactionActivity segmentTxa = findSegmentTxa(root.getTransactionActivity().getTransaction());
        assertNull(segmentTxa.getRootTracer().getAgentAttribute("async_context"));
    }

    @Test
    public void testDefaultTracedActivityName() {

        Tracer root = makeTransaction();
        Assert.assertNotNull(root);
        Assert.assertNotNull(root.getTransactionActivity().getTransaction());

        Segment segment = root.getTransactionActivity().getTransaction().startSegment(MetricNames.CUSTOM, "My Segment");
        Assert.assertNotNull(segment);

        segment.end();
        root.finish(Opcodes.ARETURN, null);

        assertTrue(root.getTransactionActivity().getTransaction().isFinished());

        // Find txa with Segment
        TransactionActivity segmentTxa = findSegmentTxa(root.getTransactionActivity().getTransaction());
        Assert.assertNotNull(segmentTxa);

        assertEquals("Custom/My Segment", segmentTxa.getRootTracer().getTransactionSegmentName());
    }

    // Only the tests that validate the timeout algo require the config manager,
    // but we just throw it in the setup() method for all the tests.

    private static Map<String, Object> createConfigMap() {
        Map<String, Object> map = new HashMap<>();
        map.put(AgentConfigImpl.APP_NAME, APP_NAME);
        map.put("traced_activity_timeout", 3);
        map.put("token_timeout", 2);
        map.put(AgentConfigImpl.THREAD_CPU_TIME_ENABLED, Boolean.TRUE);
        Map<String, Object> ttMap = new HashMap<>();
        ttMap.put(TransactionTracerConfigImpl.GC_TIME_ENABLED, Boolean.TRUE);
        ttMap.put(TransactionTracerConfigImpl.TRANSACTION_THRESHOLD, 0.0f);
        map.put(AgentConfigImpl.TRANSACTION_TRACER, ttMap);

        Map<String, Object> dtMap = new HashMap<>();
        dtMap.put("enabled", true);
        map.put("distributed_tracing", dtMap);
        Map<String, Object> spanConfig = new HashMap<>();
        spanConfig.put("collect_span_events", true);
        map.put("span_events", spanConfig);

        map.put("trusted_account_key", "12abc345");
        map.put("account_id", "12abc345");

        return map;
    }

    private static void createServiceManager(Map<String, Object> map, ExpirationService expirationService)
            throws Exception {
        ConfigService configService = ConfigServiceFactory.createConfigServiceUsingSettings(map);
        MockServiceManager serviceManager = new MockServiceManager(configService);
        ServiceFactory.setServiceManager(serviceManager);

        ThreadService threadService = new ThreadService();
        serviceManager.setThreadService(threadService);

        serviceManager.setConfigService(configService);

        StatsService statsService = new StatsServiceImpl();
        serviceManager.setStatsService(statsService);

        MockCoreService agent = new MockCoreService();
        serviceManager.setCoreService(agent);

        HarvestService harvestService = new MockHarvestService();
        serviceManager.setHarvestService(harvestService);

        AsyncTransactionService asyncTxService = new AsyncTransactionService();
        serviceManager.setAsyncTransactionService(asyncTxService);

        TransactionService transactionService = new TransactionService(2, 1, 3, TimeUnit.SECONDS);
        serviceManager.setTransactionService(transactionService);

        EnvironmentService envService = new EnvironmentServiceImpl();
        serviceManager.setEnvironmentService(envService);

        TransactionTraceService transactionTraceService = new TransactionTraceService();
        serviceManager.setTransactionTraceService(transactionTraceService);

        SqlTraceService sqlTraceService = new SqlTraceServiceImpl();
        serviceManager.setSqlTraceService(sqlTraceService);

        serviceManager.setAttributesService(new AttributesService());
        DistributedTraceServiceImpl distributedTraceService = new DistributedTraceServiceImpl();

        Map<String, Object> configMap = ImmutableMap.<String, Object>builder().put("cross_application_tracer",
                ImmutableMap.builder().put("account_id", "12abc345").put("trusted_account_key", "12abc345").build())
                .build();
        distributedTraceService.connected(null, AgentConfigFactory.createAgentConfig(configMap, null, null));

        serviceManager.setDistributedTraceService(distributedTraceService);
        TransactionDataToDistributedTraceIntrinsics transactionDataToDistributedTraceIntrinsics = new TransactionDataToDistributedTraceIntrinsics(
                distributedTraceService);
        serviceManager.setTransactionEventsService(
                new TransactionEventsService(transactionDataToDistributedTraceIntrinsics));

        MockRPMServiceManager rpmServiceManager = new MockRPMServiceManager();
        serviceManager.setRPMServiceManager(rpmServiceManager);

        serviceManager.setExpirationService(expirationService);
        SpanEventsService spanEventsService = SpanEventsServiceFactory.builder()
                .configService(configService)
                .reservoirManager(new MockSpanEventReservoirManager(configService))
                .transactionService(serviceManager.getTransactionService())
                .rpmServiceManager(serviceManager.getRPMServiceManager())
                .spanEventCreationDecider(new SpanEventCreationDecider(configService))
                .environmentService(envService)
                .transactionDataToDistributedTraceIntrinsics(transactionDataToDistributedTraceIntrinsics)
                .build();

        serviceManager.setSpansEventService(spanEventsService);
        serviceManager.start();
    }

    @Override
    public void dispatcherTransactionStarted(Transaction transaction) {
        if (runningTransactions == null) {
            runningTransactions = Queues.newConcurrentLinkedQueue();
        }

        runningTransactions.add(transaction);
    }

    @Override
    public void dispatcherTransactionFinished(TransactionData transactionData, TransactionStats transactionStats) {
        runningTransactions.remove(transactionData.getTransaction());
    }

    @Override
    public void dispatcherTransactionCancelled(Transaction transaction) {
    }

    @Test
    public void confirmTxnGc() {
        // Ensure that holding a Segment does not prevent transactions from being collected.

        Tracer rootTracer = makeTransaction();
        Transaction txn = rootTracer.getTransactionActivity().getTransaction();

        // Sanity check: we have a txn
        Assert.assertNotNull(rootTracer);
        assertEquals("Thread local txn has a different value", txn, Transaction.getTransaction(false));

        Segment seg = txn.startSegment(MetricNames.CUSTOM, "My Little Segment");
        Assert.assertNotNull(seg);

        rootTracer.finish(Opcodes.ARETURN, null);
        seg.end();

        assertTrue(txn.isFinished());

        // let it go, let it go,
        // Can't hold it back anymore
        // - Queen Elsa
        txn = null;
        rootTracer = null;

        // If this test flickers, consider filing a JIRA to make this a soak test

        // Please, please, run the gc
        System.gc();

        assertNull(seg.getParent());

        // Need to find if the underlying txn is still alive.
        com.newrelic.api.agent.Transaction tx = seg.getTransaction();
        assertNull("Transaction was not garbage collected", tx);
    }

    @Test
    public void testSegmentTracerAttributes() throws InterruptedException {
        // Assert that an async Segment has three attributes:
        // 1) start_thread: name of thread that started the segment
        // 2) end_thread: name of thread tht ended the segment
        // 3) async_context: "segment-api"

        Tracer root = makeTransaction();
        Assert.assertNotNull(root);
        Assert.assertNotNull(root.getTransactionActivity().getTransaction());

        final Transaction txn = root.getTransactionActivity().getTransaction();
        final Segment segment = txn.startSegment(MetricNames.CUSTOM, "Segment Name");

        final AtomicReference<Tracer> segmentTracerRef = new AtomicReference<>();
        Thread finishThread = new Thread(new Runnable() {

            @Override
            public void run() {
                Thread.currentThread().setName("Second Thread");
                segmentTracerRef.set(segment.getTracer());
                segment.end();
            }
        });

        finishThread.start();
        finishThread.join();

        root.finish(Opcodes.ARETURN, null);
        assertTrue(root.getTransactionActivity().getTransaction().isFinished());

        Tracer segmentTracer = segmentTracerRef.get();

        assertEquals("Initiating thread does not match",
                Thread.currentThread().getName(),
                segmentTracer.getAgentAttribute(Segment.START_THREAD));

        assertEquals("Ending thread does not match",
                "Second Thread",
                segmentTracer.getAgentAttribute(Segment.END_THREAD));

        assertEquals("segment-api", segmentTracer.getAgentAttribute("async_context"));
    }

    @Test
    public void testSameThreadSegmentTracerAttributes() throws InterruptedException {
        // Assert that Segment ended on same thread do not have these three attributes:
        // start_thread, end_thread, async_context

        Tracer root = makeTransaction();
        Assert.assertNotNull(root);
        Assert.assertNotNull(root.getTransactionActivity().getTransaction());

        final Transaction txn = root.getTransactionActivity().getTransaction();
        final Segment segment = txn.startSegment(MetricNames.CUSTOM, "Segment Name");
        Tracer segmentTracer = segment.getTracer();
        segment.end();

        root.finish(Opcodes.ARETURN, null);
        assertTrue(root.getTransactionActivity().getTransaction().isFinished());

        assertNull(segmentTracer.getAgentAttribute(Segment.START_THREAD));
        assertNull(segmentTracer.getAgentAttribute(Segment.END_THREAD));
        assertNull(segmentTracer.getAgentAttribute("async_context"));
    }

    @Test(timeout = 30000)
    public void testTxnAttrSegmentTimeout() throws InterruptedException {
        inlineExpirationService.setTimeout(0);
        try {
            Transaction.clearTransaction();
            Tracer rootTracer = makeTransaction();
            Transaction tx = rootTracer.getTransactionActivity().getTransaction();

            // Name the transaction so we can identify it in the listener below and create an event
            tx.setTransactionName(TransactionNamePriority.CUSTOM_HIGH, true, "SegmentTimeout",
                    "timeout");

            final List<TransactionEvent> events = new ArrayList<>();
            final CountDownLatch latch = new CountDownLatch(1);

            ServiceFactory.getServiceManager().getTransactionService()
                    .addTransactionListener(new TransactionListener() {
                        @Override
                        public void dispatcherTransactionFinished(TransactionData transactionData,
                                                                  TransactionStats transactionStats) {
                            if (transactionData.getPriorityTransactionName()
                                    .getName()
                                    .equals("OtherTransaction/SegmentTimeout/timeout")) {
                                events.add(ServiceFactory.getTransactionEventsService().createEvent(transactionData,
                                        transactionStats, transactionData.getBlameOrRootMetricName()));
                                latch.countDown();
                            }
                        }
                    });

            tx.getTransactionActivity().tracerStarted(rootTracer);

            // Let this timeout the transaction
            com.newrelic.api.agent.Segment segment = TransactionApiImpl.INSTANCE.startSegment(null);

            rootTracer.finish(Opcodes.RETURN, 0);
            assertFalse(tx.isFinished());

            // Don't start the thread. The timeout is configured to 3 seconds.
            // Allow it to expire and then run the code that implements it.

            Thread.sleep(5000);
            latch.await();

            assertTrue(tx.isFinished());
            assertFalse(events.isEmpty());
            assertEquals("OtherTransaction/SegmentTimeout/timeout", events.get(0).getName());
            assertEquals(TimeoutCause.SEGMENT, events.get(0).getTimeoutCause());
        } finally {
            inlineExpirationService.clearTimeout();
        }
    }

    @Test
    public void testSpanParenting() {
        SpanEventsService spanEventService = ServiceFactory.getSpanEventService();
        SamplingPriorityQueue<SpanEvent> eventPool = spanEventService.getOrCreateDistributedSamplingReservoir(APP_NAME);

        Transaction.clearTransaction();
        Tracer rootTracer = makeTransaction();
        rootTracer.getTransactionActivity().getTransaction().setPriorityIfNotNull(1.0f);
        rootTracer.setMetricName("RootTracer");
        Transaction tx = rootTracer.getTransactionActivity().getTransaction();
        Segment segment = tx.startSegment("custom", "segment");

        final HashMap<String, String> headers = new HashMap<>();
        segment.addOutboundRequestHeaders(new OutboundHeaders() {
            @Override
            public HeaderType getHeaderType() {
                return HeaderType.HTTP;
            }

            @Override
            public void setHeader(String name, String value) {
                headers.put(name, value);
            }
        });
        String payload = headers.get("newrelic");

        segment.end();
        rootTracer.finish(Opcodes.ARETURN, null);

        Transaction.clearTransaction();
        Tracer secondRootTracer = makeTransaction();
        secondRootTracer.setMetricName("SecondRootTracer");
        Transaction secondTxn = secondRootTracer.getTransactionActivity().getTransaction();
        secondTxn.acceptDistributedTracePayload(payload);
        secondRootTracer.finish(Opcodes.ARETURN, null);

        List<SpanEvent> spanEvents = eventPool.asList();
        Assert.assertNotNull(spanEvents);
        assertEquals(3, spanEvents.size());

        SpanEvent rootTracerSpanEvent = findSpanByName(spanEvents, "RootTracer");
        SpanEvent segmentSpanEvent = findSpanByName(spanEvents, "custom/segment");
        SpanEvent secondRootTracerSpanEvent = findSpanByName(spanEvents, "SecondRootTracer");

        assertEquals(rootTracerSpanEvent.getTraceId(), segmentSpanEvent.getTraceId());
        assertEquals(segmentSpanEvent.getTraceId(), secondRootTracerSpanEvent.getTraceId());

        assertEquals(rootTracerSpanEvent.getGuid(), segmentSpanEvent.getParentId());
        assertEquals(segmentSpanEvent.getGuid(), secondRootTracerSpanEvent.getParentId());
    }

    @Test
    public void testSegmentAddCustomAttributeSync() {
        Transaction.clearTransaction();
        Tracer rootTracer = makeTransaction();
        Transaction tx = rootTracer.getTransactionActivity().getTransaction();
        Segment segment = tx.startSegment("custom", "segment");
        Tracer tracer = segment.getTracer();

        segment.addCustomAttribute("redbeans", "rice");
        segment.addCustomAttribute("numBeans", 400);
        segment.addCustomAttribute("sausage", true);
        segment.addCustomAttribute(null, "Keys cant be null");
        Map<String, Object> extras = new HashMap<>();
        extras.put("pickles", null);
        extras.put("hotSauce", true);
        segment.addCustomAttributes(extras);
        segment.end();

        assertEquals(4, tracer.getCustomAttributes().size());
        assertEquals("rice", tracer.getCustomAttributes().get("redbeans"));
        assertEquals(400, tracer.getCustomAttributes().get("numBeans"));
        assertTrue((Boolean) tracer.getCustomAttributes().get("sausage"));
        assertTrue((Boolean) tracer.getCustomAttributes().get("hotSauce"));
    }

    @Test
    public void testSegmentAddCustomAttributeAsync() throws InterruptedException {
        Tracer root = makeTransaction();
        final Transaction txn = root.getTransactionActivity().getTransaction();
        final Segment segment = txn.startSegment("custom", "Segment Name");
        final AtomicReference<Tracer> segmentTracerRef = new AtomicReference<>();

        Thread finishThread = new Thread(new Runnable() {

            @Override
            public void run() {
                segment.addCustomAttribute("redbeans", "rice");
                segment.addCustomAttribute("numBeans", 400);
                segment.addCustomAttribute("sausage", true);
                segment.addCustomAttribute(null, "Keys cant be null");
                Map<String, Object> extras = new HashMap<>();
                extras.put("pickles", null);
                extras.put("hotSauce", true);
                segment.addCustomAttributes(extras);

                Thread.currentThread().setName("Second Thread");
                segmentTracerRef.set(segment.getTracer());
                segment.end();
            }
        });

        finishThread.start();
        finishThread.join();
        Tracer tracer = segmentTracerRef.get();
        assertEquals(4, tracer.getCustomAttributes().size());
        assertEquals("rice", tracer.getCustomAttributes().get("redbeans"));
        assertEquals(400, tracer.getCustomAttributes().get("numBeans"));
        assertTrue((Boolean) tracer.getCustomAttributes().get("sausage"));
        assertTrue((Boolean) tracer.getCustomAttributes().get("hotSauce"));
    }

    private SpanEvent findSpanByName(List<SpanEvent> spanEvents, String spanName) {
        for (SpanEvent spanEvent : spanEvents) {
            if (spanEvent.getName().equals(spanName)) {
                return spanEvent;
            }
        }

        return null;
    }

    private TransactionActivity findSegmentTxa(Transaction transaction) {
        for (TransactionActivity txa : transaction.getFinishedChildren()) {
            if (txa.getSegment() != null) {
                return txa;
            }
        }

        return null;
    }
}
