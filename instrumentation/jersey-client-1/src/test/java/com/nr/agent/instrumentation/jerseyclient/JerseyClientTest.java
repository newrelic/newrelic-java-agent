/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.jerseyclient;

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
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "com.sun.jersey" })
public class JerseyClientTest {
    @ClassRule
    public static HttpServerRule server = new HttpServerRule();

    @Test
    public void testError() {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();

        final String fakeUrl = "http://www.picturesofgeese.com";
        makeExternalRequest(fakeUrl, true);

        Assert.assertEquals(1, introspector.getFinishedTransactionCount(3000));

        // Unknown hosts generate no external rollups
        Assert.assertEquals(0, MetricsHelper.getUnscopedMetricCount("External/allOther"));
        Assert.assertEquals(0, MetricsHelper.getUnscopedMetricCount("External/all"));

    }

    @Test
    public void testHttpMethods() throws Exception {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();

        URI endpoint = server.getEndPoint();
        requestHttpMethods(endpoint.toURL().toString());

        // transaction
        String txName = "OtherTransaction/Custom/com.nr.agent.instrumentation.jerseyclient.JerseyClientTest/requestHttpMethods";
        assertEquals(1, introspector.getFinishedTransactionCount(3000));
        Collection<String> names = introspector.getTransactionNames();
        assertTrue(names.contains(txName));

        //external request test should go here
        Collection<ExternalRequest> externalRequests = InstrumentationTestRunner.getIntrospector()
                .getExternalRequests(txName);

        assertEquals(4, externalRequests.size());
        assertEquals(4, MetricsHelper.getUnscopedMetricCount("External/localhost/all"));
        assertEquals(4, MetricsHelper.getUnscopedMetricCount("External/allOther"));
        assertEquals(4, MetricsHelper.getUnscopedMetricCount("External/all"));
    }

    @Test
    public void testCat() throws Exception {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();

        URI endpoint = server.getEndPoint();
        String host = endpoint.getHost();
        makeExternalRequest(endpoint.toURL().toString(), true);

        // transaction
        String txName = "OtherTransaction/Custom/com.nr.agent.instrumentation.jerseyclient.JerseyClientTest/makeExternalRequest";
        assertEquals(2, introspector.getFinishedTransactionCount(3000));
        Collection<String> names = introspector.getTransactionNames();
        assertEquals(2, names.size());
        assertTrue(names.contains(server.getServerTransactionName()));
        assertTrue(names.contains(txName));

        // scoped metrics
        assertEquals(1, MetricsHelper.getScopedMetricCount(txName, "ExternalTransaction/" + host + "/"
                + server.getCrossProcessId() + "/" + server.getServerTransactionName()));
        assertEquals(1, MetricsHelper.getScopedMetricCount(txName,
                "Java/com.nr.agent.instrumentation.jerseyclient.JerseyClientTest/makeExternalRequest"));

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
            Client.create().resource(host)
                    .accept(MediaType.APPLICATION_JSON)
                    .header(HttpTestServer.DO_CAT, String.valueOf(doCat))
                    .get(ClientResponse.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Trace(dispatcher = true)
    public void requestHttpMethods(String host) throws InterruptedException {
        Client client = Client.create();
        StringBuilder hostUrl = new StringBuilder(host).append("?no-transaction");
        try {
            WebResource webResource = client.resource(hostUrl.toString());
            webResource.type("application/json").get(ClientResponse.class);
            webResource.type("application/json").post(ClientResponse.class);
            webResource.type("application/json").delete(ClientResponse.class);
            webResource.type("application/json").put(ClientResponse.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
