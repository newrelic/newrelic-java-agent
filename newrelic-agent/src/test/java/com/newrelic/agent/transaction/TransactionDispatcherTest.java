/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.transaction;

import com.newrelic.agent.MetricNames;
import com.newrelic.agent.MockCoreService;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.dispatchers.Dispatcher;
import com.newrelic.agent.dispatchers.WebRequestDispatcher;
import com.newrelic.agent.metric.MetricName;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.StatsEngine;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.OtherRootTracer;
import com.newrelic.agent.tracers.metricname.SimpleMetricNameFormat;
import com.newrelic.agent.tracers.servlet.MockHttpRequest;
import com.newrelic.agent.tracers.servlet.MockHttpResponse;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TransactionDispatcherTest {

    @BeforeClass
    public static void beforeClass() throws Exception {
        MockCoreService.getMockAgentAndBootstrapTheServiceManager();
    }

    @Before
    public void before() {
        Transaction.clearTransaction();
    }

    @Test
    public void testSetDispatcherFirstWins() {
        Transaction tx = Transaction.getTransaction(true);

        Dispatcher dispatcherOne = new WebRequestDispatcher(new MockHttpRequest(), new MockHttpResponse(), tx);
        tx.setDispatcher(dispatcherOne);

        assertEquals(dispatcherOne, tx.getDispatcher());

        Dispatcher dispatcherTwo = new WebRequestDispatcher(new MockHttpRequest(), new MockHttpResponse(), tx);
        tx.setDispatcher(dispatcherTwo);
        assertEquals(dispatcherOne, tx.getDispatcher());
    }

    @Test
    public void testSetWebRequestFirstWins() {
        Transaction.clearTransaction();
        Transaction tx = Transaction.getTransaction(true);

        MockHttpRequest requestOne = new MockHttpRequest();
        tx.setWebRequest(requestOne);
        assertEquals(requestOne, tx.getDispatcher().getRequest());

        MockHttpRequest requestTwo = new MockHttpRequest();
        tx.setWebRequest(requestTwo);
        assertEquals(requestOne, tx.getDispatcher().getRequest());
    }

    @Test
    public void testSetWebResponseFirstWins() {
        Transaction tx = Transaction.getTransaction(true);

        MockHttpRequest request = new MockHttpRequest();
        tx.setWebRequest(request);

        MockHttpResponse responseOne = new MockHttpResponse();
        tx.setWebResponse(responseOne);
        assertEquals(responseOne, tx.getDispatcher().getResponse());

        MockHttpResponse responseTwo = new MockHttpResponse();
        tx.setWebResponse(responseTwo);
        assertEquals(responseOne, tx.getDispatcher().getResponse());
    }

    @Test
    public void testSetResponseOnly() {
        Transaction tx = Transaction.getTransaction(true);

        /* Start a tracer and end it so that the transaction runs the dispatcher
           transactionActivityWithResponseFinished code
         */
        OtherRootTracer rootTracer = new OtherRootTracer(tx,
                new ClassMethodSignature("", "", ""), null,
                new SimpleMetricNameFormat(""));

        tx.getTransactionActivity().tracerStarted(rootTracer);

        MockHttpResponse response = new MockHttpResponse();
        response.setResponseStatus(418);
        response.setResponseStatusMessage("tea");
        tx.setWebResponse(response);

        rootTracer.finish(null);

        assertEquals(418, tx.getStatus());
        assertEquals("tea", tx.getStatusMessage());

        // Make sure the status code was set in time for the Apdex calculation.
        StatsEngine statsEngine = ServiceFactory.getStatsService().getStatsEngineForHarvest(
                tx.getApplicationName());
        assertEquals(1, statsEngine.getApdexStats(MetricName.create(MetricNames.APDEX)).getApdexFrustrating());
    }
}
