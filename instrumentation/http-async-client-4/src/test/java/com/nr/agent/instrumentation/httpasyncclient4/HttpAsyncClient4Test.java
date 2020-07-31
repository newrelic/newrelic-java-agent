/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.httpasyncclient4;

import com.newrelic.agent.introspec.CatHelper;
import com.newrelic.agent.introspec.ExternalRequest;
import com.newrelic.agent.introspec.HttpTestServer;
import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.agent.introspec.MetricsHelper;
import com.newrelic.agent.introspec.TransactionEvent;
import com.newrelic.agent.introspec.internal.HttpServerLocator;
import com.newrelic.agent.introspec.internal.HttpServerRule;
import com.newrelic.api.agent.Trace;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = {"org.apache.http.nio.client", "org.apache.http.nio.protocol"})
public class HttpAsyncClient4Test {

    @ClassRule
    public static HttpServerRule server = new HttpServerRule();

    static URI endpoint;
    static String host;

    @BeforeClass
    public static void before() {
        try {
            endpoint = server.getEndPoint();
            host = endpoint.getHost();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testError() {
        final String host2 = "www.notarealhostbrosef.bro";
        try {
            httpClientExternal("http://" + host2, false);
            Assert.fail("Host should not be reachable: " + host2);
        } catch (Exception e) {
        }

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount());
        final String txTwo = introspector.getTransactionNames().iterator().next();
        // creates a scoped (and unscoped)
        Assert.assertEquals(1, MetricsHelper.getScopedMetricCount(txTwo, "External/UnknownHost/HttpAsyncClient/failed"));
        Assert.assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/UnknownHost/HttpAsyncClient/failed"));

        // Unknown hosts generate no external rollups
        Assert.assertEquals(0, MetricsHelper.getUnscopedMetricCount("External/allOther"));
        Assert.assertEquals(0, MetricsHelper.getUnscopedMetricCount("External/all"));
    }

    @Test
    public void testExternal() throws IOException, InterruptedException, ExecutionException {
        httpClientExternal(endpoint.toString(), false);

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(2, introspector.getFinishedTransactionCount());
        String txOne = null;
        for (String txName : introspector.getTransactionNames()) {
            if (txName.matches(".*HttpAsyncClient.*")) {
                txOne = txName;
            }
        }
        Assert.assertNotNull("Transaction not found", txOne);
        Assert.assertEquals(1, MetricsHelper.getScopedMetricCount(txOne, "External/" + host + "/HttpAsyncClient/responseCompleted"));

        Assert.assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/" + host + "/HttpAsyncClient/responseCompleted"));

        // external rollups
        Assert.assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/" + host + "/all"));
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
        Assert.assertEquals(host, externalRequest.getHostname());
        Assert.assertEquals("HttpAsyncClient", externalRequest.getLibrary());
        Assert.assertEquals("responseCompleted", externalRequest.getOperation());
    }

    @Test
    public void testRollups() throws IOException, InterruptedException, ExecutionException, URISyntaxException {
        final int port = server.getEndPoint().getPort();
        // manually set host and port here in order to get 2 unique endpoints
        httpClientExternal("http://localhost:" + port, false);
        httpClientExternal("http://localhost:" + port, false);
        httpClientExternal("http://127.0.0.1:" + port, false);

        // generates one scoped metric
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(6, introspector.getFinishedTransactionCount());

        // host rollups
        Assert.assertEquals(2, MetricsHelper.getUnscopedMetricCount("External/localhost/HttpAsyncClient/responseCompleted"));
        Assert.assertEquals(2, MetricsHelper.getUnscopedMetricCount("External/localhost/all"));

        Assert.assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/127.0.0.1/HttpAsyncClient/responseCompleted"));
        Assert.assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/127.0.0.1/all"));

        // external rollups
        Assert.assertEquals(3, MetricsHelper.getUnscopedMetricCount("External/allOther"));
        Assert.assertEquals(3, MetricsHelper.getUnscopedMetricCount("External/all"));
    }

    @Test
    public void testCat() throws IOException, InterruptedException, ExecutionException {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();

        httpClientExternal(endpoint.toURL().toString(), true);

        // transaction
        String txName = "OtherTransaction/Custom/com.nr.agent.instrumentation.httpasyncclient4.HttpAsyncClient4Test/httpClientExternal";
        assertEquals(2, introspector.getFinishedTransactionCount());
        Collection<String> names = introspector.getTransactionNames();
        assertEquals(2, names.size());
        assertTrue(names.contains(server.getServerTransactionName()));
        assertTrue(names.contains(txName));

        // scoped metrics
        assertEquals(1, MetricsHelper.getScopedMetricCount(txName, "ExternalTransaction/" + host + "/"
                + server.getCrossProcessId() + "/" + server.getServerTransactionName()));
        assertEquals(1, MetricsHelper.getScopedMetricCount(txName,
                "Java/com.nr.agent.instrumentation.httpasyncclient4.HttpAsyncClient4Test/httpClientExternal"));

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
    }

    /**
     * Start a background transaction, make an external request using httpclient, then finish.
     */
    @Trace(dispatcher = true)
    private void httpClientExternal(String host, boolean doCat) throws IOException, ExecutionException, InterruptedException {
        try (CloseableHttpAsyncClient httpclient = HttpAsyncClients.createDefault()) {
            httpclient.start();
            HttpGet request = new HttpGet(host);
            request.addHeader(HttpTestServer.DO_CAT, String.valueOf(doCat));
            Future<HttpResponse> future = httpclient.execute(request, null);
            HttpResponse response = future.get();
            Thread.sleep(200);

        }
    }
}
