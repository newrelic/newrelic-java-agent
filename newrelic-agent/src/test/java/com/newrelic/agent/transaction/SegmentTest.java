/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.transaction;

import com.newrelic.agent.AgentHelper;
import com.newrelic.agent.ExpirationService;
import com.newrelic.agent.HarvestService;
import com.newrelic.agent.MockCoreService;
import com.newrelic.agent.MockHarvestService;
import com.newrelic.agent.MockRPMServiceManager;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.ThreadService;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionApiImpl;
import com.newrelic.agent.TransactionAsyncUtility;
import com.newrelic.agent.TransactionData;
import com.newrelic.agent.TransactionListener;
import com.newrelic.agent.TransactionService;
import com.newrelic.agent.attributes.AttributesService;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.ConfigServiceFactory;
import com.newrelic.agent.config.TransactionTracerConfigImpl;
import com.newrelic.agent.environment.EnvironmentService;
import com.newrelic.agent.environment.EnvironmentServiceImpl;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.analytics.TransactionDataToDistributedTraceIntrinsics;
import com.newrelic.agent.service.analytics.TransactionEventsService;
import com.newrelic.agent.service.async.AsyncTransactionService;
import com.newrelic.agent.sql.SqlTraceService;
import com.newrelic.agent.sql.SqlTraceServiceImpl;
import com.newrelic.agent.stats.StatsService;
import com.newrelic.agent.stats.StatsServiceImpl;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.agent.trace.TransactionTraceService;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracing.DistributedTraceServiceImpl;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class SegmentTest implements TransactionListener {

    private static InlineExpirationService inlineExpirationService;

    private TransactionData data;
    private TransactionStats stats;

    // Wrap the ExpirationService so we can know when a segment has fully ended
    public static class InlineExpirationService extends ExpirationService {

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
    }

    @Before
    public void before() throws Exception {
        data = null;
        stats = null;
        ServiceFactory.getTransactionService().addTransactionListener(this);
    }

    @After
    public void after() {
        ServiceFactory.getTransactionService().removeTransactionListener(this);
    }

    private static Map<String, Object> createConfigMap() {
        Map<String, Object> map = new HashMap<>();
        map.put(AgentConfigImpl.APP_NAME, "Unit Test");
        map.put(AgentConfigImpl.THREAD_CPU_TIME_ENABLED, Boolean.TRUE);
        Map<String, Object> ttMap = new HashMap<>();
        ttMap.put(TransactionTracerConfigImpl.GC_TIME_ENABLED, Boolean.TRUE);
        ttMap.put(TransactionTracerConfigImpl.TRANSACTION_THRESHOLD, 0.0f);
        map.put(AgentConfigImpl.TRANSACTION_TRACER, ttMap);
        return map;
    }

    @Override
    public void dispatcherTransactionFinished(TransactionData transactionData, TransactionStats transactionStats) {
        data = transactionData;
        stats = transactionStats;
    }

    @Test
    public void testBasicSegment() throws InterruptedException {
        Transaction.clearTransaction();
        Transaction tx = Transaction.getTransaction();
        Tracer rootTracer = TransactionAsyncUtility.createDispatcherTracer(this, "hi");
        tx.getTransactionActivity().tracerStarted(rootTracer);

        com.newrelic.api.agent.Segment segment = TransactionApiImpl.INSTANCE.startSegment("My Segment");
        segment.end();

        rootTracer.finish(Opcodes.RETURN, 0);
        Assert.assertNotNull(data);
        Assert.assertNotNull(stats);

        Assert.assertEquals(2, data.getTransactionActivities().size());
        Collection<Tracer> tracers = new ArrayList(data.getTracers());
        tracers.add(data.getRootTracer());

        Assert.assertEquals(2, tracers.size());

        for (Tracer current : tracers) {
            String segmentName = current.getTransactionSegmentName();
            String metricName = current.getTransactionSegmentName();
            Assert.assertTrue("Unexpected segment name: " + segmentName,
                    segmentName.equals("RequestDispatcher/com.newrelic.agent.transaction.SegmentTest/hi")
                            || segmentName.equals("Custom/My Segment"));

            Assert.assertTrue("Unexpected metric name: " + metricName,
                    metricName.equals("RequestDispatcher/com.newrelic.agent.transaction.SegmentTest/hi")
                            || metricName.equals("Custom/My Segment"));
        }
    }

    @Test
    public void testBasicSegmentFinishAfterTracer() throws InterruptedException {
        Transaction.clearTransaction();
        Transaction tx = Transaction.getTransaction();
        Tracer rootTracer = TransactionAsyncUtility.createDispatcherTracer(this, "hi");
        tx.getTransactionActivity().tracerStarted(rootTracer);

        com.newrelic.api.agent.Segment segment = TransactionApiImpl.INSTANCE.startSegment(null);

        rootTracer.finish(Opcodes.RETURN, 0);
        segment.end();

        Assert.assertNotNull(data);
        Assert.assertNotNull(stats);

        Assert.assertEquals(2, data.getTransactionActivities().size());
        Collection<Tracer> tracers = new ArrayList(data.getTracers());
        tracers.add(data.getRootTracer());

        Assert.assertEquals(2, tracers.size());

        for (Tracer current : tracers) {
            String segmentName = current.getTransactionSegmentName();
            String metricName = current.getTransactionSegmentName();
            Assert.assertTrue("Unexpected segment name: " + segmentName,
                    segmentName.equals("RequestDispatcher/com.newrelic.agent.transaction.SegmentTest/hi")
                            || segmentName.equals("Custom/Unnamed Segment"));

            Assert.assertTrue("Unexpected metric name: " + metricName,
                    metricName.equals("RequestDispatcher/com.newrelic.agent.transaction.SegmentTest/hi")
                            || metricName.equals("Custom/Unnamed Segment"));
        }
    }

    private static void createServiceManager(Map<String, Object> map, ExpirationService expirationService) throws Exception {
        MockServiceManager serviceManager = new MockServiceManager();
        ServiceFactory.setServiceManager(serviceManager);
        serviceManager.start();

        ThreadService threadService = new ThreadService();
        serviceManager.setThreadService(threadService);

        AgentConfig agentConfig = AgentHelper.createAgentConfig(true, map, Collections.<String, Object>emptyMap());

        ConfigService configService = ConfigServiceFactory.createConfigService(agentConfig, map);
        serviceManager.setConfigService(configService);

        StatsService statsService = new StatsServiceImpl();
        serviceManager.setStatsService(statsService);

        MockCoreService agent = new MockCoreService();
        serviceManager.setCoreService(agent);

        HarvestService harvestService = new MockHarvestService();
        serviceManager.setHarvestService(harvestService);

        AsyncTransactionService asyncTxService = new AsyncTransactionService();
        serviceManager.setAsyncTransactionService(asyncTxService);

        TransactionService transactionService = new TransactionService();
        serviceManager.setTransactionService(transactionService);

        EnvironmentService envService = new EnvironmentServiceImpl();
        serviceManager.setEnvironmentService(envService);

        TransactionTraceService transactionTraceService = new TransactionTraceService();
        serviceManager.setTransactionTraceService(transactionTraceService);

        SqlTraceService sqlTraceService = new SqlTraceServiceImpl();
        serviceManager.setSqlTraceService(sqlTraceService);

        serviceManager.setAttributesService(new AttributesService());

        DistributedTraceServiceImpl distributedTraceService = new DistributedTraceServiceImpl();
        serviceManager.setDistributedTraceService(distributedTraceService);
        TransactionDataToDistributedTraceIntrinsics transactionDataToDistributedTraceIntrinsics = new TransactionDataToDistributedTraceIntrinsics(distributedTraceService);
        serviceManager.setTransactionEventsService(new TransactionEventsService(transactionDataToDistributedTraceIntrinsics));

        MockRPMServiceManager rpmServiceManager = new MockRPMServiceManager();
        serviceManager.setRPMServiceManager(rpmServiceManager);

        serviceManager.setExpirationService(expirationService);
    }

}
