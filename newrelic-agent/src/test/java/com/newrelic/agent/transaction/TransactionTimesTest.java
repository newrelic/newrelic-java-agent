/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.transaction;

import com.newrelic.agent.AgentHelper;
import com.newrelic.agent.HarvestService;
import com.newrelic.agent.MockCoreService;
import com.newrelic.agent.MockHarvestService;
import com.newrelic.agent.MockRPMServiceManager;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.ThreadService;
import com.newrelic.agent.Transaction;
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
import com.newrelic.agent.sql.SqlTraceService;
import com.newrelic.agent.sql.SqlTraceServiceImpl;
import com.newrelic.agent.stats.SimpleStatsEngine;
import com.newrelic.agent.stats.StatsServiceImpl;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.agent.trace.TransactionTraceService;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.agent.tracers.OtherRootTracer;
import com.newrelic.agent.tracers.metricname.OtherTransSimpleMetricNameFormat;
import com.newrelic.agent.tracing.DistributedTraceServiceImpl;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class TransactionTimesTest implements TransactionListener {

    private volatile TransactionStats stats;

    @Override
    public void dispatcherTransactionFinished(TransactionData transactionData, TransactionStats transactionStats) {
        stats = transactionStats;

    }

    @Before
    public void beforeTest() throws Exception {
        createServiceManager(createConfigMap());
        ServiceFactory.getTransactionService().addTransactionListener(this);
        Transaction.clearTransaction();
    }

    @After
    public void afterTest() {
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

    private static void createServiceManager(Map<String, Object> map) throws Exception {
        MockServiceManager serviceManager = new MockServiceManager();
        ServiceFactory.setServiceManager(serviceManager);
        serviceManager.start();

        ThreadService threadService = new ThreadService();
        serviceManager.setThreadService(threadService);

        AgentConfig agentConfig = AgentHelper.createAgentConfig(true, map, Collections.<String, Object>emptyMap());

        ConfigService configService = ConfigServiceFactory.createConfigService(agentConfig, map);
        serviceManager.setConfigService(configService);

        MockCoreService agent = new MockCoreService();
        serviceManager.setCoreService(agent);

        HarvestService harvestService = new MockHarvestService();
        serviceManager.setHarvestService(harvestService);

        EnvironmentService envService = new EnvironmentServiceImpl();
        serviceManager.setEnvironmentService(envService);

        TransactionService transactionService = new TransactionService();
        serviceManager.setTransactionService(transactionService);

        StatsServiceImpl statsService = new StatsServiceImpl();
        serviceManager.setStatsService(statsService);

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
    }

    @Test
    public void testTransactionTimes() throws InterruptedException {
        Transaction tx = Transaction.getTransaction();
        OtherRootTracer root = createDispatcherTracer();
        tx.getTransactionActivity().tracerStarted(root);
        DefaultTracer tracer1 = createDefaultTraer("hello1");
        tx.getTransactionActivity().tracerStarted(tracer1);
        DefaultTracer tracer2 = createDefaultTraer("hello2");
        tx.getTransactionActivity().tracerStarted(tracer2);
        Thread.sleep(10);
        tracer2.finish(0, null);
        tracer1.finish(0, null);
        root.finish(0, null);

        Assert.assertNotNull(stats);
        SimpleStatsEngine unscoped = stats.getUnscopedStats();
        Assert.assertNotNull(unscoped.getOrCreateResponseTimeStats("OtherTransaction/all"));
        Assert.assertNotNull(unscoped.getOrCreateResponseTimeStats("OtherTransactionTotalTime/all"));
        Assert.assertNotNull(unscoped.getOrCreateResponseTimeStats("OtherTransaction/myMetricName"));
        Assert.assertNotNull(unscoped.getOrCreateResponseTimeStats("OtherTransactionTotalTime/myMetricName"));

        Assert.assertEquals(1, unscoped.getOrCreateResponseTimeStats("OtherTransaction/all").getCallCount());
        Assert.assertEquals(1, unscoped.getOrCreateResponseTimeStats("OtherTransactionTotalTime").getCallCount());
        Assert.assertEquals(1, unscoped.getOrCreateResponseTimeStats("OtherTransaction/myMetricName").getCallCount());
        Assert.assertEquals(1, unscoped.getOrCreateResponseTimeStats("OtherTransactionTotalTime/myMetricName").getCallCount());

        SimpleStatsEngine scoped = stats.getScopedStats();
        Assert.assertNotNull(scoped.getOrCreateResponseTimeStats("Java/com.newrelic.agent.transaction.TransactionTimesTest/dude"));
        Assert.assertEquals(1, scoped.getOrCreateResponseTimeStats(
                "Java/com.newrelic.agent.transaction.TransactionTimesTest/dude").getCallCount());

        Assert.assertEquals(root.getDuration(), tx.getTransactionTimer().getTotalSumTimeInNanos());
        Assert.assertEquals(root.getDuration(), tx.getTransactionTimer().getResponseTimeInNanos());
        Assert.assertEquals(1, tx.getFinishedChildren().size());

    }

    private DefaultTracer createDefaultTraer(String methodName) {
        Transaction tx = Transaction.getTransaction();
        ClassMethodSignature sig = new ClassMethodSignature(getClass().getName(), methodName, "()V");
        return new DefaultTracer(tx, sig, this);
    }

    // Create a Tracer for tests that require one.
    private OtherRootTracer createDispatcherTracer() {
        Transaction tx = Transaction.getTransaction();
        ClassMethodSignature sig = new ClassMethodSignature(getClass().getName(), "dude", "()V");
        return new OtherRootTracer(tx, sig, this, new OtherTransSimpleMetricNameFormat("myMetricName"));
    }
}
