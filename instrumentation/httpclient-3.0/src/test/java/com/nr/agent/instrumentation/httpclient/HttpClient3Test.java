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
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "org.apache.commons.httpclient" })
public class HttpClient3Test {
    List<HttpMethod> stuffToClose = new ArrayList<>(10);

    @Rule
    public HttpServerRule server = new HttpServerRule();

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

        String txOne = "OtherTransaction/Custom/com.nr.agent.instrumentation.httpclient.HttpClient3Test/httpClientExternal";
        Assert.assertEquals(1, MetricsHelper.getScopedMetricCount(txOne, "External/localhost/CommonsHttp"));
        Assert.assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/localhost/CommonsHttp"));

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
    }

    @Test
    public void testRollups() throws Exception {
        final int port = server.getEndPoint().getPort();

        // manually set host and port here in order to get 2 unique endpoints
        httpClientExternal("http://localhost:" + port);
        httpClientExternal("http://localhost:" + port);
        httpClientExternal("http://127.0.0.1:" + port);

        // generates one scoped metric
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(6, introspector.getFinishedTransactionCount());

        // host rollups
        Assert.assertEquals(2, MetricsHelper.getUnscopedMetricCount("External/localhost/CommonsHttp"));
        Assert.assertEquals(2, MetricsHelper.getUnscopedMetricCount("External/localhost/all"));

        Assert.assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/127.0.0.1/CommonsHttp"));
        Assert.assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/127.0.0.1/all"));

        // external rollups
        Assert.assertEquals(3, MetricsHelper.getUnscopedMetricCount("External/allOther"));
        Assert.assertEquals(3, MetricsHelper.getUnscopedMetricCount("External/all"));
    }

    @Test
    public void testCat() throws Exception {
        URI endpoint = server.getEndPoint();
        String host = endpoint.getHost();
        int status = httpClientExternal(endpoint.toURL().toString(), true);
        assertEquals(200, status);

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(2, introspector.getFinishedTransactionCount());

        String txName = "OtherTransaction/Custom/com.nr.agent.instrumentation.httpclient.HttpClient3Test/httpClientExternal";
        Collection<String> names = introspector.getTransactionNames();
        assertEquals(2, names.size());
        assertTrue(names.contains(server.getServerTransactionName()));
        assertTrue(names.contains(txName));

        // scoped metrics
        assertEquals(1, MetricsHelper.getScopedMetricCount(txName, "ExternalTransaction/" + host + "/"
                + server.getCrossProcessId() + "/" + server.getServerTransactionName()));
        assertEquals(1, MetricsHelper.getScopedMetricCount(txName,
                "Java/com.nr.agent.instrumentation.httpclient.HttpClient3Test/httpClientExternal"));

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
    public void httpClient1() throws IOException, URISyntaxException {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();

        URI endpoint = server.getEndPoint();
        execute1(endpoint);

        Map<String, TracedMetricData> metrics = introspector.getMetricsForTransaction(
                "OtherTransaction/Custom/com.nr.agent.instrumentation.httpclient.HttpClient3Test/execute1");
        Assert.assertTrue(metrics.containsKey("External/" + endpoint.getHost() + "/CommonsHttp"));
        metrics.get("External/" + endpoint.getHost() + "/CommonsHttp");
        // This is 2 because execute3() calls execute() and releaseConnection(), both are instrumented
        Assert.assertEquals(2, metrics.get("External/" + endpoint.getHost() + "/CommonsHttp").getCallCount());
    }

    @Trace(dispatcher = true)
    private void execute1(URI endpoint) throws IOException {
        HttpClient client = new HttpClient();
        HttpMethod method = new GetMethod(endpoint.toString());
        method.setRequestHeader(HttpTestServer.DO_CAT, "false");

        int status = client.executeMethod(method);
        assertEquals(200, status);

        method.releaseConnection();
    }

    @Test
    public void httpClient2() throws IOException, URISyntaxException {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();

        URI endpoint = server.getEndPoint();
        execute2(endpoint);
        Map<String, TracedMetricData> metrics = introspector.getMetricsForTransaction(
                "OtherTransaction/Custom/com.nr.agent.instrumentation.httpclient.HttpClient3Test/execute2");
        Assert.assertTrue(metrics.containsKey("External/" + endpoint.getHost() + "/CommonsHttp"));
        metrics.get("External/" + endpoint.getHost() + "/CommonsHttp");
        // This is 2 because execute2() calls execute() and releaseConnection(), both are instrumented
        Assert.assertEquals(2, metrics.get("External/" + endpoint.getHost() + "/CommonsHttp").getCallCount());
    }

    @Trace(dispatcher = true)
    private void execute2(URI endpoint) throws IOException {
        HttpClient client = new HttpClient();
        HostConfiguration hostConfig = new HostConfiguration();
        hostConfig.setHost(endpoint.getHost(), endpoint.getPort());
        HttpMethod method = new GetMethod();
        method.setRequestHeader(HttpTestServer.DO_CAT, "false");

        int status = client.executeMethod(hostConfig, method);
        assertEquals(200, status);
        method.releaseConnection();
    }

    @Test
    public void httpClient3() throws IOException, URISyntaxException {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();

        URI endpoint = server.getEndPoint();
        execute3(endpoint);
        Map<String, TracedMetricData> metrics = introspector.getMetricsForTransaction(
                "OtherTransaction/Custom/com.nr.agent.instrumentation.httpclient.HttpClient3Test/execute3");
        Assert.assertTrue(metrics.containsKey("External/" + endpoint.getHost() + "/CommonsHttp"));
        metrics.get("External/" + endpoint.getHost() + "/CommonsHttp");
        // This is 2 because execute3() calls execute() and releaseConnection(), both are instrumented
        Assert.assertEquals(2, metrics.get("External/" + endpoint.getHost() + "/CommonsHttp").getCallCount());
    }

    @Trace(dispatcher = true)
    private void execute3(URI endpoint) throws IOException {
        HttpClient client = new HttpClient();
        HostConfiguration hostConfig = new HostConfiguration();
        hostConfig.setHost(endpoint.getHost(), endpoint.getPort());
        HttpMethod method = new GetMethod(endpoint.toString());
        method.setRequestHeader(HttpTestServer.DO_CAT, "false");

        int status = client.executeMethod(method);
        assertEquals(200, status);
        method.releaseConnection();
    }

    @Test
    public void httpClient4() throws IOException, URISyntaxException, InterruptedException {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();

        URI endpoint = server.getEndPoint();
        execute4(endpoint);
        Map<String, TracedMetricData> metrics = introspector.getMetricsForTransaction(
                "OtherTransaction/Custom/com.nr.agent.instrumentation.httpclient.HttpClient3Test/execute4");

        Assert.assertTrue(metrics.containsKey("External/" + endpoint.getHost() + "/CommonsHttp"));
        metrics.get("External/" + endpoint.getHost() + "/CommonsHttp");
        // This is 3 because execute4() calls execute(), getResponseBody()
        // and releaseConnection(). All 3 methods are instrumented
        Assert.assertEquals(3, metrics.get("External/" + endpoint.getHost() + "/CommonsHttp").getCallCount());
    }

    @Trace(dispatcher = true)
    private void execute4(URI endpoint) throws IOException, InterruptedException {
        HttpClient httpClient = new HttpClient();
        HttpMethod method = new GetMethod(endpoint.toString());
        method.setRequestHeader(HttpTestServer.DO_CAT, "false");

        int status = httpClient.executeMethod(null, method, new HttpState());
        assertEquals(200, status);
        method.getResponseBody();
        method.releaseConnection();
    }

    @Test
    public void httpClient5() throws IOException, URISyntaxException {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();

        URI endpoint = server.getEndPoint();
        execute5(endpoint);
        Map<String, TracedMetricData> metrics = introspector.getMetricsForTransaction(
                "OtherTransaction/Custom/com.nr.agent.instrumentation.httpclient.HttpClient3Test/execute5");
        Assert.assertTrue(metrics.containsKey("External/" + endpoint.getHost() + "/CommonsHttp"));
        metrics.get("External/" + endpoint.getHost() + "/CommonsHttp");
        // This is 2 because execute5() calls execute() and releaseConnection(), both are instrumented
        Assert.assertEquals(2, metrics.get("External/" + endpoint.getHost() + "/CommonsHttp").getCallCount());
    }

    @Trace(dispatcher = true)
    private void execute5(URI endpoint) throws IOException {
        HttpClient httpClient = new HttpClient(new MultiThreadedHttpConnectionManager());
        httpClient.getHostConfiguration().setHost(
                endpoint.getHost(), endpoint.getPort(), endpoint.getScheme());
        HttpMethod method = new GetMethod(endpoint.toString());
        method.setRequestHeader(HttpTestServer.DO_CAT, "false");
        method.setFollowRedirects(true);

        int status = httpClient.executeMethod(null, method);
        assertEquals(200, status);
        method.releaseConnection();
    }

}
