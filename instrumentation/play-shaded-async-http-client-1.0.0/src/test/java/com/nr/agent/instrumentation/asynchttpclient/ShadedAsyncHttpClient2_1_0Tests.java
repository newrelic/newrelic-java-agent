/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.asynchttpclient;

import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.agent.introspec.TraceSegment;
import com.newrelic.agent.introspec.TransactionEvent;
import com.newrelic.agent.introspec.TransactionTrace;
import com.newrelic.agent.introspec.internal.HttpServerRule;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.test.marker.Java10IncompatibleTest;
import com.newrelic.test.marker.Java11IncompatibleTest;
import com.newrelic.test.marker.Java12IncompatibleTest;
import com.newrelic.test.marker.Java13IncompatibleTest;
import com.newrelic.test.marker.Java14IncompatibleTest;
import com.newrelic.test.marker.Java15IncompatibleTest;
import com.newrelic.test.marker.Java16IncompatibleTest;
import com.newrelic.test.marker.Java17IncompatibleTest;
import com.newrelic.test.marker.Java9IncompatibleTest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import play.shaded.ahc.org.asynchttpclient.AsyncCompletionHandler;
import play.shaded.ahc.org.asynchttpclient.AsyncHttpClient;
import play.shaded.ahc.org.asynchttpclient.BoundRequestBuilder;
import play.shaded.ahc.org.asynchttpclient.DefaultAsyncHttpClientConfig;
import play.shaded.ahc.org.asynchttpclient.Dsl;
import play.shaded.ahc.org.asynchttpclient.Response;

import java.net.URI;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;

@Category({ Java9IncompatibleTest.class, Java10IncompatibleTest.class, Java11IncompatibleTest.class, Java12IncompatibleTest.class,
        Java13IncompatibleTest.class, Java14IncompatibleTest.class, Java15IncompatibleTest.class, Java16IncompatibleTest.class,
        Java17IncompatibleTest.class })
@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "com.nr.agent.instrumentation.asynchttpclient", "org.asynchttpclient", "play.shaded.ahc.org.asynchttpclient" })
public class ShadedAsyncHttpClient2_1_0Tests {

    @Rule
    public HttpServerRule server = new HttpServerRule();

    @Test
    public void testTxnWithOnComplete() throws Exception {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();

        URI endpoint = server.getEndPoint();
        String host = endpoint.getHost();
        String url = endpoint.toURL().toExternalForm();
        int status = makeAynscRequestWithOnComplete(url + "?no-transaction=true");
        assertEquals(200, status);

        // transaction
        assertEquals(1, introspector.getFinishedTransactionCount());
        String txName = introspector.getTransactionNames().iterator().next();

        // events
        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(txName);
        assertEquals(1, transactionEvents.size());
        TransactionEvent transactionEvent = transactionEvents.iterator().next();
        // attribute added in onCompleted method of future
        assertEquals(1, transactionEvent.getAttributes().size());
        assertEquals("test", transactionEvent.getAttributes().get("key"));

        // traces
        Collection<TransactionTrace> traces = introspector.getTransactionTracesForTransaction(txName);
        assertEquals(1, traces.size());
        TransactionTrace trace = traces.iterator().next();
        for (TraceSegment segment : trace.getInitialTraceSegment().getChildren()) {
            Map<String, Object> attributes = segment.getTracerAttributes();
            if (attributes.get("async_context").equals("External")) {
                assertEquals("", segment.getMethodName());
                assertEquals("External Request", segment.getClassName());
                assertEquals("External/" + host + "/AsyncHttpClient/onCompleted", segment.getName());
                assertEquals(url, segment.getUri());
                assertEquals(1, segment.getCallCount());
            }
            assertEquals("test", attributes.get("key"));
        }
    }

    @Trace(dispatcher = true)
    private static int makeAynscRequestWithOnComplete(String url) {
        DefaultAsyncHttpClientConfig.Builder clientBuilder = Dsl.config()
                .setConnectTimeout(500);

        try (AsyncHttpClient client = Dsl.asyncHttpClient(clientBuilder)) {
            BoundRequestBuilder builder = client.prepareGet(url);

            // see https://www.baeldung.com/async-http-client
            Future<Response> future = builder.execute(new AsyncCompletionHandler<Response>() {
                @Override
                public Response onCompleted(Response response) {
                    NewRelic.addCustomParameter("key", "test");
                    return response;
                }
            });

            Response response = future.get();
            return response.getStatusCode();
        } catch (Exception e) {
            return -1;
        }
    }

}
