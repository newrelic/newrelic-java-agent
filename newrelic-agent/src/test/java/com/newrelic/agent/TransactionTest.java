/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.google.common.collect.ImmutableMap;
import com.newrelic.agent.attributes.AttributeNames;
import com.newrelic.agent.attributes.AttributesService;
import com.newrelic.agent.bridge.*;
import com.newrelic.agent.config.*;
import com.newrelic.agent.dispatchers.OtherDispatcher;
import com.newrelic.agent.environment.EnvironmentService;
import com.newrelic.agent.environment.EnvironmentServiceImpl;
import com.newrelic.agent.instrumentation.InstrumentationImpl;
import com.newrelic.agent.interfaces.SamplingPriorityQueue;
import com.newrelic.agent.model.AnalyticsEvent;
import com.newrelic.agent.model.PriorityAware;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.analytics.TransactionDataToDistributedTraceIntrinsics;
import com.newrelic.agent.service.analytics.TransactionEventsService;
import com.newrelic.agent.service.async.AsyncTransactionService;
import com.newrelic.agent.sql.SqlTraceService;
import com.newrelic.agent.sql.SqlTraceServiceImpl;
import com.newrelic.agent.stats.*;
import com.newrelic.agent.trace.TransactionTraceService;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.agent.tracers.OtherRootTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.metricname.SimpleMetricNameFormat;
import com.newrelic.agent.tracers.servlet.BasicRequestRootTracer;
import com.newrelic.agent.tracers.servlet.MockHttpRequest;
import com.newrelic.agent.tracers.servlet.MockHttpResponse;
import com.newrelic.agent.tracing.DistributedTracePayloadImpl;
import com.newrelic.agent.tracing.DistributedTraceService;
import com.newrelic.agent.tracing.DistributedTraceServiceImpl;
import com.newrelic.agent.tracing.DistributedTraceUtil;
import com.newrelic.agent.transaction.PriorityTransactionName;
import com.newrelic.agent.transaction.SegmentTest;
import com.newrelic.agent.util.Obfuscator;
import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.InboundHeaders;
import com.newrelic.api.agent.TransportType;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.objectweb.asm.Opcodes;

import java.util.*;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.*;

@SuppressWarnings("deprecation")
public class TransactionTest {

    private static MockServiceManager serviceManager;
    private static final ThreadService threadService = new ThreadService();

    private static Instrumentation savedInstrumentation;
    private static com.newrelic.agent.bridge.Agent savedAgent;
    private DistributedTraceService mockDistributedTraceService;

    @BeforeClass
    public static void beforeClass() throws Exception {
        savedInstrumentation = AgentBridge.instrumentation;
        savedAgent = AgentBridge.agent;
    }

    @AfterClass
    public static void afterClass() {
        AgentBridge.instrumentation = savedInstrumentation;
        AgentBridge.agent = savedAgent;
    }

    private static Map<String, Object> createConfigMap() {
        Map<String, Object> map = new HashMap<>();
        map.put(AgentConfigImpl.APP_NAME, "Unit Test");
        map.put(AgentConfigImpl.THREAD_CPU_TIME_ENABLED, Boolean.TRUE);
        Map<String, Object> ttMap = new HashMap<>();
        ttMap.put(TransactionTracerConfigImpl.GC_TIME_ENABLED, Boolean.TRUE);
        ttMap.put(TransactionTracerConfigImpl.TRANSACTION_THRESHOLD, 0.0f);
        map.put(AgentConfigImpl.TRANSACTION_TRACER, ttMap);
        Map<String, Object> crossProcessMap = new HashMap<>();
        crossProcessMap.put(CrossProcessConfigImpl.TRUSTED_ACCOUNT_IDS, "12345,9123");
        crossProcessMap.put(DistributedTracingConfig.TRUSTED_ACCOUNT_KEY, "67890");
        map.put(AgentConfigImpl.CROSS_APPLICATION_TRACER, crossProcessMap);
        return map;
    }

    private static void createServiceManager(Map<String, Object> map) throws Exception {
        serviceManager = new MockServiceManager();
        ServiceFactory.setServiceManager(serviceManager);
        serviceManager.start();

        serviceManager.setThreadService(threadService);

        Map<String, Object> serverData = new HashMap<>();
        serverData.put(CrossProcessConfigImpl.CROSS_PROCESS_ID, "12345#56789");
        serverData.put(CrossProcessConfigImpl.APPLICATION_ID, "56789");
        serverData.put(CrossProcessConfigImpl.TRUSTED_ACCOUNT_IDS, "12345,9123");
        serverData.put(CrossProcessConfigImpl.ENCODING_KEY, "anotherExampleKey");
        AgentConfig agentConfig = AgentHelper.createAgentConfig(true, map, serverData);

        ConfigService configService = ConfigServiceFactory.createConfigService(agentConfig, map);
        serviceManager.setConfigService(configService);

        StatsService statsService = new StatsServiceImpl();
        serviceManager.setStatsService(statsService);

        MockCoreService agent = new MockCoreService();
        serviceManager.setCoreService(agent);

        HarvestService harvestService = new MockHarvestService();
        serviceManager.setHarvestService(harvestService);

        TransactionService transactionService = new TransactionService();
        serviceManager.setTransactionService(transactionService);

        EnvironmentService envService = new EnvironmentServiceImpl();
        serviceManager.setEnvironmentService(envService);

        TransactionTraceService transactionTraceService = new TransactionTraceService();
        serviceManager.setTransactionTraceService(transactionTraceService);

        SqlTraceService sqlTraceService = new SqlTraceServiceImpl();
        serviceManager.setSqlTraceService(sqlTraceService);

        serviceManager.setAttributesService(new AttributesService());

        serviceManager.setAsyncTransactionService(new AsyncTransactionService());


        MockRPMServiceManager rpmServiceManager = new MockRPMServiceManager();
        serviceManager.setRPMServiceManager(rpmServiceManager);

        DistributedTraceServiceImpl distributedTraceService = new DistributedTraceServiceImpl();
        serviceManager.setDistributedTraceService(distributedTraceService);
        distributedTraceService.start();

        TransactionDataToDistributedTraceIntrinsics transactionDataToDistributedTraceIntrinsics = new TransactionDataToDistributedTraceIntrinsics(distributedTraceService);
        serviceManager.setTransactionEventsService(new TransactionEventsService(transactionDataToDistributedTraceIntrinsics));

        serviceManager.setExpirationService(new SegmentTest.InlineExpirationService());

        AgentBridge.instrumentation = new InstrumentationImpl(Agent.LOG);
        AgentBridge.agent = new AgentImpl(Agent.LOG);

        List<ConnectionListener> connectionListeners = rpmServiceManager.getConnectionListeners();
        for (ConnectionListener connectionListener : connectionListeners) {
            connectionListener.connected(rpmServiceManager.getRPMService(), agentConfig);
        }
    }

    @Before
    public void setUp() throws Exception {
        mockDistributedTraceService = new DistributedTraceService() {
            @Override
            public boolean isEnabled() {
                return true;
            }

            @Override
            public int getMajorSupportedCatVersion() {
                return 1;
            }

            @Override
            public int getMinorSupportedCatVersion() {
                return 0;
            }

            @Override
            public String getAccountId() {
                return "9123";
            }

            @Override
            public String getApplicationId() {
                return "1234";
            }

            @Override
            public <T extends PriorityAware> float calculatePriority(Float priority, SamplingPriorityQueue<T> reservoir) {
                return 1.0f;
            }

            @Override
            public Map<String, Object> getIntrinsics(DistributedTracePayloadImpl inboundPayload, String guid,
                    String traceId, TransportType transportType, long parentTransportDuration,
                    long largestTransportDuration, String parentId, String parentSpanId, float priority) {
                return null;
            }

            @Override
            public String getTrustKey() {
                return "67890";
            }

            @Override
            public DistributedTracePayload createDistributedTracePayload(Tracer tracer) {
                return null;
            }
        };
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testTracerLimit1() throws Exception {
        Map<String, Object> config = createConfigMap();
        Map<String, Object> ttmap = (Map) config.get(AgentConfigImpl.TRANSACTION_TRACER);
        ttmap.put("segment_limit", 3);
        createServiceManager(config);

        Transaction.clearTransaction();
        Transaction tx = Transaction.getTransaction();
        BasicRequestRootTracer rootTracer = (BasicRequestRootTracer) createDispatcherTracer(true);

        tx.getTransactionActivity().tracerStarted(rootTracer);
        DefaultTracer t1 = createBasicTracer("one");
        tx.getTransactionActivity().tracerStarted(t1);
        DefaultTracer t2 = createBasicTracer("two");
        tx.getTransactionActivity().tracerStarted(t2);
        DefaultTracer t3 = createBasicTracer("three");
        tx.getTransactionActivity().tracerStarted(t3);
        DefaultTracer t4 = createBasicTracer("four");
        tx.getTransactionActivity().tracerStarted(t4);
        DefaultTracer t5 = createBasicTracer("five");
        tx.getTransactionActivity().tracerStarted(t5);

        tx.getTransactionActivity().tracerFinished(t5, 0);
        tx.getTransactionActivity().tracerFinished(t4, 0);
        tx.getTransactionActivity().tracerFinished(t3, 0);
        tx.getTransactionActivity().tracerFinished(t2, 0);
        tx.getTransactionActivity().tracerFinished(t1, 0);
        finishTransaction(tx, rootTracer);

        Set<TransactionActivity> done = tx.getFinishedChildren();
        assertEquals(1, done.size());
        TransactionActivity txa = done.iterator().next();
        List<Tracer> tracers = txa.getTracers();
        assertEquals(3, tracers.size());
        assertTrue(tracers.contains(t1));
        assertTrue(tracers.contains(t2));
        assertTrue(tracers.contains(t3));

        // Verify that we send up the segment_clamp supportability metric
        TransactionStats stats = txa.getTransactionStats();
        assertNotNull(stats);
        StatsImpl segmentClamp = (StatsImpl) stats.getUnscopedStats().getStatsMap().get(MetricNames.SUPPORTABILITY_TRANSACTION_SEGMENT_CLAMP);
        assertNotNull(segmentClamp);
        assertEquals(4, (int) segmentClamp.getTotal()); // The limit is 3 so the clamp will take effect at 4
        assertEquals(1, segmentClamp.getCallCount()); // Should get incremented once for the tx
    }

    private void finishTransaction(Transaction tx, BasicRequestRootTracer rootTracer) throws InterruptedException {
        tx.getTransactionActivity().tracerFinished(rootTracer, 0);
        Thread.sleep(100);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testTracerLimitAsync() throws Exception {
        Map<String, Object> config = createConfigMap();
        Map<String, Object> ttmap = (Map) config.get(AgentConfigImpl.TRANSACTION_TRACER);
        ttmap.put("segment_limit", 3);
        createServiceManager(config);

        Object context1 = 1L;
        Object context2 = 2L;
        Object context3 = 3L;
        Object context4 = 4L;
        Object context5 = 5L;

        Transaction.clearTransaction();
        Transaction tx = Transaction.getTransaction();
        BasicRequestRootTracer rootTracer = (BasicRequestRootTracer) createDispatcherTracer(true);
        tx.getTransactionActivity().tracerStarted(rootTracer);
        assertTrue(ServiceFactory.getAsyncTxService().registerAsyncActivity(context1));

        InitThread async1 = new InitThread(context1, context2, false);
        async1.start();
        async1.join();

        assertEquals(1, tx.getFinishedChildren().size());
        assertEquals(1, tx.getRunningTransactionActivityCount());

        InitThread async2 = new InitThread(context2, context3, false);
        async2.start();
        async2.join();

        assertEquals(2, tx.getFinishedChildren().size());
        assertEquals(1, tx.getRunningTransactionActivityCount());

        InitThread async3 = new InitThread(context3, context4, false);
        async3.start();
        async3.join();

        assertEquals(3, tx.getFinishedChildren().size());
        assertEquals(1, tx.getRunningTransactionActivityCount());
        TransactionStats stats = tx.getOverLimitTxStatsforTesting();
        assertNull(stats);

        InitThread async4 = new InitThread(context4, context5, false);
        async4.start();
        async4.join();

        assertEquals(3, tx.getFinishedChildren().size());
        assertEquals(1, tx.getRunningTransactionActivityCount());
        stats = tx.getOverLimitTxStatsforTesting();
        assertNotNull(stats);
        ResponseTimeStatsImpl theStat = (ResponseTimeStatsImpl) stats.getScopedStats().getStatsMap().get(
                "Java/com.newrelic.agent.TransactionTest/dude");
        assertNotNull(theStat);
        float initValue = theStat.getTotal();
        assertEquals(1, theStat.getCallCount());

        InitThread async5 = new InitThread(context5, null, false);
        async5.start();
        async5.join();

        assertEquals(3, tx.getFinishedChildren().size());
        assertEquals(1, tx.getRunningTransactionActivityCount());

        stats = tx.getOverLimitTxStatsforTesting();
        assertNotNull(stats);
        theStat = (ResponseTimeStatsImpl) stats.getScopedStats().getStatsMap().get(
                "Java/com.newrelic.agent.TransactionTest/dude");
        assertNotNull(theStat);
        assertTrue(theStat.getTotal() > initValue);
        assertEquals(2, theStat.getCallCount());

        finishTransaction(tx, rootTracer);

        assertEquals(4, tx.getFinishedChildren().size());
        assertEquals(0, tx.getRunningTransactionActivityCount());
        // here all of the stats should have been merged in - not just the TXAs that were dropped
        stats = tx.getOverLimitTxStatsforTesting();
        assertNotNull(stats);
        theStat = (ResponseTimeStatsImpl) stats.getScopedStats().getStatsMap().get(
                "Java/com.newrelic.agent.TransactionTest/dude");
        assertNotNull(theStat);
        assertTrue(theStat.getTotal() > initValue);
        assertEquals(5, theStat.getCallCount());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testTracerLimitSync() throws Exception {
        Map<String, Object> config = createConfigMap();
        Map<String, Object> ttmap = (Map) config.get(AgentConfigImpl.TRANSACTION_TRACER);
        ttmap.put(TransactionTracerConfigImpl.SEGMENT_LIMIT, 5);
        createServiceManager(config);

        Transaction.clearTransaction();
        Transaction tx = Transaction.getTransaction();
        BasicRequestRootTracer rootTracer = (BasicRequestRootTracer) createDispatcherTracer(true);
        tx.getTransactionActivity().tracerStarted(rootTracer);

        for (int i = 0; i < 10; i++) {
            Segment segment = rootTracer.getTransactionActivity().getTransaction().startSegment(MetricNames.CUSTOM, "Custom Sync Segment");
            if (segment != null) {
                segment.end();
            }
            // Wait for segment to end
            Thread.sleep(5);
        }

        finishTransaction(tx, rootTracer);

        assertEquals(6, tx.getTransactionCounts().getSegmentCount());
        assertEquals(0, tx.getRunningTransactionActivityCount());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testTracerLimitAsyncWhenStartedLate() throws Exception {
        Map<String, Object> config = createConfigMap();
        Map<String, Object> ttmap = (Map) config.get(AgentConfigImpl.TRANSACTION_TRACER);
        ttmap.put("segment_limit", 3);
        createServiceManager(config);

        Object context1 = 1L;
        Object context2 = 2L;
        Object context3 = 3L;
        Object context4 = 4L;
        Object context5 = 5L;

        Transaction.clearTransaction();
        Transaction tx = Transaction.getTransaction();
        BasicRequestRootTracer rootTracer = (BasicRequestRootTracer) createDispatcherTracer(true);
        tx.getTransactionActivity().tracerStarted(rootTracer);

        // register context1, but don't start it until we're over the segment limit
        assertTrue(ServiceFactory.getAsyncTxService().registerAsyncActivity(context1));

        // register context1 again, make sure we return false
        assertFalse(ServiceFactory.getAsyncTxService().registerAsyncActivity(context1));

        // register context2
        assertTrue(ServiceFactory.getAsyncTxService().registerAsyncActivity(context2));

        // start context2 on new thread and register context3
        InitThread doContext2 = new InitThread(context2, context3, false);
        doContext2.start();
        doContext2.join();

        // activity for context2 should finish normally
        assertEquals(1, tx.getFinishedChildren().size());

        // start context3 on new thread and register context4
        InitThread doContext3 = new InitThread(context3, context4, false);
        doContext3.start();
        doContext3.join();

        // activity for context3 should finish normally
        assertEquals(2, tx.getFinishedChildren().size());

        // start context4 on new thread and register context5
        InitThread doContext4 = new InitThread(context4, context5, false);
        doContext4.start();
        doContext4.join();

        // activity for context4 should finish normally
        assertEquals(3, tx.getFinishedChildren().size());

        // stats object to hold additional activity results should be created
        TransactionStats stats = tx.getOverLimitTxStatsforTesting();
        assertNull(stats);

        // start context5 on a new thread
        InitThread doContext5 = new InitThread(context5, null, false);
        doContext5.start();
        doContext5.join();

        // context5 should not be retained in finished children set because we're over the segment limit
        assertEquals(3, tx.getFinishedChildren().size());

        // context5's stats should get dumped into the tx stats object
        stats = tx.getOverLimitTxStatsforTesting();
        assertNotNull(stats);
        ResponseTimeStatsImpl theStat = (ResponseTimeStatsImpl) stats.getScopedStats().getStatsMap().get(
                "Java/com.newrelic.agent.TransactionTest/dude");
        assertNotNull(theStat);
        float initValue = theStat.getTotal();
        assertEquals(1, theStat.getCallCount());

        // start context1 on a new thread
        InitThread doContext1 = new InitThread(context1, null, false);
        doContext1.start();
        doContext1.join();

        // context1 should not be retained in finished children set because we're over the segment limit
        assertEquals(3, tx.getFinishedChildren().size());

        // context1's stats should get dumped into the tx stats object
        stats = tx.getOverLimitTxStatsforTesting();
        assertNotNull(stats);
        theStat = (ResponseTimeStatsImpl) stats.getScopedStats().getStatsMap().get(
                "Java/com.newrelic.agent.TransactionTest/dude");
        assertNotNull(theStat);
        assertTrue(theStat.getTotal() > initValue);
        assertEquals(2, theStat.getCallCount());

        // finish the transaction
        finishTransaction(tx, rootTracer);

        // root activity gets placed in finished children
        assertEquals(4, tx.getFinishedChildren().size());

        // all of the stats should have been merged in - not just the TXAs that were dropped
        stats = tx.getOverLimitTxStatsforTesting();
        assertNotNull(stats);
        theStat = (ResponseTimeStatsImpl) stats.getScopedStats().getStatsMap().get(
                "Java/com.newrelic.agent.TransactionTest/dude");
        assertNotNull(theStat);
        assertTrue(theStat.getTotal() > initValue);
        assertEquals(5, theStat.getCallCount());
    }

    class InitThread extends Thread {
        private Object thisContext;
        private Object regContext;
        private boolean first;

        InitThread(Object thisC, Object regC, boolean isFirst) {
            thisContext = thisC;
            regContext = regC;
            first = isFirst;
        }

        @Override
        public void run() {
            Tracer async = createDispatcherTracer(first);
            Transaction.getTransaction().getTransactionActivity().tracerStarted(async);
            ServiceFactory.getAsyncTxService().startAsyncActivity(thisContext);
            if (regContext != null) {
                ServiceFactory.getAsyncTxService().registerAsyncActivity(regContext);
            }
            async.finish(0, null);
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testTracerLimitAsyncMultiple() throws Exception {
        Map<String, Object> config = createConfigMap();
        Map<String, Object> ttmap = (Map) config.get(AgentConfigImpl.TRANSACTION_TRACER);
        ttmap.put("segment_limit", 3);
        createServiceManager(config);

        Object context1 = 1L;
        Object context2 = 2L;
        Object context3 = 3L;
        Object context4 = 4L;
        Object context5 = 5L;

        Transaction.clearTransaction();
        Transaction tx = Transaction.getTransaction();
        BasicRequestRootTracer rootTracer = (BasicRequestRootTracer) createDispatcherTracer(true);
        tx.getTransactionActivity().tracerStarted(rootTracer);
        assertTrue(ServiceFactory.getAsyncTxService().registerAsyncActivity(context1));

        InitMultipleThread async1 = new InitMultipleThread(context1, context2, false);
        async1.start();
        async1.join();

        assertEquals(1, tx.getFinishedChildren().size());
        assertEquals(1, tx.getRunningTransactionActivityCount());
        TransactionStats stats = tx.getOverLimitTxStatsforTesting();
        assertNull(stats);

        InitMultipleThread async2 = new InitMultipleThread(context2, context3, false);
        async2.start();
        async2.join();

        // two have been stored - the rest should be removed
        assertEquals(1, tx.getFinishedChildren().size());
        assertEquals(1, tx.getRunningTransactionActivityCount());
        float previous = verifyMetric(tx, 1, 0);
        InitMultipleThread async3 = new InitMultipleThread(context3, context4, false);
        async3.start();
        async3.join();

        assertEquals(1, tx.getFinishedChildren().size());
        assertEquals(1, tx.getRunningTransactionActivityCount());
        previous = verifyMetric(tx, 2, previous);
        InitMultipleThread async4 = new InitMultipleThread(context4, context5, false);
        async4.start();
        async4.join();

        assertEquals(1, tx.getFinishedChildren().size());
        assertEquals(1, tx.getRunningTransactionActivityCount());
        previous = verifyMetric(tx, 3, previous);

        InitMultipleThread async5 = new InitMultipleThread(context5, null, false);
        async5.start();
        async5.join();

        assertEquals(1, tx.getFinishedChildren().size());
        assertEquals(1, tx.getRunningTransactionActivityCount());
        previous = verifyMetric(tx, 4, previous);

        finishTransaction(tx, rootTracer);

        assertEquals(0, tx.getRunningTransactionActivityCount());
        assertEquals(2, tx.getFinishedChildren().size());
        // here all of the stats should have been merged in - not just the TXAs that were dropped
        verifyMetric(tx, 5, previous);
    }

    private float verifyMetric(Transaction tx, int count, float previous) {
        TransactionStats stats = tx.getOverLimitTxStatsforTesting();
        assertNotNull(stats);
        ResponseTimeStatsImpl theStat = (ResponseTimeStatsImpl) stats.getScopedStats().getStatsMap().get(
                "Java/com.newrelic.agent.TransactionTest/dude");
        float myValue = theStat.getTotal();
        assertTrue(myValue > previous);
        assertEquals(count, theStat.getCallCount());
        assertEquals(count,
                ((ResponseTimeStatsImpl) stats.getScopedStats().getStatsMap().get("Custom/mynameone")).getCallCount());
        assertEquals(count,
                ((ResponseTimeStatsImpl) stats.getScopedStats().getStatsMap().get("Custom/mynametwo")).getCallCount());
        assertEquals(count, ((ResponseTimeStatsImpl) stats.getScopedStats().getStatsMap().get(
                "Custom/mynamethree")).getCallCount());
        assertEquals(count, ((ResponseTimeStatsImpl) stats.getScopedStats().getStatsMap().get(
                "Custom/mynamefour")).getCallCount());
        return myValue;
    }

    class InitMultipleThread extends Thread {
        private Object thisContext;
        private Object regContext;
        private boolean first;

        InitMultipleThread(Object thisC, Object regC, boolean isFirst) {
            thisContext = thisC;
            regContext = regC;
            first = isFirst;
        }

        @Override
        public void run() {
            Tracer async = createDispatcherTracer(first);
            Transaction.getTransaction().getTransactionActivity().tracerStarted(async);
            ServiceFactory.getAsyncTxService().startAsyncActivity(thisContext);
            if (regContext != null) {
                ServiceFactory.getAsyncTxService().registerAsyncActivity(regContext);
            }

            DefaultTracer t1 = createBasicTracer("one");
            Transaction.getTransaction().getTransactionActivity().tracerStarted(t1);
            t1.finish(0, null);

            DefaultTracer t2 = createBasicTracer("two");
            Transaction.getTransaction().getTransactionActivity().tracerStarted(t2);
            t2.finish(0, null);

            DefaultTracer t3 = createBasicTracer("three");
            Transaction.getTransaction().getTransactionActivity().tracerStarted(t3);
            t3.finish(0, null);

            DefaultTracer t4 = createBasicTracer("four");
            Transaction.getTransaction().getTransactionActivity().tracerStarted(t4);
            t4.finish(0, null);

            async.finish(0, null);
        }
    }

    // you really should not be starting late like this, but it should still work
    @SuppressWarnings("unchecked")
    @Test
    public void testTracerLimitAsyncStartLate() throws Exception {
        Map<String, Object> config = createConfigMap();
        Map<String, Object> ttmap = (Map) config.get(AgentConfigImpl.TRANSACTION_TRACER);
        ttmap.put("segment_limit", 3);
        createServiceManager(config);

        Object context1 = 1L;
        Object context2 = 2L;
        Object context3 = 3L;
        Object context4 = 4L;
        Object context5 = 5L;

        Transaction.clearTransaction();
        Transaction tx = Transaction.getTransaction();
        BasicRequestRootTracer rootTracer = (BasicRequestRootTracer) createDispatcherTracer(true);
        tx.getTransactionActivity().tracerStarted(rootTracer);
        assertTrue(ServiceFactory.getAsyncTxService().registerAsyncActivity(context1));

        InitThreadStartLate async1 = new InitThreadStartLate(context1, context2, false);
        async1.start();
        async1.join();

        assertEquals(1, tx.getFinishedChildren().size());
        TransactionStats stats = tx.getOverLimitTxStatsforTesting();
        assertNull(stats);

        InitThreadStartLate async2 = new InitThreadStartLate(context2, context3, false);
        async2.start();
        async2.join();

        assertEquals(1, tx.getFinishedChildren().size());
        float previous = verifyMetric(tx, 1, 0);
        InitThreadStartLate async3 = new InitThreadStartLate(context3, context4, false);
        async3.start();
        async3.join();

        assertEquals(1, tx.getFinishedChildren().size());
        previous = verifyMetric(tx, 2, previous);
        InitThreadStartLate async4 = new InitThreadStartLate(context4, context5, false);
        async4.start();
        async4.join();

        assertEquals(1, tx.getFinishedChildren().size());
        previous = verifyMetric(tx, 3, previous);

        InitThreadStartLate async5 = new InitThreadStartLate(context5, null, false);
        async5.start();
        async5.join();

        assertEquals(1, tx.getFinishedChildren().size());
        previous = verifyMetric(tx, 4, previous);

        finishTransaction(tx, rootTracer);

        assertEquals(2, tx.getFinishedChildren().size());
        // here all of the stats should have been merged in - not just the TXAs that were dropped
        verifyMetric(tx, 5, previous);
    }

    public class InitThreadStartLate extends Thread {
        private Object thisContext;
        private Object regContext;
        private boolean first;

        InitThreadStartLate(Object thisC, Object regC, boolean isFirst) {
            thisContext = thisC;
            regContext = regC;
            first = isFirst;
        }

        @Override
        public void run() {
            Tracer async = createDispatcherTracer(first);
            Transaction.getTransaction().getTransactionActivity().tracerStarted(async);

            DefaultTracer t1 = createBasicTracer("one");
            Transaction.getTransaction().getTransactionActivity().tracerStarted(t1);
            t1.finish(0, null);

            DefaultTracer t2 = createBasicTracer("two");
            Transaction.getTransaction().getTransactionActivity().tracerStarted(t2);
            t2.finish(0, null);

            DefaultTracer t3 = createBasicTracer("three");
            Transaction.getTransaction().getTransactionActivity().tracerStarted(t3);
            t3.finish(0, null);

            DefaultTracer t4 = createBasicTracer("four");
            Transaction.getTransaction().getTransactionActivity().tracerStarted(t4);
            t4.finish(0, null);

            ServiceFactory.getAsyncTxService().startAsyncActivity(thisContext);
            if (regContext != null) {
                ServiceFactory.getAsyncTxService().registerAsyncActivity(regContext);
            }

            async.finish(0, null);
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testTracerLimit2() throws Exception {
        Map<String, Object> config = createConfigMap();
        Map<String, Object> ttmap = (Map) config.get(AgentConfigImpl.TRANSACTION_TRACER);
        ttmap.put("segment_limit", 3);
        createServiceManager(config);

        Transaction.clearTransaction();
        Transaction tx = Transaction.getTransaction();
        BasicRequestRootTracer rootTracer = (BasicRequestRootTracer) createDispatcherTracer(true);

        tx.getTransactionActivity().tracerStarted(rootTracer);
        DefaultTracer t1 = createBasicTracer("one");
        tx.getTransactionActivity().tracerStarted(t1);
        tx.getTransactionActivity().tracerFinished(t1, 0);

        DefaultTracer t2 = createBasicTracer("two");
        tx.getTransactionActivity().tracerStarted(t2);
        tx.getTransactionActivity().tracerFinished(t2, 0);

        DefaultTracer t3 = createBasicTracer("three");
        tx.getTransactionActivity().tracerStarted(t3);
        tx.getTransactionActivity().tracerFinished(t3, 0);

        DefaultTracer t4 = createBasicTracer("four");
        tx.getTransactionActivity().tracerStarted(t4);
        tx.getTransactionActivity().tracerFinished(t4, 0);

        DefaultTracer t5 = createBasicTracer("five");
        tx.getTransactionActivity().tracerStarted(t5);
        tx.getTransactionActivity().tracerFinished(t5, 0);

        finishTransaction(tx, rootTracer);

        Set<TransactionActivity> done = tx.getFinishedChildren();
        assertEquals(1, done.size());
        TransactionActivity txa = done.iterator().next();
        List<Tracer> tracers = txa.getTracers();
        assertEquals(3, tracers.size());
        assertTrue(tracers.contains(t1));
        assertTrue(tracers.contains(t2));
        assertTrue(tracers.contains(t3));
    }

    @Test
    public void checkRunningDuration() throws Exception {
        createServiceManager(createConfigMap());
        Transaction.clearTransaction();
        Transaction tx = Transaction.getTransaction();
        BasicRequestRootTracer rootTracer = (BasicRequestRootTracer) createDispatcherTracer(true);

        tx.getTransactionActivity().tracerStarted(rootTracer);
        long firstTime = tx.getRunningDurationInNanos();
        Thread.sleep(2);
        finishTransaction(tx, rootTracer);
        long endTime = tx.getRunningDurationInNanos();
        assertTrue(endTime > firstTime);
    }

    @Test
    public void testIgnoreErrors() throws Exception {
        Map<String, Object> configMap = createConfigMap();
        createServiceManager(configMap);

        Transaction.clearTransaction();
        Transaction tx = Transaction.getTransaction();
        assertFalse(tx.isErrorReportableAndNotIgnored());

        // arguably this should not be allowed if the tx isn't started.
        tx.setThrowable(new Exception(), TransactionErrorPriority.API, false);
        tx.ignoreErrors();

        assertTrue("Calling before tx start should have no effect.", tx.isErrorReportableAndNotIgnored());

        tx.setDispatcher(new OtherDispatcher(tx, null));
        tx.ignoreErrors();
        assertFalse("Error should be ignored.", tx.isErrorReportableAndNotIgnored());
    }

    @Test
    public void testDoFinish() throws Exception {
        createServiceManager(createConfigMap());
        TransactionService transactionService = Mockito.mock(TransactionService.class);
        ((MockServiceManager) ServiceFactory.getServiceManager()).setTransactionService(transactionService);
        Transaction.clearTransaction();
        Transaction tx = Transaction.getTransaction();
        tx.setPriorityTransactionName(PriorityTransactionName.create("TEST", null, TransactionNamePriority.NONE));

        final TransactionTracerConfig tracerConfig = Mockito.mock(TransactionTracerConfig.class);
        MockDispatcherTracer tracer = new MockDispatcherTracer(tx) {
            @Override
            public TransactionTracerConfig getTransactionTracerConfig() {
                return tracerConfig;
            }

            @Override
            public boolean isTransactionSegment() {
                return true;
            }
        };
        tx.getTransactionActivity().tracerStarted(tracer);
        tx.getTransactionActivity().tracerFinished(tracer, 0);
        Mockito.verify(transactionService, Mockito.atLeastOnce()).transactionFinished(
                Mockito.any(TransactionData.class), Mockito.any(TransactionStats.class));
        Mockito.verify(transactionService, Mockito.atLeastOnce()).transactionStarted(Mockito.any(Transaction.class));
        Mockito.verify(transactionService, Mockito.atLeastOnce()).transactionFinished(Mockito.any(
                TransactionData.class), Mockito.any(TransactionStats.class));
        Mockito.verifyNoMoreInteractions(transactionService);
    }

    @Test
    public void recordCpuAndGCTime() throws Exception {
        Map<String, Object> configMap = createConfigMap();
        createServiceManager(configMap);

        Transaction.clearTransaction();
        Transaction tx = Transaction.getTransaction();
        BasicRequestRootTracer rootTracer = (BasicRequestRootTracer) createDispatcherTracer(true);

        Thread.sleep(40);
        tx.getTransactionActivity().tracerStarted(rootTracer);
        rootTracer.finish(Opcodes.ARETURN, null);
        tx.recordFinalGCTime(tx.getTransactionActivity().getTransactionStats());
        Long cpuTime = (Long) tx.getIntrinsicAttributes().get(AttributeNames.CPU_TIME_PARAMETER_NAME);
        assertNotNull(cpuTime);
        assertTrue(cpuTime > 0);
        tx.recordFinalGCTime(tx.getTransactionActivity().getTransactionStats());
        Long cpuTime2 = (Long) tx.getIntrinsicAttributes().get(AttributeNames.CPU_TIME_PARAMETER_NAME);
        assertEquals(cpuTime, cpuTime2);
    }

    @Test
    public void recordCpuAndGCTimeWithLegacyAsync() throws Exception {
        Map<String, Object> configMap = createConfigMap();
        createServiceManager(configMap);

        Transaction.clearTransaction();
        Transaction tx = Transaction.getTransaction();
        BasicRequestRootTracer rootTracer = (BasicRequestRootTracer) createDispatcherTracer(true);

        Thread.sleep(40);
        tx.getTransactionActivity().tracerStarted(rootTracer);
        tx.addTotalCpuTimeForLegacy(5000000);
        rootTracer.finish(Opcodes.ARETURN, null);
        tx.recordFinalGCTime(tx.getTransactionActivity().getTransactionStats());
        Long cpuTime = (Long) tx.getIntrinsicAttributes().get(AttributeNames.CPU_TIME_PARAMETER_NAME);
        assertNotNull(cpuTime);
        assertTrue(cpuTime > 5000000);
        tx.recordFinalGCTime(tx.getTransactionActivity().getTransactionStats());
        Long cpuTime2 = (Long) tx.getIntrinsicAttributes().get(AttributeNames.CPU_TIME_PARAMETER_NAME);
        assertEquals(cpuTime, cpuTime2);
    }

    @Test
    public void recordCpuAndGCTimeWithLegacyIsNotReported() throws Exception {
        Map<String, Object> configMap = createConfigMap();
        createServiceManager(configMap);

        Transaction.clearTransaction();
        Transaction tx = Transaction.getTransaction();
        BasicRequestRootTracer rootTracer = (BasicRequestRootTracer) createDispatcherTracer(true);

        Thread.sleep(40);
        tx.getTransactionActivity().tracerStarted(rootTracer);
        tx.addTotalCpuTimeForLegacy(TransactionActivity.NOT_REPORTED);
        rootTracer.finish(Opcodes.ARETURN, null);
        tx.recordFinalGCTime(tx.getTransactionActivity().getTransactionStats());
        Long cpuTime = (Long) tx.getIntrinsicAttributes().get(AttributeNames.CPU_TIME_PARAMETER_NAME);
        // should not be reported
        assertNull(cpuTime);
    }

    @Test
    public void recordCpuAndGCTimeNotEnabled() throws Exception {
        Map<String, Object> configMap = createConfigMap();
        configMap.put(AgentConfigImpl.THREAD_CPU_TIME_ENABLED, Boolean.FALSE);
        createServiceManager(configMap);

        Transaction.clearTransaction();
        Transaction tx = Transaction.getTransaction();
        BasicRequestRootTracer rootTracer = (BasicRequestRootTracer) createDispatcherTracer(true);

        tx.getTransactionActivity().tracerStarted(rootTracer);
        finishTransaction(tx, rootTracer);
        Long cpuTime = (Long) tx.getIntrinsicAttributes().get(AttributeNames.CPU_TIME_PARAMETER_NAME);
        assertNull(cpuTime);
    }

    @Test
    public void addUserParams() throws Exception {
        Map<String, Object> configMap = createConfigMap();
        createServiceManager(configMap);

        Transaction.clearTransaction();
        Transaction tx = Transaction.getTransaction();
        BasicRequestRootTracer rootTracer = (BasicRequestRootTracer) createDispatcherTracer(true);

        Thread.sleep(40);
        tx.getTransactionActivity().tracerStarted(rootTracer);
        tx.getUserAttributes().put("key1", "value1");

        finishTransaction(tx, rootTracer);
        Long cpuTime = (Long) tx.getIntrinsicAttributes().get(AttributeNames.CPU_TIME_PARAMETER_NAME);
        assertNotNull(cpuTime);
        assertTrue(cpuTime > 0);
        assertEquals(2, tx.getAgentAttributes().size());
        assertNotNull(tx.getAgentAttributes().get("jvm.thread_name"));
        assertEquals(1, tx.getUserAttributes().size());
        assertEquals("value1", tx.getUserAttributes().get("key1"));
    }

    @Test
    public void testInstanceNameAttribute() throws Exception {
        Map<String, Object> configMap = createConfigMap();
        configMap.put("instance_name", "MyInstanceName");
        createServiceManager(configMap);

        Transaction.clearTransaction();
        Transaction tx = Transaction.getTransaction();
        BasicRequestRootTracer rootTracer = (BasicRequestRootTracer) createDispatcherTracer(true);
        Thread.sleep(10);
        tx.getTransactionActivity().tracerStarted(rootTracer);
        finishTransaction(tx, rootTracer);
        assertEquals(3, tx.getAgentAttributes().size());
        String actualInstanceName = (String) tx.getAgentAttributes().get("process.instanceName");
        assertNotNull(actualInstanceName);
        assertEquals("MyInstanceName", actualInstanceName);
        assertNotNull(tx.getAgentAttributes().get("jvm.thread_name"));

        configMap = createConfigMap();
        createServiceManager(configMap);
        Transaction.clearTransaction();
        tx = Transaction.getTransaction();
        rootTracer = (BasicRequestRootTracer) createDispatcherTracer(true);

        Thread.sleep(10);
        tx.getTransactionActivity().tracerStarted(rootTracer);
        finishTransaction(tx, rootTracer);
        assertEquals(2, tx.getAgentAttributes().size());
        actualInstanceName = (String) tx.getAgentAttributes().get("process.instanceName");
        assertNull(actualInstanceName);
        assertNotNull(tx.getAgentAttributes().get("jvm.thread_name"));
    }

    @Test
    public void testDisplayHostAttribute() throws Exception {
        Map<String, Object> configMap = createConfigMap();
        Map<String, Object> values = new HashMap<>();
        values.put("display_name", "onetwothree");
        configMap.put("process_host", values);
        createServiceManager(configMap);

        Transaction.clearTransaction();
        Transaction tx = Transaction.getTransaction();
        BasicRequestRootTracer rootTracer = (BasicRequestRootTracer) createDispatcherTracer(true);
        Thread.sleep(10);
        tx.getTransactionActivity().tracerStarted(rootTracer);
        finishTransaction(tx, rootTracer);
        assertEquals(3, tx.getAgentAttributes().size());
        String actualDisplayName = (String) tx.getAgentAttributes().get("host.displayName");
        assertNotNull(actualDisplayName);
        assertEquals("onetwothree", actualDisplayName);
        assertNotNull(tx.getAgentAttributes().get("jvm.thread_name"));
    }

    @Test
    public void testNoThreadName() throws Exception {
        createServiceManager(createConfigMap());
        Transaction.clearTransaction();

        Tracer dispatcherTracer = createDispatcherTracer(true);
        Transaction transaction = dispatcherTracer.getTransactionActivity().getTransaction();

        dispatcherTracer.getTransactionActivity().tracerStarted(dispatcherTracer);
        Token token = transaction.getToken();
        dispatcherTracer.getTransactionActivity().tracerFinished(dispatcherTracer, Opcodes.ARETURN);

        token.expire();
        // Wait until token is expired and transaction finishes
        sleep(1000);
        assertTrue(transaction.isFinished());

        Map<String, Object> agentAttributes = transaction.getAgentAttributes();
        assertNull(agentAttributes.get("jvm.thread_name"));
    }

    @Test
    public void testThreadName() throws Exception {
        createServiceManager(createConfigMap());
        Transaction.clearTransaction();

        Tracer dispatcherTracer = createDispatcherTracer(true);
        Transaction transaction = dispatcherTracer.getTransactionActivity().getTransaction();
        TransactionActivity transactionActivity = dispatcherTracer.getTransactionActivity();
        transactionActivity.tracerStarted(dispatcherTracer);
        transactionActivity.tracerFinished(dispatcherTracer, Opcodes.ARETURN);
        assertTrue(transaction.isFinished());

        Map<String, Object> agentAttributes = transaction.getAgentAttributes();
        assertEquals(Thread.currentThread().getName(), agentAttributes.get("jvm.thread_name"));
    }

    @Test
    public void testNoPayloadInitialValue() throws Exception {
        createServiceManager(createConfigMap());
        Transaction.clearTransaction();
        Transaction tx = Transaction.getTransaction(true);
        assertNull(tx.getSpanProxy().getInboundDistributedTracePayload());
    }

    @Test
    public void testCreatePayload() throws Exception {
        Map<String, Object> configMap = createConfigMap();
        configMap.put(AgentConfigImpl.DISTRIBUTED_TRACING, ImmutableMap.of("enabled", Boolean.TRUE));
        createServiceManager(configMap);
        Transaction.clearTransaction();
        Tracer dispatcherTracer = createDispatcherTracer(true);
        Transaction transaction = dispatcherTracer.getTransactionActivity().getTransaction();
        transaction.getTransactionActivity().tracerStarted(dispatcherTracer);
        String spanId = "honkhonkhonk";
        DistributedTracePayloadImpl payload = transaction.createDistributedTracePayload(spanId);
        assertNotNull(payload);
        dispatcherTracer.finish(Opcodes.ARETURN, null);

        assertEquals("App", payload.parentType);
        assertEquals(spanId, payload.guid);
        assertTrue(DistributedTraceUtil.isSampledPriority(payload.priority)); // first 10 transactions should be sampled
    }

    /**
     * The same logic exists in {@link AnalyticsEvent},
     * {@link com.newrelic.agent.Transaction}, and {@link com.newrelic.agent.tracing.DistributedTraceServiceImpl}.
     */
    @Test
    public void testNumberFormats() throws Exception {
        Map<String, Object> config = createConfigMap();
        createServiceManager(config);

        Locale.setDefault(new Locale("ar", "AE"));
        nextFloat();

        Locale.setDefault(Locale.GERMANY);
        nextFloat();

        Locale.setDefault(Locale.CHINA);
        nextFloat();

        Locale.setDefault(Locale.US);
        nextFloat();
    }

    private void nextFloat() {
        try {
            DistributedTraceServiceImpl.nextTruncatedFloat();
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testAcceptCreatePayload() throws Exception {
        Map<String, Object> configMap = createNRDTConfigMap(false);
        createServiceManager(configMap);
        serviceManager.setDistributedTraceService(mockDistributedTraceService);

        Transaction.clearTransaction();
        Tracer dispatcherTracer = createDispatcherTracer(true);
        Transaction transaction = dispatcherTracer.getTransactionActivity().getTransaction();
        transaction.getTransactionActivity().tracerStarted(dispatcherTracer);

        String inboundPayload =
                "{" +
                        "  \"v\": [0,2]," +
                        "  \"d\": {" +
                        "    \"ty\": \"Mobile\"," +
                        "    \"ac\": \"9123\"," +
                        "    \"tk\": \"67890\"," +
                        "    \"ap\": \"51424\"" +
                        "    \"id\": \"27856f70d3d314b7\"," +
                        "    \"tr\": \"3221bf09aa0bcf0d\"," +
                        "    \"pr\": 0.999993," +
                        "    \"ti\": 1482959525577" +
                        "  }" +
                        "}";

        transaction.acceptDistributedTracePayload(inboundPayload);
        String spanId = "meatball99";
        DistributedTracePayloadImpl payload = transaction.createDistributedTracePayload(spanId);
        DistributedTracePayloadImpl secondPayload = transaction.createDistributedTracePayload(spanId);
        dispatcherTracer.finish(Opcodes.ARETURN, null);

        assertEquals("App", payload.parentType);
        assertEquals(spanId, payload.guid);
        assertEquals("3221bf09aa0bcf0d", payload.traceId);
        assertNotNull(transaction.getSpanProxy().getInboundDistributedTracePayload());
        assertEquals("Mobile", transaction.getSpanProxy().getInboundDistributedTracePayload().parentType);
        assertEquals(0.999993f, payload.priority, 0.0f);

        assertEquals("App", secondPayload.parentType);
        assertEquals("3221bf09aa0bcf0d", secondPayload.traceId);
        assertEquals(spanId, secondPayload.guid);
        assertEquals(0.999993f, secondPayload.priority, 0.0f);
    }

    @Test
    public void noticeThenSetAsTracerPicksUpOriginalSpanNotCurrent() throws Exception {
        createServiceManager(Collections.<String, Object>emptyMap());
        Transaction.clearTransaction();
        Tracer dispatcherTracer = createDispatcherTracer(true);
        Transaction tx = dispatcherTracer.getTransactionActivity().getTransaction();
        tx.getTransactionActivity().tracerStarted(dispatcherTracer);
        Throwable exc = new Throwable();

        tx.noticeTracerException(exc, "span id 1");
        tx.noticeTracerException(exc, "span id 2");
        tx.setThrowable(exc, TransactionErrorPriority.TRACER, false);
        assertEquals("span id 1", tx.getThrowable().spanId);
    }

    @Test
    public void noticeThenSetAsTracerSetsSpanToNullIfNoTracerAndNotNoticed() throws Exception {
        createServiceManager(Collections.<String, Object>emptyMap());
        Transaction.clearTransaction();
        Transaction tx = Transaction.getTransaction();
        Throwable exc = new Throwable();

        tx.setThrowable(exc, TransactionErrorPriority.TRACER, false);
        assertNull(tx.getThrowable().spanId);
    }

    @Test
    public void noticeThenSetAsAPISetsSpanToNullIfNoTracerAndNotNoticed() throws Exception {
        createServiceManager(Collections.<String, Object>emptyMap());
        Transaction.clearTransaction();
        Transaction tx = Transaction.getTransaction();
        Throwable exc = new Throwable();

        tx.setThrowable(exc, TransactionErrorPriority.API, false);
        assertNull(tx.getThrowable().spanId);
    }

    @Test
    public void noticeThenSetAsAPISetsSpanIfNoticedButNoSpan() throws Exception {
        createServiceManager(Collections.<String, Object>emptyMap());
        Transaction.clearTransaction();
        Transaction tx = Transaction.getTransaction();
        Throwable exc = new Throwable();

        tx.noticeTracerException(exc, "span id 1");
        tx.setThrowable(exc, TransactionErrorPriority.API, false);
        assertEquals("span id 1", tx.getThrowable().spanId);
    }

    @Test
    public void noticeThenSetAsAPIUsesCurrentSpan() throws Exception {
        createServiceManager(Collections.<String, Object>emptyMap());
        Transaction.clearTransaction();
        Tracer dispatcherTracer = createDispatcherTracer(true);
        Transaction tx = dispatcherTracer.getTransactionActivity().getTransaction();
        tx.getTransactionActivity().tracerStarted(dispatcherTracer);

        Throwable exc = new Throwable();

        tx.noticeTracerException(exc, "span id 1");
        tx.noticeTracerException(exc, "span id 2");
        tx.setThrowable(exc, TransactionErrorPriority.API, false);
        assertEquals(dispatcherTracer.getGuid(), tx.getThrowable().spanId);
    }

    @Test
    public void testWithNullPriorityDoesNotResetPriority() throws Exception {
        Map<String, Object> configMap = createConfigMap();
        configMap.put(AgentConfigImpl.DISTRIBUTED_TRACING, ImmutableMap.of("enabled", Boolean.TRUE));
        createServiceManager(configMap);
        serviceManager.setDistributedTraceService(new DistributedTraceService() {
            @Override
            public boolean isEnabled() {
                return true;
            }

            @Override
            public int getMajorSupportedCatVersion() {
                return 1;
            }

            @Override
            public int getMinorSupportedCatVersion() {
                return 0;
            }

            @Override
            public String getAccountId() {
                return "9123";
            }

            @Override
            public String getApplicationId() {
                return "1234";
            }

            @Override
            public <T extends PriorityAware> float calculatePriority(Float priority, SamplingPriorityQueue<T> reservoir) {
                return 0.678f;
            }

            @Override
            public Map<String, Object> getIntrinsics(DistributedTracePayloadImpl inboundPayload, String guid,
                    String traceId, TransportType transportType, long parentTransportDuration,
                    long largestTransportDuration, String parentId, String parentSpanId, float priority) {
                return null;
            }

            @Override
            public String getTrustKey() {
                return "67890";
            }

            @Override
            public DistributedTracePayload createDistributedTracePayload(Tracer tracer) {
                return null;
            }
        });

        Transaction.clearTransaction();
        Tracer dispatcherTracer = createDispatcherTracer(true);
        Transaction transaction = dispatcherTracer.getTransactionActivity().getTransaction();
        transaction.getTransactionActivity().tracerStarted(dispatcherTracer);

        String inboundPayload =
                "{" +
                        "  \"v\": [0,2]," +
                        "  \"d\": {" +
                        "    \"ty\": \"Mobile\"," +
                        "    \"ac\": \"9123\"," +
                        "    \"tk\": \"67890\"," +
                        "    \"ap\": \"51424\"" +
                        "    \"id\": \"27856f70d3d314b7\"," +
                        "    \"tr\": \"3221bf09aa0bcf0d\"," +
                        "    \"pr\": null," +
                        "    \"ti\": 1482959525577" +
                        "  }" +
                        "}";

        transaction.acceptDistributedTracePayload(inboundPayload);
        String spanId = "meatball101";
        DistributedTracePayloadImpl payload = transaction.createDistributedTracePayload(spanId);
        DistributedTracePayloadImpl secondPayload = transaction.createDistributedTracePayload(spanId);
        dispatcherTracer.finish(Opcodes.ARETURN, null);

        // Verify that when the inbound priority is null, that we retain our priority.
        assertEquals("Mobile", transaction.getSpanProxy().getInboundDistributedTracePayload().parentType);
        assertEquals(0.678f, payload.priority, 0.0f);

        // Verify that the payload that gets passed on will preserve the priority we created.
        assertEquals("App", secondPayload.parentType);
        assertEquals("3221bf09aa0bcf0d", secondPayload.traceId);
        assertEquals(spanId, secondPayload.guid);
        assertEquals(0.678f, secondPayload.priority, 0.0f);
    }

    @Test
    public void testSupportabilityCreateBeforeAccept() throws Exception {
        Map<String, Object> configMap = createNRDTConfigMap(false);
        createServiceManager(configMap);

        final CountDownLatch latch = new CountDownLatch(1);
        final SimpleStatsEngine unscopedStats = new SimpleStatsEngine();
        ServiceFactory.getTransactionService().addTransactionListener(new TransactionListener() {
            @Override
            public void dispatcherTransactionFinished(TransactionData transactionData, TransactionStats transactionStats) {
                if (transactionData.getPriorityTransactionName()
                        .getName()
                        .equals("WebTransaction/Test/createBeforeAcceptTxn")) {
                    unscopedStats.mergeStats(transactionStats.getUnscopedStats());
                    latch.countDown();
                }
            }
        });

        Transaction.clearTransaction();
        Tracer dispatcherTracer = createDispatcherTracer(true);
        Transaction transaction = dispatcherTracer.getTransactionActivity().getTransaction();
        transaction.getTransactionActivity().tracerStarted(dispatcherTracer);
        transaction.setTransactionName(com.newrelic.api.agent.TransactionNamePriority.CUSTOM_HIGH, true, "Test", "createBeforeAcceptTxn");
        transaction.createDistributedTracePayload("spanId31238ou");

        String inboundPayload =
                "{" +
                        "  \"v\": [0,2]," +
                        "  \"d\": {" +
                        "    \"ty\": \"Mobile\"," +
                        "    \"ac\": \"9123\"," +
                        "    \"tk\": \"67890\"," +
                        "    \"ap\": \"51424\"" +
                        "    \"id\": \"27856f70d3d314b7\"," +
                        "    \"tr\": \"3221bf09aa0bcf0d\"," +
                        "    \"pr\": 0.00001," +
                        "    \"ti\": 1482959525577" +
                        "  }" +
                        "}";
        transaction.acceptDistributedTracePayload(inboundPayload);
        dispatcherTracer.finish(Opcodes.ARETURN, null);

        latch.await();

        assertTrue(1 <= transaction.getTransactionActivity()
                .getTransactionStats()
                .getUnscopedStats()
                .getStats(MetricNames.SUPPORTABILITY_ACCEPT_PAYLOAD_CREATE_BEFORE_ACCEPT)
                .getCallCount());
    }

    @Test
    public void testSampleFlag() throws Exception {
        Map<String, Object> configMap = createNRDTConfigMap(false);
        createServiceManager(configMap);
        serviceManager.setDistributedTraceService(mockDistributedTraceService);

        Transaction.clearTransaction();
        Tracer dispatcherTracer = createDispatcherTracer(true);
        Transaction transaction = dispatcherTracer.getTransactionActivity().getTransaction();
        transaction.getTransactionActivity().tracerStarted(dispatcherTracer);
        String inboundPayload =
                "{" +
                        "  \"v\": [0,2]," +
                        "  \"d\": {" +
                        "    \"ty\": \"Mobile\"," +
                        "    \"ac\": \"9123\"," +
                        "    \"tk\": \"67890\"," +
                        "    \"ap\": \"51424\"" +
                        "    \"id\": \"27856f70d3d314b7\"," +
                        "    \"tr\": \"3221bf09aa0bcf0d\"," +
                        "    \"pr\": 0.0002," +
                        "    \"ti\": 1482959525577," +
                        "  }" +
                        "}";

        transaction.acceptDistributedTracePayload(inboundPayload);
        dispatcherTracer.finish(Opcodes.ARETURN, null);
        assertEquals(0.0002f, transaction.getPriority(), 0.0f);

        Transaction.clearTransaction();
        dispatcherTracer = createDispatcherTracer(true);
        transaction = dispatcherTracer.getTransactionActivity().getTransaction();
        transaction.getTransactionActivity().tracerStarted(dispatcherTracer);
        inboundPayload =
                "{" +
                        "  \"v\": [0,2]," +
                        "  \"d\": {" +
                        "    \"ty\": \"Mobile\"," +
                        "    \"ac\": \"9123\"," +
                        "    \"tk\": \"67890\"," +
                        "    \"ap\": \"51424\"" +
                        "    \"id\": \"27856f70d3d314b7\"," +
                        "    \"tr\": \"3221bf09aa0bcf0d\"," +
                        "    \"pr\": 1.348272," +
                        "    \"ti\": 1482959525577" +
                        "  }" +
                        "}";

        transaction.acceptDistributedTracePayload(inboundPayload);
        dispatcherTracer.finish(Opcodes.ARETURN, null);
        assertTrue(DistributedTraceUtil.isSampledPriority(transaction.getPriority()));
    }

    @Test
    public void testTransportDuration() throws Exception {
        Map<String, Object> configMap = createNRDTConfigMap(false);
        createServiceManager(configMap);
        serviceManager.setDistributedTraceService(mockDistributedTraceService);

        final long sentTimestamp = System.currentTimeMillis();
        String inboundPayload =
                "{" +
                        "  \"v\": [0,2]," +
                        "  \"d\": {" +
                        "    \"ty\": \"Mobile\"," +
                        "    \"ac\": \"9123\"," +
                        "    \"tk\": \"67890\"," +
                        "    \"ap\": \"51424\"" +
                        "    \"id\": \"27856f70d3d314b7\"," +
                        "    \"tr\": \"3221bf09aa0bcf0d\"," +
                        "    \"pr\": 0.33333," +
                        "    \"ti\": " + sentTimestamp +
                        "  }" +
                        "}";

        // Simulate some transport time
        sleep(100);

        Transaction.clearTransaction();
        Tracer dispatcherTracer = createDispatcherTracer(true);
        final long txnStartTime = System.currentTimeMillis();
        Transaction transaction = dispatcherTracer.getTransactionActivity().getTransaction();
        transaction.getTransactionActivity().tracerStarted(dispatcherTracer);

        transaction.acceptDistributedTracePayload(inboundPayload);
        dispatcherTracer.finish(Opcodes.ARETURN, null);

        assertTrue(isTransportDurationMillisWithinAcceptableDelta(transaction.getTransportDurationInMillis(), txnStartTime, sentTimestamp, 5));
        assertTrue(transaction.getTransportDurationInMillis() > 0);

        // No transport duration should be equal to 0
        Transaction.clearTransaction();
        dispatcherTracer = createDispatcherTracer(true);
        transaction = dispatcherTracer.getTransactionActivity().getTransaction();
        transaction.getTransactionActivity().tracerStarted(dispatcherTracer);
        assertEquals(0, transaction.getTransportDurationInMillis());

        // No negative transport duration
        long futureTimestamp = System.currentTimeMillis() + 1000000;
        inboundPayload =
                "{" +
                        "  \"v\": [0,2]," +
                        "  \"d\": {" +
                        "    \"ty\": \"APP\"," +
                        "    \"ac\": \"9123\"," +
                        "    \"tk\": \"67890\"," +
                        "    \"ap\": \"51424\"" +
                        "    \"id\": \"27856f70d3d314b7\"," +
                        "    \"tr\": \"3221bf09aa0bcf0d\"," +
                        "    \"pr\": 0.33333," +
                        "    \"ti\": " + futureTimestamp +
                        "  }" +
                        "}";
        transaction.acceptDistributedTracePayload(inboundPayload);
        assertEquals(0, transaction.getTransportDurationInMillis());
    }

    public Map<String, String> createHeaderMap() {
        Map<String, String> headerMap = new HashMap<>();

        String synthVal = "[\n" +
                "   1,\n" +
                "   417446,\n" +
                "   \"fd09bfa1-bd85-4f8a-9bee-8d51582f5a54\",\n" +
                "   \"77cbc5dc-327b-4542-90f0-335644134bed\",\n" +
                "   \"3e5c28ac-7cf3-4faf-ae52-ff36bc93504a\"\n" +
                "]";
        String obfuscatedSynthVal = Obfuscator.obfuscateNameUsingKey(synthVal, "anotherExampleKey");

        String synthInfoVal = "{\n" +
                "       \"version\": \"1\",\n" +
                "       \"type\": \"scheduled\",\n" +
                "       \"initiator\": \"cli\",\n" +
                "       \"attributes\": {\n" +
                "           \"example1\": \"Value1\",\n" +
                "           \"example2\": \"Value2\"\n" +
                "           }\n" +
                "}";
        String obfuscatedSynthInfoVal = Obfuscator.obfuscateNameUsingKey(synthInfoVal, "anotherExampleKey");

        headerMap.put("X-NewRelic-Synthetics-Info", obfuscatedSynthInfoVal);
        headerMap.put("X-NewRelic-Synthetics", obfuscatedSynthVal);

        return headerMap;
    }

    public InboundHeaders createInboundHeaders(Map<String, String> map) {
        Map<String, String> headerMap = createHeaderMap();

        return new InboundHeaders() {
            @Override
            public HeaderType getHeaderType() {
                return HeaderType.HTTP;
            }
            @Override
            public String getHeader(String name) {

                if (headerMap.containsKey(name)) {
                    return headerMap.get(name);
                }
                return null;
            }
        };
    }

    private Transaction createTxWithSyntheticsHeaders() throws Exception {
        Map<String, Object> configMap = createConfigMap();
        createServiceManager(configMap);
        InboundHeaders inboundHeaders = createInboundHeaders(createHeaderMap());
        Transaction tx = Transaction.getTransaction(true);
        Tracer dispatcherTracer = createDispatcherTracer(false);
        tx.getTransactionActivity().tracerStarted(dispatcherTracer);
        MockHttpRequest httpRequest = new MockHttpRequest();
        MockHttpResponse httpResponse = new MockHttpResponse();
        httpRequest.setHeader("X-NewRelic-Synthetics-Info", inboundHeaders.getHeader("X-NewRelic-Synthetics-Info"));
        httpRequest.setHeader("X-NewRelic-Synthetics", inboundHeaders.getHeader("X-NewRelic-Synthetics"));
        tx.setRequestAndResponse(httpRequest, httpResponse);
        tx.getTransactionActivity().tracerFinished(dispatcherTracer, 0);
        return tx;
    }

    @Test
    public void testInboundHeaderStateForSyntheticsInformation() throws Exception {

        Transaction tx = createTxWithSyntheticsHeaders();

        InboundHeaderState ihs = tx.getInboundHeaderState();
        assertNotNull(tx.getInboundHeaderState());
        assertEquals("77cbc5dc-327b-4542-90f0-335644134bed", tx.getInboundHeaderState().getSyntheticsJobId());
        assertEquals("fd09bfa1-bd85-4f8a-9bee-8d51582f5a54", ihs.getSyntheticsResourceId());
        assertEquals("3e5c28ac-7cf3-4faf-ae52-ff36bc93504a", ihs.getSyntheticsMonitorId());
        assertEquals(1, ihs.getSyntheticsVersion());
        assertEquals("cli", ihs.getSyntheticsInitiator());
        assertEquals("scheduled", ihs.getSyntheticsType());
        assertEquals("Value1", ihs.getSyntheticsAttrs().get("example1"));
        assertEquals("Value2", ihs.getSyntheticsAttrs().get("example2"));
    }

    @Test
    public void testTransactionIntrinsicAttrsForSyntheticsInformation() throws Exception {

        Transaction tx = createTxWithSyntheticsHeaders();

        assertNotNull(tx.getIntrinsicAttributes());
        assertEquals("77cbc5dc-327b-4542-90f0-335644134bed", tx.getIntrinsicAttributes().get("synthetics_job_id"));
        assertEquals("fd09bfa1-bd85-4f8a-9bee-8d51582f5a54", tx.getIntrinsicAttributes().get("synthetics_resource_id"));
        assertEquals("3e5c28ac-7cf3-4faf-ae52-ff36bc93504a", tx.getIntrinsicAttributes().get("synthetics_monitor_id"));
        assertEquals("cli", tx.getIntrinsicAttributes().get("synthetics_initiator"));
        assertEquals("scheduled", tx.getIntrinsicAttributes().get("synthetics_type"));
        assertEquals("Value1", tx.getIntrinsicAttributes().get("synthetics_example1"));
        assertEquals("Value2", tx.getIntrinsicAttributes().get("synthetics_example2"));
        assertEquals(1, tx.getIntrinsicAttributes().get("synthetics_version"));
    }

    private Map<String, Object> createNRDTConfigMap(boolean excludeNewRelicHeader) {
        Map<String, Object> configMap = createConfigMap();
        configMap.put(AgentConfigImpl.DISTRIBUTED_TRACING, ImmutableMap.of(
                "enabled", Boolean.TRUE,
                "exclude_newrelic_header", excludeNewRelicHeader));
        return configMap;
    }

    private boolean isTransportDurationMillisWithinAcceptableDelta(long transportDuration, long txStart, long sentTimestamp, long acceptableDelta) {
        long difference = txStart - sentTimestamp;
        long lowerBound = difference - acceptableDelta;
        long upperBound = difference + acceptableDelta;
        return transportDuration >= lowerBound && transportDuration <= upperBound;
    }

    @SuppressWarnings("StatementWithEmptyBody")
    private void sleep(int sleepInMs) {
        final long end = System.currentTimeMillis() + sleepInMs;
        while (System.currentTimeMillis() < end) {
        }
    }

    // Create a Tracer for tests that require one.
    private Tracer createDispatcherTracer(boolean createRequest) {
        Transaction tx = Transaction.getTransaction();
        ClassMethodSignature sig = new ClassMethodSignature(getClass().getName(), "dude", "()V");

        if (createRequest) {
            MockHttpRequest httpRequest = new MockHttpRequest();
            MockHttpResponse httpResponse = new MockHttpResponse();
            return new BasicRequestRootTracer(tx, sig, this, httpRequest, httpResponse);
        } else {
            return new OtherRootTracer(tx, sig, this, new SimpleMetricNameFormat("thisismyname"));
        }
    }

    private DefaultTracer createBasicTracer(String id) {
        Transaction tx = Transaction.getTransaction();
        ClassMethodSignature sig = new ClassMethodSignature(getClass().getName(), id, "()V");
        return new DefaultTracer(tx, sig, this, new SimpleMetricNameFormat("Custom/myname" + id));
    }

}
