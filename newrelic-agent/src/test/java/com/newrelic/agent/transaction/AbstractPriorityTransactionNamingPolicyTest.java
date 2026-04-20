/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.transaction;

import com.google.common.collect.ImmutableMap;
import com.newrelic.agent.HarvestService;
import com.newrelic.agent.MetricNames;
import com.newrelic.agent.MockHarvestService;
import com.newrelic.agent.MockRPMServiceManager;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.ThreadService;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionService;
import com.newrelic.agent.attributes.AttributesService;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.TransactionNamePriority;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.ConfigServiceFactory;
import com.newrelic.agent.config.DistributedTracingConfig;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.sql.SqlTraceService;
import com.newrelic.agent.sql.SqlTraceServiceImpl;
import com.newrelic.agent.trace.TransactionTraceService;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.OtherRootTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.metricname.MetricNameFormat;
import com.newrelic.agent.tracers.metricname.SimpleMetricNameFormat;
import com.newrelic.agent.tracers.servlet.BasicRequestRootTracer;
import com.newrelic.agent.tracers.servlet.MockHttpRequest;
import com.newrelic.agent.tracers.servlet.MockHttpResponse;
import com.newrelic.agent.util.AgentCollectionFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class AbstractPriorityTransactionNamingPolicyTest {

    @Before
    public void before() throws Exception {

        createServiceManager();
        Transaction.clearTransaction();
    }

    private MockServiceManager createServiceManager() throws Exception {
        // Initialize AgentBridge with real Caffeine factory for tests
        AgentBridge.collectionFactory = new AgentCollectionFactory();

        MockServiceManager serviceManager = new MockServiceManager();
        ServiceFactory.setServiceManager(serviceManager);

        ThreadService threadService = new ThreadService();
        serviceManager.setThreadService(threadService);

        ImmutableMap<String, Object> distributedTracingSettings = ImmutableMap.<String, Object>builder()
                .put(DistributedTracingConfig.ENABLED, Boolean.FALSE)
                .build();

        Map<String, Object> settings = new HashMap<>();
        settings.put(AgentConfigImpl.DISTRIBUTED_TRACING, distributedTracingSettings);

        AgentConfig config = AgentConfigImpl.createAgentConfig(settings);
        ConfigService configService = ConfigServiceFactory.createConfigService(config, settings);
        serviceManager.setConfigService(configService);

        HarvestService harvestService = new MockHarvestService();
        serviceManager.setHarvestService(harvestService);

        TransactionService transactionService = new TransactionService();
        serviceManager.setTransactionService(transactionService);

        TransactionTraceService transactionTraceService = new TransactionTraceService();
        serviceManager.setTransactionTraceService(transactionTraceService);

        SqlTraceService sqlTraceService = new SqlTraceServiceImpl();
        serviceManager.setSqlTraceService(sqlTraceService);

        MockRPMServiceManager rpmServiceManager = new MockRPMServiceManager();
        serviceManager.setRPMServiceManager(rpmServiceManager);

        serviceManager.setAttributesService(new AttributesService());

        return serviceManager;
    }

    private void startWebTransaction() throws Exception {
        Transaction tx = Transaction.getTransaction();
        MockHttpRequest httpRequest = new MockHttpRequest();
        httpRequest.setRequestURI("");
        MockHttpResponse httpResponse = new MockHttpResponse();
        ClassMethodSignature sig = new ClassMethodSignature(getClass().getName(), "dude", "()V");
        Tracer tracer = new BasicRequestRootTracer(tx, sig, this, httpRequest, httpResponse);
        tx.getTransactionActivity().tracerStarted(tracer);
    }

    private void startOtherTransaction() throws Exception {
        Transaction tx = Transaction.getTransaction();
        ClassMethodSignature sig = new ClassMethodSignature("", "", "");
        MetricNameFormat format = new SimpleMetricNameFormat("");
        Tracer tracer = new OtherRootTracer(tx, sig, this, format);
        tx.getTransactionActivity().tracerStarted(tracer);
    }

    @Test
    public void nullCategory() throws Exception {
        startWebTransaction();
        String name = "MyTransactionName";
        Transaction tx = Transaction.getTransaction();
        PriorityTransactionName expected = PriorityTransactionName.create(name, null,
                TransactionNamePriority.CUSTOM_HIGH);
        TransactionNamingPolicy policy = TransactionNamingPolicy.getHigherPriorityTransactionNamingPolicy();
        policy.setTransactionName(tx, name, null, expected.getPriority());
        PriorityTransactionName actual = tx.getPriorityTransactionName();
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void emptyCategory() throws Exception {
        startWebTransaction();
        String name = "MyTransactionName";
        Transaction tx = Transaction.getTransaction();
        TransactionNamingPolicy policy = TransactionNamingPolicy.getHigherPriorityTransactionNamingPolicy();
        policy.setTransactionName(tx, name, "", TransactionNamePriority.FRAMEWORK_HIGH);
        PriorityTransactionName actual = tx.getPriorityTransactionName();
        Assert.assertEquals("WebTransaction/MyTransactionName", actual.getName());
    }

    @Test
    public void nullTransactionName() throws Exception {
        startWebTransaction();
        String name = null;
        Transaction tx = Transaction.getTransaction();
        PriorityTransactionName expected = PriorityTransactionName.create(name, null,
                TransactionNamePriority.CUSTOM_HIGH);
        TransactionNamingPolicy policy = TransactionNamingPolicy.getHigherPriorityTransactionNamingPolicy();
        policy.setTransactionName(tx, name, "RequestAttribute", expected.getPriority());
        PriorityTransactionName actual = tx.getPriorityTransactionName();
        Assert.assertEquals(expected, actual);
    }

    /*
     * @Test public void emptyTransactionName() throws Exception { startWebTransaction(); String name = ""; Transaction
     * tx = Transaction.getTransaction(); PriorityTransactionName expected =
     * PriorityTransactionName.create(ServletRequestPointCut.REQUEST_ATTRIBUTE_CATEGORY, name,
     * TransactionNamePriority.CUSTOM_HIGH); TransactionNamingPolicy policy =
     * HigherPriorityTransactionNamingPolicy.getInstance(); policy.setTransactionName(tx, name,
     * ServletRequestPointCut.REQUEST_ATTRIBUTE_CATEGORY, expected.getPriority()); PriorityTransactionName actual =
     * tx.getPriorityTransactionName(); Assert.assertEquals(expected, actual); }
     */

    @Test
    public void alreadyNormalized() throws Exception {
        startWebTransaction();
        String name = MetricNames.URI_WEB_TRANSACTION + "/MyTransactionName";
        Transaction tx = Transaction.getTransaction();
        PriorityTransactionName expected = PriorityTransactionName.create(name, null,
                TransactionNamePriority.CUSTOM_HIGH);
        TransactionNamingPolicy policy = TransactionNamingPolicy.getHigherPriorityTransactionNamingPolicy();
        policy.setTransactionName(tx, name, "Uri", expected.getPriority());
        PriorityTransactionName actual = tx.getPriorityTransactionName();
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void forwardSlash() throws Exception {
        startWebTransaction();
        String name = "/MyTransactionName";
        Transaction tx = Transaction.getTransaction();
        PriorityTransactionName expected = PriorityTransactionName.create(MetricNames.URI_WEB_TRANSACTION + name, null,
                TransactionNamePriority.CUSTOM_HIGH);
        TransactionNamingPolicy policy = TransactionNamingPolicy.getHigherPriorityTransactionNamingPolicy();
        policy.setTransactionName(tx, name, "Uri", expected.getPriority());
        PriorityTransactionName actual = tx.getPriorityTransactionName();
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void noForwardSlash() throws Exception {
        startWebTransaction();
        String name = "MyTransactionName";
        Transaction tx = Transaction.getTransaction();
        PriorityTransactionName expected = PriorityTransactionName.create(MetricNames.URI_WEB_TRANSACTION + "/" + name,
                null, TransactionNamePriority.CUSTOM_HIGH);
        TransactionNamingPolicy policy = TransactionNamingPolicy.getHigherPriorityTransactionNamingPolicy();
        policy.setTransactionName(tx, name, "Uri", expected.getPriority());
        PriorityTransactionName actual = tx.getPriorityTransactionName();
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void forwardSlashOther() throws Exception {
        startOtherTransaction();
        String name = "/MyTransactionName";
        Transaction tx = Transaction.getTransaction();
        PriorityTransactionName expected = PriorityTransactionName.create(MetricNames.OTHER_TRANSACTION_CUSTOM + name,
                null, TransactionNamePriority.CUSTOM_HIGH);
        TransactionNamingPolicy policy = TransactionNamingPolicy.getHigherPriorityTransactionNamingPolicy();
        policy.setTransactionName(tx, name, "Custom", expected.getPriority());
        PriorityTransactionName actual = tx.getPriorityTransactionName();
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void noForwardSlashOther() throws Exception {
        startOtherTransaction();
        String name = "MyTransactionName";
        Transaction tx = Transaction.getTransaction();
        PriorityTransactionName expected = PriorityTransactionName.create(MetricNames.OTHER_TRANSACTION_CUSTOM + "/"
                + name, null, TransactionNamePriority.CUSTOM_HIGH);
        TransactionNamingPolicy policy = TransactionNamingPolicy.getHigherPriorityTransactionNamingPolicy();
        policy.setTransactionName(tx, name, "Custom", expected.getPriority());
        PriorityTransactionName actual = tx.getPriorityTransactionName();
        Assert.assertEquals(expected, actual);
    }

}
