/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package test.newrelic.test.agent;

import javax.servlet.ServletException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.newrelic.agent.AgentHelper;
import com.newrelic.agent.MetricNames;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionData;
import com.newrelic.agent.TransactionDataList;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.api.agent.Trace;

public class ErrorsTest {
    private TransactionDataList transactionList = new TransactionDataList();

    @Before
    public void setup() {
        ServiceFactory.getTransactionService().addTransactionListener(transactionList);
        transactionList.clear();
    }

    @After
    public void teardown() {
        transactionList.clear();
        ServiceFactory.getTransactionService().removeTransactionListener(transactionList);
        Transaction.clearTransaction();
    }

    @Test
    public void test404() throws Exception {
        try {
            AgentHelper.invokeServlet(null, "", "Test", "bad_request");
            Assert.fail();
        } catch (ServletException e) {
        }

        Assert.assertEquals(1, transactionList.size());
        TransactionData transactionData = transactionList.get(0);

        Assert.assertEquals(MetricNames.NORMALIZED_URI_WEB_TRANSACTION + "/404/*", transactionData.getBlameMetricName());
    }

    @Test
    public void errorWithUnfamiliarUrl() throws Exception {
        error(500);

        Assert.assertEquals(transactionList.toString(), 1, transactionList.size());
        TransactionData transactionData = transactionList.get(0);

        Assert.assertEquals(MetricNames.NORMALIZED_URI_WEB_TRANSACTION + "/500/*", transactionData.getBlameMetricName());
    }

    @Trace(dispatcher = true)
    private void error(int statusCode) {
        AgentBridge.getAgent().getTransaction().convertToWebTransaction();
        AgentBridge.getAgent().getTransaction().getWebResponse().setStatus(statusCode);
    }
}
