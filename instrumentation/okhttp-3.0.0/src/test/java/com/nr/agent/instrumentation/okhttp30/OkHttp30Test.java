/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.okhttp30;

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
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "com.nr.agent.instrumentation.okhttp30" })
public class OkHttp30Test {

    @ClassRule
    public static HttpServerRule server = new HttpServerRule();

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
        // creates a scoped (and unscoped)
        Assert.assertEquals(1, MetricsHelper.getScopedMetricCount(txTwo, "External/UnknownHost/OkHttp/execute"));
        Assert.assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/UnknownHost/OkHttp/execute"));

        // Unknown hosts generate no external rollups
        Assert.assertEquals(0, MetricsHelper.getUnscopedMetricCount("External/allOther"));
        Assert.assertEquals(0, MetricsHelper.getUnscopedMetricCount("External/all"));
    }

    @Test
    public void testExternal() throws Exception {
        URI endpoint = server.getEndPoint();
        String host1 = endpoint.getHost();
        httpClientExternal(endpoint.toString(), false, 2000);
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(2, introspector.getFinishedTransactionCount());
        String txOne = null;
        for (String txName : introspector.getTransactionNames()) {
            if (txName.matches(".*OkHttp30Test.*")) {
                txOne = txName;
            }
        }
        Assert.assertNotNull("Transaction not found", txOne);
        Assert.assertEquals(1, MetricsHelper.getScopedMetricCount(txOne, "External/" + host1 + "/OkHttp/execute"));
        Assert.assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/" + host1 + "/OkHttp/execute"));

        // external rollups
        Assert.assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/" + host1 + "/all"));
        Assert.assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/allOther"));
        Assert.assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/all"));

        // verify timing of External/all metrics
        TracedMetricData externalMetrics = InstrumentationTestRunner.getIntrospector().getUnscopedMetrics().get("External/all");
        assertNotNull(externalMetrics);
        assertTrue(externalMetrics.getTotalTimeInSec() > 2);

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
        Assert.assertEquals("OkHttp", externalRequest.getLibrary());
        Assert.assertEquals("execute", externalRequest.getOperation());
        assertEquals(Integer.valueOf(200), externalRequest.getStatusCode());
        assertEquals("OK ", externalRequest.getStatusText()); // the test server does return the trailing space, this client does not trim it
    }

    @Test
    public void testCat() throws Exception {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        URI endpoint = server.getEndPoint();
        String host = endpoint.getHost();
        httpClientExternal(endpoint.toURL().toString());

        // transaction
        String txName = "OtherTransaction/Custom/com.nr.agent.instrumentation.okhttp30.OkHttp30Test/httpClientExternal";
        assertEquals(2, introspector.getFinishedTransactionCount());
        Collection<String> names = introspector.getTransactionNames();
        assertEquals(2, names.size());
        assertTrue(names.contains(server.getServerTransactionName()));
        assertTrue(names.contains(txName));

        // scoped metrics
        assertEquals(1, MetricsHelper.getScopedMetricCount(txName, "ExternalTransaction/" + host + "/"
                + server.getCrossProcessId() + "/" + server.getServerTransactionName()));
        assertEquals(1, MetricsHelper.getScopedMetricCount(txName,
                "Java/com.nr.agent.instrumentation.okhttp30.OkHttp30Test/httpClientExternal"));

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
        assertEquals("OK ", externalRequest.getStatusText()); // the test server does return the trailing space, this client does not trim it
    }

    private void httpClientExternal(String host) throws IOException {
        httpClientExternal(host, true, 0);
    }

    /**
     * Start a background transaction, make an external request using okhttp, then finish.
     */
    @Trace(dispatcher = true)
    private void httpClientExternal(String host, boolean doCat, long sleepTimeInMillis) throws IOException {
        final OkHttpClient client = new OkHttpClient();

        final Request request = new Request.Builder()
                .url(host)
                .addHeader(HttpTestServer.DO_CAT, String.valueOf(doCat))
                .addHeader(HttpTestServer.SLEEP_MS_HEADER_KEY, String.valueOf(sleepTimeInMillis))
                .build();

        Response response = client.newCall(request).execute();
        response.body().close();
    }

}
