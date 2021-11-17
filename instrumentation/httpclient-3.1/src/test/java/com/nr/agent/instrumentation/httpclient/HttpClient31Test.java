/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.httpclient;

import com.newrelic.agent.introspec.CatHelper;
import com.newrelic.agent.introspec.ExternalRequest;
import com.newrelic.agent.introspec.HttpTestServer;
import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.agent.introspec.MetricsHelper;
import com.newrelic.agent.introspec.TracedMetricData;
import com.newrelic.agent.introspec.TransactionEvent;
import com.newrelic.agent.introspec.internal.HttpServerLocator;
import com.newrelic.agent.introspec.internal.HttpServerRule;
import com.newrelic.api.agent.Trace;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "org.apache.commons.httpclient" })
public class HttpClient31Test {
    @Rule
    public HttpServerRule server = new HttpServerRule();

    List<HttpMethod> stuffToClose = new ArrayList<>(10);

    /**
     * Calling releaseConnection() creates an additional scoped metric. We want to keep this out of our asserts so we
     * close outside of the transaction.
     *
     * See JAVA-1996
     */
    @After
    public void cleanup() {
        for (HttpMethod method : stuffToClose) {
            method.releaseConnection();
        }
        stuffToClose.clear();
    }

    @Test
    public void testError() throws Exception {
        final String host2 = "www.notarealhostbrosef.bro";
        try {
            httpClientExternal("http://" + host2);
            Assert.fail("Host should not be reachable: " + host2);
        } catch (UnknownHostException e) {
        }

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount());
        final String txTwo = introspector.getTransactionNames().iterator().next();
        // no metrics for unknown hosts in httpclient3
        Assert.assertEquals(0, MetricsHelper.getScopedMetricCount(txTwo, "External/Unknown/CommonsHttp"));
        Assert.assertEquals(0, MetricsHelper.getUnscopedMetricCount("External/Unknown/CommonsHttp"));

        // Unknown hosts generate no external rollups
        Assert.assertEquals(0, MetricsHelper.getUnscopedMetricCount("External/allOther"));
        Assert.assertEquals(0, MetricsHelper.getUnscopedMetricCount("External/all"));
    }

    @Test
    public void testExternal() throws Exception {
        httpClientExternal("http://localhost:" + server.getEndPoint().getPort());

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(2, introspector.getFinishedTransactionCount());

        String txOne = "OtherTransaction/Custom/com.nr.agent.instrumentation.httpclient.HttpClient31Test/httpClientExternal";
        Assert.assertEquals(1, MetricsHelper.getScopedMetricCount(txOne, "External/localhost/CommonsHttp/execute"));
        Assert.assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/localhost/CommonsHttp/execute"));

        // external rollups
        Assert.assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/localhost/all"));
        Assert.assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/allOther"));
        Assert.assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/all"));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(txOne);
        Assert.assertEquals(1, transactionEvents.size());
        TransactionEvent transactionEvent = transactionEvents.iterator().next();
        Assert.assertEquals(1, transactionEvent.getExternalCallCount());
        Assert.assertTrue(transactionEvent.getExternalDurationInSec() > 0);

        Collection<ExternalRequest> externalRequests = introspector.getExternalRequests(txOne);
        Assert.assertEquals(1, externalRequests.size());
        ExternalRequest externalRequest = externalRequests.iterator().next();
        Assert.assertEquals(1, externalRequest.getCount());
        Assert.assertEquals("localhost", externalRequest.getHostname());
        Assert.assertEquals("CommonsHttp", externalRequest.getLibrary());
        Assert.assertEquals("execute", externalRequest.getOperation());
        Assert.assertEquals(Integer.valueOf(200), externalRequest.getStatusCode());
        Assert.assertEquals("OK", externalRequest.getStatusText());
    }

    @Test
    public void testRollups() throws Exception {
        // manually set host and port here in order to get 2 unique endpoints
        httpClientExternal("http://localhost:" + server.getEndPoint().getPort());
        httpClientExternal("http://localhost:" + server.getEndPoint().getPort());
        httpClientExternal("http://127.0.0.1:" + server.getEndPoint().getPort());

        // generates one scoped metric
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(6, introspector.getFinishedTransactionCount());

        // host rollups
        Assert.assertEquals(2, MetricsHelper.getUnscopedMetricCount("External/localhost/CommonsHttp/execute"));
        Assert.assertEquals(2, MetricsHelper.getUnscopedMetricCount("External/localhost/all"));

        Assert.assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/127.0.0.1/CommonsHttp/execute"));
        Assert.assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/127.0.0.1/all"));

        // external rollups
        Assert.assertEquals(3, MetricsHelper.getUnscopedMetricCount("External/allOther"));
        Assert.assertEquals(3, MetricsHelper.getUnscopedMetricCount("External/all"));
    }

    /**
     * Start a background transaction, make an external request using httpclient, then finish.
     */
    @Trace(dispatcher = true)
    private int httpClientExternal(String host, boolean doCat) throws IOException {
        HttpClient httpclient = new HttpClient();
        GetMethod httpget = null;
        try {
            httpget = new GetMethod(host);
            httpget.setRequestHeader(HttpTestServer.DO_CAT, String.valueOf(doCat));
            return httpclient.executeMethod(httpget);
        } finally {
            if (null != httpget) {
                stuffToClose.add(httpget);
            }
        }
    }

    private int httpClientExternal(String host) throws IOException {
        return httpClientExternal(host, false);
    }

    @Test
    public void testCat() throws Exception {
        URI endpoint = server.getEndPoint();
        String host = endpoint.getHost();
        int status = httpClientExternal(endpoint.toURL().toString(), true);
        assertEquals(200, status);

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(2, introspector.getFinishedTransactionCount());

        String txName = "OtherTransaction/Custom/com.nr.agent.instrumentation.httpclient.HttpClient31Test/httpClientExternal";
        Collection<String> names = introspector.getTransactionNames();
        assertEquals(2, names.size());
        assertTrue(names.contains(server.getServerTransactionName()));
        assertTrue(names.contains(txName));

        // scoped metrics
        assertEquals(1, MetricsHelper.getScopedMetricCount(txName, "ExternalTransaction/" + host + "/"
                + server.getCrossProcessId() + "/" + server.getServerTransactionName()));
        assertEquals(1, MetricsHelper.getScopedMetricCount(txName,
                "Java/com.nr.agent.instrumentation.httpclient.HttpClient31Test/httpClientExternal"));

        // unscoped metrics
        assertEquals(1, MetricsHelper.getUnscopedMetricCount("ExternalTransaction/" + host + "/"
                + server.getCrossProcessId() + "/" + server.getServerTransactionName()));
        assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/" + host + "/all"));
        assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/all"));
        assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/allOther"));

        // events
        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(txName);
        assertEquals(1, transactionEvents.size());
        TransactionEvent transactionEvent = transactionEvents.iterator().next();
        assertEquals(1, transactionEvent.getExternalCallCount());
        assertTrue(transactionEvent.getExternalDurationInSec() > 0);

        CatHelper.verifyOneSuccessfulCat(introspector, txName);

        // external request information
        Collection<ExternalRequest> externalRequests = introspector.getExternalRequests(txName);
        assertEquals(1, externalRequests.size());
        ExternalRequest externalRequest = externalRequests.iterator().next();
        assertEquals(1, externalRequest.getCount());
        assertEquals(host, externalRequest.getHostname());
        assertEquals(Integer.valueOf(200), externalRequest.getStatusCode());
        assertEquals("OK", externalRequest.getStatusText());
    }

}
