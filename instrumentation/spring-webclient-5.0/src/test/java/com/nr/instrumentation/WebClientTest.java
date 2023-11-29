/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation;

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
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "org.springframework" })
public class WebClientTest {

    private static final int TIMEOUT = 3000;

    @ClassRule
    public static HttpServerRule server = new HttpServerRule();
    private static URI catEndpoint;
    private static URI nonCatEndpoint;
    private static String host;

    @BeforeClass
    public static void before() {
        // This is here to prevent reactor.util.ConsoleLogger output from taking over your screen
        System.setProperty("reactor.logging.fallback", "JDK");
        try {
            catEndpoint = server.getEndPoint();
            nonCatEndpoint = new URI(server.getEndPoint().toString() + "?no-transaction=true");
            host = catEndpoint.getHost();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    @AfterClass
    public static void after() {
        server.shutdown();
    }

    @Test
    public void testRequest() {
        final String response = makeGetRequest(nonCatEndpoint).block().bodyToMono(String.class).block();
        assertEquals("<html><body><h1>SuccessfulResponse</h1>\n</body></html>\n", response);

        String txnName = "OtherTransaction/Custom/com.nr.instrumentation.WebClientTest/makeGetRequest";
        validateOutcomes(txnName);
    }

    @Test
    public void testBaseUrlRequest() {
        final String response = makeBaseUrlGetRequest(nonCatEndpoint).block().bodyToMono(String.class).block();
        assertEquals("<html><body><h1>SuccessfulResponse</h1>\n</body></html>\n", response);

        String txnName = "OtherTransaction/Custom/com.nr.instrumentation.WebClientTest/makeBaseUrlGetRequest";
        validateOutcomes(txnName);
    }

    @Test
    public void testRetrieve() {
        final String response = makeRetrieveRequest(nonCatEndpoint).bodyToMono(String.class).block();
        assertEquals("<html><body><h1>SuccessfulResponse</h1>\n</body></html>\n", response);

        String txnName = "OtherTransaction/Custom/com.nr.instrumentation.WebClientTest/makeRetrieveRequest";
        validateOutcomes(txnName);
    }

    @Test
    public void testDelete() {
        final String response = makeDeleteRequest(nonCatEndpoint).block().bodyToMono(String.class).block();
        assertEquals("<html><body><h1>SuccessfulResponse</h1>\n</body></html>\n", response);

        String txnName = "OtherTransaction/Custom/com.nr.instrumentation.WebClientTest/makeDeleteRequest";
        validateOutcomes(txnName);
    }

    @Test
    public void testPost() {
        final String response = makePostRequest(nonCatEndpoint).block().bodyToMono(String.class).block();
        assertEquals("<html><body><h1>SuccessfulResponse</h1>\n</body></html>\n", response);

        String txnName = "OtherTransaction/Custom/com.nr.instrumentation.WebClientTest/makePostRequest";
        validateOutcomes(txnName);
    }

    @Test
    public void testError() throws URISyntaxException {
        URI fakeUri = new URI("http://www.notarealhostbrosef.bro");
        try {
            makeGetRequest(fakeUri).block().bodyToMono(String.class).block();
        } catch (Exception e) {
        }

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        final String txnName = introspector.getTransactionNames().iterator().next();
        // creates a scoped (and unscoped)
        Assert.assertEquals(1,
                MetricsHelper.getScopedMetricCount(txnName, "External/UnknownHost/Spring-WebClient/failed"));
        Assert.assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/UnknownHost/Spring-WebClient/failed"));

        // Unknown hosts generate no external rollups
        Assert.assertEquals(0, MetricsHelper.getUnscopedMetricCount("External/allOther"));
        Assert.assertEquals(0, MetricsHelper.getUnscopedMetricCount("External/all"));
    }

    @Test
    public void testCat() {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();

        final String response = makeCatRequest(catEndpoint).block().bodyToMono(String.class).block();
        assertEquals("<html><body><h1>SuccessfulResponse</h1>\n</body></html>\n", response);

        // transaction
        String txName = "OtherTransaction/Custom/com.nr.instrumentation.WebClientTest/makeCatRequest";
        assertEquals(2, introspector.getFinishedTransactionCount(
                TIMEOUT)); // One transaction is the one we care about and the other is the server-side CAT tx
        Collection<String> names = introspector.getTransactionNames();
        assertEquals(2, names.size());
        assertTrue(names.contains(server.getServerTransactionName()));
        assertTrue(names.contains(txName));

        // scoped metrics
        assertEquals(1, MetricsHelper.getScopedMetricCount(txName, "ExternalTransaction/" + host + "/"
                + server.getCrossProcessId() + "/" + server.getServerTransactionName()));
        assertEquals(1,
                MetricsHelper.getScopedMetricCount(txName, "Java/com.nr.instrumentation.WebClientTest/makeCatRequest"));

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
    public void testCatBaseUrl() {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();

        final String response = makeBaseUrlCatRequest(catEndpoint).block().bodyToMono(String.class).block();
        assertEquals("<html><body><h1>SuccessfulResponse</h1>\n</body></html>\n", response);

        // transaction
        String txName = "OtherTransaction/Custom/com.nr.instrumentation.WebClientTest/makeBaseUrlCatRequest";
        assertEquals(2, introspector.getFinishedTransactionCount(
                TIMEOUT)); // One transaction is the one we care about and the other is the server-side CAT tx
        Collection<String> names = introspector.getTransactionNames();
        assertEquals(2, names.size());
        assertTrue(names.contains(server.getServerTransactionName()));
        assertTrue(names.contains(txName));

        // scoped metrics
        assertEquals(1, MetricsHelper.getScopedMetricCount(txName, "ExternalTransaction/" + host + "/"
                + server.getCrossProcessId() + "/" + server.getServerTransactionName()));
        assertEquals(1,
                MetricsHelper.getScopedMetricCount(txName, "Java/com.nr.instrumentation.WebClientTest/makeBaseUrlCatRequest"));

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
    public Mono<ClientResponse> makeGetRequest(URI uri) {
        WebClient webClient = WebClient.builder().build();
        return webClient.get().uri(uri).exchange();
    }

    @Trace(dispatcher = true)
    public Mono<ClientResponse> makeBaseUrlGetRequest(URI uri) {
        WebClient webClient = WebClient.builder().baseUrl(uri.toString()).build();
        return webClient.get().exchange();
    }
    @Trace(dispatcher = true)
    public Mono<ClientResponse> makePostRequest(URI uri) {
        WebClient webClient = WebClient.builder().build();
        return webClient.post().uri(uri).exchange();
    }

    @Trace(dispatcher = true)
    public Mono<ClientResponse> makeDeleteRequest(URI uri) {
        WebClient webClient = WebClient.builder().build();
        return webClient.delete().uri(uri).exchange();
    }

    @Trace(dispatcher = true)
    public WebClient.ResponseSpec makeRetrieveRequest(URI uri) {
        WebClient webClient = WebClient.builder().build();
        return webClient.get().uri(uri).retrieve();
    }

    @Trace(dispatcher = true)
    public Mono<ClientResponse> makeCatRequest(URI uri) {
        WebClient webClient = WebClient.builder().build();
        return webClient.get().uri(uri).header(HttpTestServer.DO_CAT, "true").exchange();
    }

    @Trace(dispatcher = true)
    public Mono<ClientResponse> makeBaseUrlCatRequest(URI uri) {
        WebClient webClient = WebClient.builder().baseUrl(uri.toString()).build();
        return webClient.get().header(HttpTestServer.DO_CAT, "true").exchange();
    }

    private void validateOutcomes(String txnName) {
        final Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        assertTrue(introspector.getTransactionNames().contains(txnName));

        Collection<ExternalRequest> externalRequests = introspector.getExternalRequests(txnName);
        Assert.assertEquals(1, externalRequests.size());
        ExternalRequest externalRequest = externalRequests.iterator().next();
        Assert.assertEquals("Spring-WebClient", externalRequest.getLibrary());
        Assert.assertEquals("exchange", externalRequest.getOperation());

        Assert.assertEquals(1,
                MetricsHelper.getScopedMetricCount(txnName, "External/" + host + "/Spring-WebClient/exchange"));
        Assert.assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/" + host + "/Spring-WebClient/exchange"));

        // external rollups
        Assert.assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/" + host + "/all"));
        Assert.assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/allOther"));
        Assert.assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/all"));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(txnName);
        Assert.assertEquals(1, transactionEvents.size());
        TransactionEvent transactionEvent = transactionEvents.iterator().next();
        Assert.assertEquals(1, transactionEvent.getExternalCallCount());
        Assert.assertTrue(transactionEvent.getExternalDurationInSec() > 0);
    }
}