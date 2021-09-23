/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.transaction;

import com.google.common.collect.ImmutableMap;
import com.newrelic.agent.HarvestService;
import com.newrelic.agent.MockHarvestService;
import com.newrelic.agent.MockRPMServiceManager;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.ThreadService;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionService;
import com.newrelic.agent.bridge.TransactionNamePriority;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.ConfigServiceFactory;
import com.newrelic.agent.config.DistributedTracingConfig;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.sql.SqlTraceService;
import com.newrelic.agent.sql.SqlTraceServiceImpl;
import com.newrelic.agent.trace.TransactionTraceService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class SameOrHigherPriorityTransactionNamingPolicyTest {

    @Before
    public void before() throws Exception {

        createServiceManager();
        Transaction.clearTransaction();
    }

    private MockServiceManager createServiceManager() throws Exception {

        MockServiceManager serviceManager = new MockServiceManager();
        ServiceFactory.setServiceManager(serviceManager);

        ThreadService threadService = new ThreadService();
        serviceManager.setThreadService(threadService);

        ImmutableMap<String, Object> distributedTracingSettings = ImmutableMap.<String, Object>builder()
                .put(DistributedTracingConfig.ENABLED, Boolean.FALSE)
                .build();

        Map<String, Object> settings = new HashMap<>();
        settings.put(AgentConfigImpl.DISTRIBUTED_TRACING, distributedTracingSettings);

        ConfigService configService = ConfigServiceFactory.createConfigService(
                AgentConfigImpl.createAgentConfig(settings), settings);
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

        return serviceManager;
    }

    @Test
    public void canSetTransactionNameHigherPriority() {
        TransactionNamingPolicy policy = TransactionNamingPolicy.getSameOrHigherPriorityTransactionNamingPolicy();
        Transaction tx = Transaction.getTransaction();
        Assert.assertTrue(policy.canSetTransactionName(tx, TransactionNamePriority.FILTER_NAME));
    }

    @Test
    public void canSetTransactionNameEqualPriority() {
        TransactionNamingPolicy policy = TransactionNamingPolicy.getSameOrHigherPriorityTransactionNamingPolicy();
        Transaction tx = Transaction.getTransaction();
        Assert.assertTrue(policy.canSetTransactionName(tx, TransactionNamePriority.NONE));
    }

    @Test
    public void canSetTransactionNameLowerPriority() {
        Transaction tx = Transaction.getTransaction();
        PriorityTransactionName expected = PriorityTransactionName.create(null, null,
                TransactionNamePriority.CUSTOM_HIGH);
        TransactionNamingPolicy policy = TransactionNamingPolicy.getSameOrHigherPriorityTransactionNamingPolicy();
        policy.setTransactionName(tx, expected.getName(), "RequestAttribute", expected.getPriority());
        Assert.assertFalse(policy.canSetTransactionName(tx, TransactionNamePriority.FRAMEWORK));
    }

    @Test
    public void setTransactionNameHigherPriority() {
        PriorityTransactionName expected = PriorityTransactionName.create("OtherTransaction/Filter/MyTransactionName",
                null, TransactionNamePriority.FILTER_NAME);
        TransactionNamingPolicy policy = TransactionNamingPolicy.getSameOrHigherPriorityTransactionNamingPolicy();
        Transaction tx = Transaction.getTransaction();
        policy.setTransactionName(tx, expected.getName(), "RequestAttribute", expected.getPriority());
        PriorityTransactionName actual = tx.getPriorityTransactionName();
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void setTransactionNameEqualPriority() {
        PriorityTransactionName expected = PriorityTransactionName.create("OtherTransaction/Filter/MyTransactionName",
                null, TransactionNamePriority.FILTER_NAME);
        TransactionNamingPolicy policy = TransactionNamingPolicy.getSameOrHigherPriorityTransactionNamingPolicy();
        Transaction tx = Transaction.getTransaction();
        policy.setTransactionName(tx, expected.getName(), "RequestAttribute", expected.getPriority());
        PriorityTransactionName actual = tx.getPriorityTransactionName();
        Assert.assertEquals(expected, actual);

        expected = PriorityTransactionName.create("OtherTransaction/Filter/MyTransactionName2", null,
                TransactionNamePriority.FILTER_NAME);
        policy.setTransactionName(tx, expected.getName(), "RequestAttribute", expected.getPriority());
        actual = tx.getPriorityTransactionName();
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void setTransactionNameLowerPriority() {
        Transaction tx = Transaction.getTransaction();
        PriorityTransactionName expected = PriorityTransactionName.create(null, null,
                TransactionNamePriority.CUSTOM_HIGH);
        TransactionNamingPolicy policy = TransactionNamingPolicy.getSameOrHigherPriorityTransactionNamingPolicy();
        policy.setTransactionName(tx, expected.getName(), "RequestAttribute", expected.getPriority());
        policy.setTransactionName(tx, null, null, TransactionNamePriority.NONE);
        PriorityTransactionName actual = tx.getPriorityTransactionName();
        Assert.assertEquals(expected, actual);
    }

}
