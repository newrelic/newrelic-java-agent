/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.transaction;

import java.util.Collections;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.newrelic.agent.HarvestService;
import com.newrelic.agent.MockHarvestService;
import com.newrelic.agent.MockRPMServiceManager;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.ThreadService;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionService;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.ConfigServiceFactory;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.sql.SqlTraceService;
import com.newrelic.agent.sql.SqlTraceServiceImpl;
import com.newrelic.agent.trace.TransactionTraceService;
import com.newrelic.agent.bridge.TransactionNamePriority;

public class HigherPriorityTransactionNamingPolicyTest {

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

        AgentConfig config = AgentConfigImpl.createAgentConfig(Collections.EMPTY_MAP);
        ConfigService configService = ConfigServiceFactory.createConfigService(config, Collections.EMPTY_MAP);
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
    public void canSetTransactionNameLowerPriority() {
        Transaction tx = Transaction.getTransaction();
        PriorityTransactionName expected = PriorityTransactionName.create(null, null,
                TransactionNamePriority.CUSTOM_HIGH);
        TransactionNamingPolicy policy = TransactionNamingPolicy.getHigherPriorityTransactionNamingPolicy();
        policy.setTransactionName(tx, expected.getName(), "RequestAttribute", expected.getPriority());
        Assert.assertFalse(policy.canSetTransactionName(tx, TransactionNamePriority.FRAMEWORK));
    }

    @Test
    public void canSetTransactionNameEqualPriority() {
        TransactionNamingPolicy policy = TransactionNamingPolicy.getHigherPriorityTransactionNamingPolicy();
        Transaction tx = Transaction.getTransaction();
        Assert.assertFalse(policy.canSetTransactionName(tx, TransactionNamePriority.NONE));
    }

    @Test
    public void canSetTransactionNameHigherPriority() {
        TransactionNamingPolicy policy = TransactionNamingPolicy.getHigherPriorityTransactionNamingPolicy();
        Transaction tx = Transaction.getTransaction();
        Assert.assertTrue(policy.canSetTransactionName(tx, TransactionNamePriority.FILTER_NAME));
    }

    @Test
    public void setTransactionNameHigherPriority() {
        PriorityTransactionName expected = PriorityTransactionName.create("OtherTransaction/Filter/MyTransactionName",
                null, TransactionNamePriority.FILTER_NAME);
        TransactionNamingPolicy policy = TransactionNamingPolicy.getHigherPriorityTransactionNamingPolicy();
        Transaction tx = Transaction.getTransaction();
        policy.setTransactionName(tx, expected.getName(), "Filter", expected.getPriority());
        PriorityTransactionName actual = tx.getPriorityTransactionName();
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void setTransactionNameEqualPriority() {
        PriorityTransactionName expected = PriorityTransactionName.create("OtherTransaction/Filter/MyTransactionName",
                null, TransactionNamePriority.FILTER_NAME);
        TransactionNamingPolicy policy = TransactionNamingPolicy.getHigherPriorityTransactionNamingPolicy();
        Transaction tx = Transaction.getTransaction();
        policy.setTransactionName(tx, expected.getName(), "Filter", expected.getPriority());
        PriorityTransactionName actual = tx.getPriorityTransactionName();
        Assert.assertEquals(expected, actual);

        policy.setTransactionName(tx, "MyTransactionName2", "Filter", expected.getPriority());
        actual = tx.getPriorityTransactionName();
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void setTransactionNameLowerPriority() {
        Transaction tx = Transaction.getTransaction();
        PriorityTransactionName expected = PriorityTransactionName.create(null, null,
                TransactionNamePriority.CUSTOM_HIGH);
        TransactionNamingPolicy policy = TransactionNamingPolicy.getHigherPriorityTransactionNamingPolicy();
        policy.setTransactionName(tx, expected.getName(), "RequestAttribute", expected.getPriority());
        policy.setTransactionName(tx, null, null, TransactionNamePriority.NONE);
        PriorityTransactionName actual = tx.getPriorityTransactionName();
        Assert.assertEquals(expected, actual);
    }

}
