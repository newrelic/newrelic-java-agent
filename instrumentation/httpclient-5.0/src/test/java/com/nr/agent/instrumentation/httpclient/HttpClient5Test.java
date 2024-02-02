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
import org.apache.hc.client5.http.async.methods.SimpleHttpRequest;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.hc.client5.http.async.methods.SimpleRequestBuilder;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.client5.http.impl.classic.BasicHttpClientResponseHandler;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.Message;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.nio.AsyncRequestProducer;
import org.apache.hc.core5.http.nio.entity.AsyncEntityProducers;
import org.apache.hc.core5.http.nio.entity.StringAsyncEntityConsumer;
import org.apache.hc.core5.http.nio.support.BasicRequestProducer;
import org.apache.hc.core5.http.nio.support.BasicResponseConsumer;
import org.apache.hc.core5.io.CloseMode;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "org.apache.hc.client5", "org.apache.hc.core5" })
public class HttpClient5Test {
    @Rule
    public HttpServerRule server = new HttpServerRule();

    @Test
    public void testErrorClassic() throws Exception {
        testError(false, false);
    }

    @Test
    public void testErrorAsync() throws Exception {
        testError(true, false);
    }

    @Test
    public void testErrorAsyncWithMessage() throws Exception {
        testError(true, false);
    }

    private void testError(boolean async, boolean withMessage) throws Exception {
        final String host2 = "www.notarealhostbrosef.bro";
        try {
            if (async) {
                if (withMessage) {
                    httpClientExternalAsyncWithMessage("http://" + host2, true);
                } else {
                    httpClientExternalAsync("http://" + host2, true);
                }
            } else {
                httpClientExternalClassic("http://" + host2, true);
            }
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
    public void testExternalClassic() throws Exception {
        testExternal(false, false);
    }

    @Test
    public void testExternalAsync() throws Exception {
        testExternal(true, false);
    }

    @Test
    public void testExternalAsyncWithMessage() throws Exception {
        testExternal(true, true);
    }

    private void testExternal(boolean async, boolean withMessage) throws Exception {
        URI endpoint = server.getEndPoint();
        String host1 = endpoint.getHost();

        if (async) {
            if (withMessage) {
                httpClientExternalAsyncWithMessage(endpoint.toString(), false);
            } else {
                httpClientExternalAsync(endpoint.toString(), false);
            }
        } else {
            httpClientExternalClassic(endpoint.toString(), false);
        }

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(2, introspector.getFinishedTransactionCount());
        String txOne = null;
        for (String txName : introspector.getTransactionNames()) {
            if (txName.matches(".*HttpClient5Test.*")) {
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
    public void testRollupsClassic() throws Exception {
        testRollups(false, false);
    }

    @Test
    public void testRollupsAsync() throws Exception {
        testRollups(true, false);
    }

    @Test
    public void testRollupsAsyncWithMessage() throws Exception {
        testRollups(true, true);
    }

    private  void testRollups(boolean async, boolean withMessage) throws Exception {
        // manually set host and port here in order to get 2 unique endpoints
        if (async) {
            if (withMessage) {
                httpClientExternalAsyncWithMessage("http://localhost:" + server.getEndPoint().getPort(), false);
                httpClientExternalAsyncWithMessage("http://localhost:" + server.getEndPoint().getPort(), false);
                httpClientExternalAsyncWithMessage("http://127.0.0.1:" + server.getEndPoint().getPort(), false);
            } else {
                httpClientExternalAsync("http://localhost:" + server.getEndPoint().getPort(), false);
                httpClientExternalAsync("http://localhost:" + server.getEndPoint().getPort(), false);
                httpClientExternalAsync("http://127.0.0.1:" + server.getEndPoint().getPort(), false);
            }
        } else {
            httpClientExternalClassic("http://localhost:" + server.getEndPoint().getPort(), false);
            httpClientExternalClassic("http://localhost:" + server.getEndPoint().getPort(), false);
            httpClientExternalClassic("http://127.0.0.1:" + server.getEndPoint().getPort(), false);
        }

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
    public void testCatClassic() throws Exception {
        testCat(false, false);
    }

    @Test
    public void testCatAsync() throws Exception {
        testCat(true, false);
    }

    @Test
    public void testCatAsyncWithMessage() throws Exception {
        testCat(true, true);
    }

    private void testCat(boolean async, boolean withMessage) throws Exception {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        URI endpoint = server.getEndPoint();
        String host = endpoint.getHost();

        String methodName = "httpClientExternalClassic";
        if (async) {
            if (withMessage) {
                httpClientExternalAsyncWithMessage(endpoint.toURL().toString(), true);
                methodName = "httpClientExternalAsyncWithMessage";
            } else {
                httpClientExternalAsync(endpoint.toURL().toString(), true);
                methodName = "httpClientExternalAsync";
            }
        } else {
            httpClientExternalClassic(endpoint.toURL().toString(), true);
        }

        // transaction
        String txName = "OtherTransaction/Custom/com.nr.agent.instrumentation.httpclient.HttpClient5Test/"+methodName;
        assertEquals(2, introspector.getFinishedTransactionCount());
        Collection<String> names = introspector.getTransactionNames();
        assertEquals(2, names.size());
        assertTrue(names.contains(server.getServerTransactionName()));
        assertTrue(names.contains(txName));

        // scoped metrics
        assertEquals(1, MetricsHelper.getScopedMetricCount(txName, "ExternalTransaction/" + host + "/"
                + server.getCrossProcessId() + "/" + server.getServerTransactionName()));
        assertEquals(1, MetricsHelper.getScopedMetricCount(txName,
                "Java/com.nr.agent.instrumentation.httpclient.HttpClient5Test/"+methodName));

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
    public void testHostWithoutScheme() throws Exception {
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
    private void httpClientExternalClassic(String host, boolean doCat) throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpget = new HttpGet(host);
            httpget.addHeader(HttpTestServer.DO_CAT, String.valueOf(doCat));
            HttpClientResponseHandler<String> responseHandler = new BasicHttpClientResponseHandler();
            httpClient.<String>execute(httpget, responseHandler);
        }
    }

    @Trace(dispatcher = true)
    private void httpClientExternalAsync(String host, boolean doCat) throws Exception {
        try (CloseableHttpAsyncClient httpAsyncClient = HttpAsyncClients.createDefault()) {
            httpAsyncClient.start();
            SimpleHttpRequest request = SimpleRequestBuilder.get(host).build();
            request.addHeader(HttpTestServer.DO_CAT, String.valueOf(doCat));
            Future<SimpleHttpResponse> future = httpAsyncClient.execute(request, null);
            SimpleHttpResponse response = future.get();
        } catch (ExecutionException ee) {
            if (ee.getCause() instanceof UnknownHostException) throw new UnknownHostException ();
        }
    }

    @Trace(dispatcher = true)
    private void httpClientExternalAsyncWithMessage(String host, boolean doCat) throws Exception {
        try (CloseableHttpAsyncClient httpAsyncClient = HttpAsyncClients.createDefault()) {
            httpAsyncClient.start();
            SimpleHttpRequest request = SimpleRequestBuilder.get(host).build();
            request.addHeader(HttpTestServer.DO_CAT, String.valueOf(doCat));
            final AsyncRequestProducer requestProducer = new BasicRequestProducer(request,
                    AsyncEntityProducers.create("", ContentType.TEXT_PLAIN));
            Future<Message<HttpResponse, String>> response = httpAsyncClient.execute(
                    requestProducer,
                    new BasicResponseConsumer<String>(new StringAsyncEntityConsumer()),
                    null);
            Message msg = response.get();

            httpAsyncClient.close(CloseMode.GRACEFUL);
        } catch (ExecutionException ee) {
            if (ee.getCause() instanceof UnknownHostException) throw new UnknownHostException();
        }
    }

    @Trace(dispatcher = true)
    public void externalRequest(URI endpoint) throws IOException {
        NewRelic.getAgent().getTransaction().setTransactionName(TransactionNamePriority.CUSTOM_HIGH, true, "HttpClientTest", "externalRequest");
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            final HttpHost httpHost = new HttpHost(endpoint.getScheme(), endpoint.getHost(), endpoint.getPort());
            final HttpPost httpPost = new HttpPost(URI.create("/qw"));
            httpPost.addHeader(HttpTestServer.DO_CAT, "false");
            client.execute(httpHost, httpPost);
        }
    }

}
