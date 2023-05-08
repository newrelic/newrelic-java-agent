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
import com.newrelic.agent.introspec.TransactionEvent;
import com.newrelic.agent.introspec.internal.HttpServerRule;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.TransactionNamePriority;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.BasicHttpClientResponseHandler;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "org.apache.hc.client5" })
public class HttpClient5ClassicTest {
    @Rule
    public HttpServerRule server = new HttpServerRule();

    @Test
    public void testError() throws Exception {
        final String host2 = "www.notarealhostbrosef.bro";
        try {
            httpClientExternal("http://" + host2, true);
            Assert.fail("Host should not be reachable: " + host2);
        } catch (UnknownHostException e) {
        }

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount());
        final String txTwo = introspector.getTransactionNames().iterator().next();
        // creates a scoped (and unscoped)
        Assert.assertEquals(1, MetricsHelper.getScopedMetricCount(txTwo, "External/UnknownHost/CommonsHttp/execute"));
        Assert.assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/UnknownHost/CommonsHttp/execute"));

        // Unknown hosts generate no external rollups
        Assert.assertEquals(0, MetricsHelper.getUnscopedMetricCount("External/allOther"));
        Assert.assertEquals(0, MetricsHelper.getUnscopedMetricCount("External/all"));
    }

    @Test
    public void testExternal() throws Exception {
        URI endpoint = server.getEndPoint();
        String host1 = endpoint.getHost();

        httpClientExternal(endpoint.toString(), false);

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(2, introspector.getFinishedTransactionCount());
        String txOne = null;
        for (String txName : introspector.getTransactionNames()) {
            if (txName.matches(".*HttpClient5ClassicTest.*")) {
                txOne = txName;
            }
        }
        Assert.assertNotNull("Transaction not found", txOne);
        Assert.assertEquals(1, MetricsHelper.getScopedMetricCount(txOne, "External/" + host1 + "/CommonsHttp/execute"));
        Assert.assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/" + host1 + "/CommonsHttp/execute"));

        // external rollups
        Assert.assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/" + host1 + "/all"));
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
        Assert.assertEquals(host1, externalRequest.getHostname());
        Assert.assertEquals("CommonsHttp", externalRequest.getLibrary());
        Assert.assertEquals("execute", externalRequest.getOperation());
    }

    @Test
    public void testRollups() throws Exception {
        // manually set host and port here in order to get 2 unique endpoints
        httpClientExternal("http://localhost:" + server.getEndPoint().getPort(), false);
        httpClientExternal("http://localhost:" + server.getEndPoint().getPort(), false);
        httpClientExternal("http://127.0.0.1:" + server.getEndPoint().getPort(), false);

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

    @Test
    public void testCat() throws Exception {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        URI endpoint = server.getEndPoint();
        String host = endpoint.getHost();
        httpClientExternal(endpoint.toURL().toString(), true);

        // transaction
        String txName = "OtherTransaction/Custom/com.nr.agent.instrumentation.httpclient.HttpClient5ClassicTest/httpClientExternal";
        assertEquals(2, introspector.getFinishedTransactionCount());
        Collection<String> names = introspector.getTransactionNames();
        assertEquals(2, names.size());
        assertTrue(names.contains(server.getServerTransactionName()));
        assertTrue(names.contains(txName));

        // scoped metrics
        assertEquals(1, MetricsHelper.getScopedMetricCount(txName, "ExternalTransaction/" + host + "/"
                + server.getCrossProcessId() + "/" + server.getServerTransactionName()));
        assertEquals(1, MetricsHelper.getScopedMetricCount(txName,
                "Java/com.nr.agent.instrumentation.httpclient.HttpClient5ClassicTest/httpClientExternal"));

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

    @Test
    public void testHostWithoutScheme() throws IOException, URISyntaxException {
        externalRequest(server.getEndPoint());

        String txName = "OtherTransaction/HttpClientTest/externalRequest";
        final String host = server.getEndPoint().getHost();

        assertEquals(1, MetricsHelper.getScopedMetricCount(txName, "External/" + host + "/CommonsHttp/execute"));
        assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/" + host + "/CommonsHttp/execute"));
        assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/" + host + "/all"));
        assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/allOther"));
    }

    /**
     * Start a background transaction, make an external request using httpclient, then finish.
     */
    @Trace(dispatcher = true)
    private void httpClientExternal(String host, boolean doCat) throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpget = new HttpGet(host);
            httpget.addHeader(HttpTestServer.DO_CAT, String.valueOf(doCat));
            HttpClientResponseHandler<String> responseHandler = new BasicHttpClientResponseHandler();
            httpClient.<String>execute(httpget, responseHandler);
        }
    }

    @Trace(dispatcher = true)
    public void externalRequest(URI endpoint) throws IOException {
        NewRelic.getAgent().getTransaction().setTransactionName(TransactionNamePriority.CUSTOM_HIGH, true, "HttpClientTest", "externalRequest");
        CloseableHttpClient client = HttpClients.createDefault();
        final HttpHost httpHost = new HttpHost(endpoint.getScheme(), endpoint.getHost(), endpoint.getPort());
        final HttpPost httpPost = new HttpPost(URI.create("/qw"));
        httpPost.addHeader(HttpTestServer.DO_CAT, "false");
        client.execute(httpHost, httpPost);
    }

}
