/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.httpclient;

import com.newrelic.agent.introspec.*;
import com.newrelic.agent.introspec.internal.HttpServerRule;
import com.newrelic.agent.model.SpanCategory;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.TransactionNamePriority;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "org.apache.hc.client5", "org.apache.hc.core5" }, configName = "spans.yml")
public class HttpClient5ClassicSpanTests {

    @Rule
    public HttpServerRule server = new HttpServerRule();

    @Test
    public void testExternalSpan() throws Exception {
        URI endpoint = server.getEndPoint();
        httpClientExternal(endpoint.toString() + "?username=bad&password=worse&whowoulddothis=somecustomer");

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        // server and client
        assertEquals(2, introspector.getFinishedTransactionCount(TimeUnit.SECONDS.toMillis(30)));
        final String txnName = "OtherTransaction/ExternalRequest/Client";

        Collection<ExternalRequest> externalRequests = introspector.getExternalRequests(txnName);
        assertEquals(1, externalRequests.size());

        ExternalRequest externalRequest = externalRequests.iterator().next();
        assertEquals("CommonsHttp", externalRequest.getLibrary());
        assertEquals("execute", externalRequest.getOperation());

        Collection<SpanEvent> externalSpanEvents = SpanEventsHelper.getSpanEventsByCategory(SpanCategory.http);
        assertEquals(1, externalSpanEvents.size());
        SpanEvent externalSpanEvent = externalSpanEvents.iterator().next();
        assertEquals(endpoint.toString(), externalSpanEvent.getHttpUrl());
        assertEquals("CommonsHttp", externalSpanEvent.getHttpComponent());
        assertEquals("execute", externalSpanEvent.getHttpMethod());
        assertEquals(Integer.valueOf(200), externalSpanEvent.getStatusCode());
        assertEquals("OK", externalSpanEvent.getStatusText());
    }

    /**
     * Start a background transaction, make an external request using httpclient, then finish.
     */
    @Trace(dispatcher = true)
    private void httpClientExternal(String host) throws IOException {
        NewRelic.getAgent().getTransaction().setTransactionName(TransactionNamePriority.CUSTOM_HIGH, true, "ExternalRequest", "Client");
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            HttpGet httpget = new HttpGet(host);
            httpclient.execute(httpget);
        }
    }

}
