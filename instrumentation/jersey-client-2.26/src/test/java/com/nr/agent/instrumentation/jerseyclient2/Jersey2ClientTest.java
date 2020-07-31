/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.jerseyclient2;

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
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ws.rs.client.AsyncInvoker;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "org.glassfish.jersey" })
public class Jersey2ClientTest {

    @Rule
    public HttpServerRule server = new HttpServerRule();

    @Test
    public void testError() {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();

        String fakeUrl = "http://www.picturesofgeese.com";
        makeExternalRequest(fakeUrl, true);
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(3000));

        // Unknown hosts generate no external rollups
        Assert.assertEquals(0, MetricsHelper.getUnscopedMetricCount("External/allOther"));
        Assert.assertEquals(0, MetricsHelper.getUnscopedMetricCount("External/all"));

        String txName = "OtherTransaction/Custom/com.nr.agent.instrumentation.jerseyclient2.Jersey2ClientTest/makeExternalRequest";
        Assert.assertEquals(1, MetricsHelper.getScopedMetricCount(txName, "External/UnknownHost/Jersey-Client/failed"));
        Assert.assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/UnknownHost/Jersey-Client/failed"));
    }

    @Test
    public void testHttpMethods() throws Exception {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();

        URI endpoint = server.getEndPoint();
        requestHttpMethods(endpoint.toURL().toString());

        // transaction
        String txName = "OtherTransaction/Custom/com.nr.agent.instrumentation.jerseyclient2.Jersey2ClientTest/requestHttpMethods";
        assertEquals(1, introspector.getFinishedTransactionCount(3000));
        assertTrue(introspector.getTransactionNames().contains(txName));

        //external request test should go here
        Collection<ExternalRequest> externalRequests = introspector.getExternalRequests(txName);

        assertEquals(4, externalRequests.size());
        assertEquals(4, MetricsHelper.getUnscopedMetricCount("External/localhost/all"));
        assertEquals(4, MetricsHelper.getUnscopedMetricCount("External/allOther"));
        assertEquals(4, MetricsHelper.getUnscopedMetricCount("External/all"));
    }

    @Test
    public void testAsync() throws Exception {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();

        URI endpoint = server.getEndPoint();
        requestHttpMethodsAsync(endpoint.toString());

        // transaction
        String txName = "OtherTransaction/Custom/com.nr.agent.instrumentation.jerseyclient2.Jersey2ClientTest/requestHttpMethodsAsync";
        assertEquals(1, introspector.getFinishedTransactionCount(2000));
        Collection<String> names = introspector.getTransactionNames();
        assertTrue(names.contains(txName));

        //external request test should go here
        Collection<ExternalRequest> externalRequests = introspector.getExternalRequests(txName);

        assertEquals(4, externalRequests.size());
        assertEquals(4, MetricsHelper.getUnscopedMetricCount("External/localhost/all"));
        assertEquals(4, MetricsHelper.getUnscopedMetricCount("External/allOther"));
        assertEquals(4, MetricsHelper.getUnscopedMetricCount("External/all"));
    }

    @Test
    public void testAsyncBad() {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        requestHttpMethodsAsync("http://thisdoesnotexist2sdf.com");

        // transaction
        String txName = "OtherTransaction/Custom/com.nr.agent.instrumentation.jerseyclient2.Jersey2ClientTest/requestHttpMethodsAsync";
        assertEquals(1, introspector.getFinishedTransactionCount(2000));
        Collection<String> names = introspector.getTransactionNames();
        assertTrue(names.contains(txName));

        Assert.assertEquals(1, MetricsHelper.getScopedMetricCount(txName, "External/UnknownHost/Jersey-Client/failed"));
        Assert.assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/UnknownHost/Jersey-Client/failed"));

        // Unknown hosts generate no external rollups
        Assert.assertEquals(0, MetricsHelper.getUnscopedMetricCount("External/allOther"));
        Assert.assertEquals(0, MetricsHelper.getUnscopedMetricCount("External/all"));
    }

    @Test
    public void testAsyncCallback() throws Exception {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();

        URI endpoint = server.getEndPoint();
        requestHttpMethodsAsyncCallback(endpoint.toString());

        // transaction
        String txName = "OtherTransaction/Custom/com.nr.agent.instrumentation.jerseyclient2.Jersey2ClientTest/requestHttpMethodsAsyncCallback";
        assertEquals(1, introspector.getFinishedTransactionCount(2000));
        Collection<String> names = introspector.getTransactionNames();
        assertTrue(names.contains(txName));

        //external request test should go here
        Collection<ExternalRequest> externalRequests = introspector.getExternalRequests(txName);

        assertEquals(1, externalRequests.size());
        assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/localhost/all"));
        assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/allOther"));
        assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/all"));
        assertEquals(1, MetricsHelper.getUnscopedMetricCount("IAmMetric"));
    }

    @Test
    public void testCat() throws Exception {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();

        URI endpoint = server.getEndPoint();
        String host = endpoint.getHost();
        makeExternalRequest(endpoint.toURL().toString(), true);

        // transaction
        String txName = "OtherTransaction/Custom/com.nr.agent.instrumentation.jerseyclient2.Jersey2ClientTest/makeExternalRequest";
        assertEquals(2, introspector.getFinishedTransactionCount(3000));
        Collection<String> names = introspector.getTransactionNames();
        assertEquals(2, names.size());
        assertTrue(names.contains(server.getServerTransactionName()));
        assertTrue(names.contains(txName));

        // scoped metrics
        assertEquals(1, MetricsHelper.getScopedMetricCount(txName, "ExternalTransaction/" + host + "/"
                + server.getCrossProcessId() + "/" + server.getServerTransactionName()));
        assertEquals(1, MetricsHelper.getScopedMetricCount(txName,
                "Java/com.nr.agent.instrumentation.jerseyclient2.Jersey2ClientTest/makeExternalRequest"));

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

    @Trace(dispatcher = true)
    public void makeExternalRequest(String host, boolean doCat) {
        try {
            Client client = ClientBuilder.newClient();
            client.target(host)
                    .request(MediaType.APPLICATION_JSON)
                    .header(HttpTestServer.DO_CAT, String.valueOf(doCat))
                    .get();
        } catch (Exception ignored) {
        }
    }

    @Trace(dispatcher = true)
    public void requestHttpMethods(String host) {
        Client client = ClientBuilder.newClient();
        StringBuilder hostUrl = new StringBuilder(host).append("?no-transaction");
        try {
            Invocation.Builder builder = client.target(hostUrl.toString()).request(MediaType.APPLICATION_JSON);
            builder.get();
            builder.delete();
            builder.post(Entity.entity("Entity", MediaType.APPLICATION_JSON));
            builder.put(Entity.entity("EntityTwo", MediaType.APPLICATION_JSON));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Trace(dispatcher = true)
    public void requestHttpMethodsAsync(String host) {
        Client client = ClientBuilder.newClient();
        StringBuilder hostUrl = new StringBuilder(host).append("?no-transaction");
        try {
            AsyncInvoker invoker = client.target(hostUrl.toString()).request(MediaType.APPLICATION_JSON).async();
            invoker.get().get();
            invoker.delete().get();
            invoker.post(Entity.entity("Hoop", MediaType.APPLICATION_JSON)).get();
            invoker.put(Entity.entity("Shoop", MediaType.APPLICATION_JSON)).get();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Trace(dispatcher = true)
    public void requestHttpMethodsAsyncCallback(String host) {
        Client client = ClientBuilder.newClient();
        StringBuilder hostUrl = new StringBuilder(host).append("?no-transaction");
        try {
            client.target(hostUrl.toString())
                    .request(MediaType.APPLICATION_JSON)
                    .async()
                    .get(new InvocationCallback<Response>() {
                        @Override
                        public void completed(Response response) {
                            System.out.println("Success");
                            NewRelic.recordMetric("IAmMetric", 27);
                        }

                        @Override
                        public void failed(Throwable throwable) {
                            System.out.println("Failed");
                        }
                    }).get();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
