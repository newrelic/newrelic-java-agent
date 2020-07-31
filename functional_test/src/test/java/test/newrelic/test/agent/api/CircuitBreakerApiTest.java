/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package test.newrelic.test.agent.api;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionData;
import com.newrelic.agent.TransactionListener;
import com.newrelic.agent.circuitbreaker.CircuitBreakerService;
import com.newrelic.agent.environment.AgentIdentity;
import com.newrelic.agent.errors.ErrorService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.ServiceManager;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.DestinationType;
import com.newrelic.api.agent.GenericParameters;
import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.MessageConsumeParameters;
import com.newrelic.api.agent.MessageProduceParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.QueryConverter;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.TracedMethod;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.bson.BsonBoolean;
import org.bson.BsonDocument;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import static org.mockito.Mockito.spy;

/**
 * Functional tests for Fit to Public API when Circuit Breaker is tripped
 */
public class CircuitBreakerApiTest implements TransactionListener {
    ApiTestHelper apiTestHelper = new ApiTestHelper();

    @Before
    public void before() {
        apiTestHelper.serviceManager = ServiceFactory.getServiceManager();
        apiTestHelper.tranStats = null;
        ServiceFactory.getTransactionService().addTransactionListener(this);
    }

    @After
    public void after() {
        Transaction.clearTransaction();

        ServiceFactory.setServiceManager(apiTestHelper.serviceManager);

        ErrorService errorService = ServiceFactory.getRPMService().getErrorService();
        errorService.getAndClearTracedErrors();

        ServiceFactory.getStatsService().getStatsEngineForHarvest(null).clear();
        ServiceFactory.getTransactionService().addTransactionListener(this);
    }

    @Override
    public void dispatcherTransactionFinished(TransactionData transactionData, TransactionStats transactionStats) {
        apiTestHelper.tranStats = transactionStats;
    }

    /* Web Frameworks - FIT to Public API */

    @Test
    public void testWebFrameworkAPI() {
        try {
            tripCircuitBreaker();
            runTestWebFrameworkAPI();
            Assert.assertNull(apiTestHelper.tranStats);
        } finally {
            Transaction.clearTransaction();
        }
    }

    @Trace(dispatcher = true)
    private void runTestWebFrameworkAPI() {
        NewRelic.setAppServerPort(666);
        NewRelic.setInstanceName("instance");
        NewRelic.setServerInfo("server", "6.6.6");

        AgentIdentity env = ServiceFactory.getEnvironmentService().getEnvironment().getAgentIdentity();

        int port = env.getServerPort();
        String instance = env.getInstanceName();
        String dispatcher = env.getDispatcher();
        String version = env.getDispatcherVersion();

        Assert.assertEquals(666, port);
        Assert.assertEquals("instance", instance);
        Assert.assertEquals("server", dispatcher);
        Assert.assertEquals("6.6.6", version);
    }

    /* External - FIT to Public API */

    @Test
    public void testExternalAPI() {
        try {
            tripCircuitBreaker();
            runTestExternalAPI();
        } finally {
            Transaction.clearTransaction();
        }
    }

    @Trace(dispatcher = true)
    private void runTestExternalAPI() {
        String library = "HttpClient";
        URI uri = null;
        try {
            uri = new URI("http://localhost:8080/test/this/path?name=Bob");
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        String operation = "execute";

        NewRelic.getAgent().getTracedMethod().reportAsExternal(GenericParameters
                .library(library)
                .uri(uri)
                .procedure(operation)
                .build());
        Tracer rootTracer = Transaction.getTransaction().getRootTracer();

        Assert.assertNull(rootTracer);
    }

    /* External/CAT - FIT to Public API */

    @Test
    public void testExternalCatAPI() {
        tripCircuitBreaker();
        runTestExternalCatAPI();
        Transaction.clearTransaction();
    }

    @Trace(dispatcher = true)
    private void runTestExternalCatAPI() {

        URL myURL = null;
        try {
            Thread.sleep(1000);
            myURL = new URL("http://localhost:8080");
            HttpUriRequest request = RequestBuilder.get().setUri(myURL.toURI()).build();

            ApiTestHelper.OutboundWrapper outboundWrapper = new ApiTestHelper.OutboundWrapper(request, HeaderType.HTTP);
            TracedMethod tracedMethod = NewRelic.getAgent().getTracedMethod();
            tracedMethod.addOutboundRequestHeaders(outboundWrapper);

            Assert.assertTrue(request.getHeaders("X-NewRelic-ID").length == 0);

            ApiTestHelper.DummyRequest incomingRequest = new ApiTestHelper.DummyRequest(HeaderType.HTTP);
            ApiTestHelper.DummyResponse response = new ApiTestHelper.DummyResponse(HeaderType.HTTP);

            NewRelic.getAgent().getTransaction().setWebRequest(incomingRequest);
            NewRelic.getAgent().getTransaction().setWebResponse(response);
            NewRelic.getAgent().getTransaction().addOutboundResponseHeaders();
            NewRelic.getAgent().getTransaction().markResponseSent();

            Assert.assertFalse(response.didSetHeader());
            Tracer rootTracer = Transaction.getTransaction().getRootTracer();
            Assert.assertNull(rootTracer);

        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    /* Datastore - FIT to Public API */

    @Test
    public void testDatastoreAPI() {
        try {
            tripCircuitBreaker();
            runTestDatastoreAPI();
            Assert.assertNull(apiTestHelper.tranStats);
        } finally {
            com.newrelic.agent.Transaction.clearTransaction();
        }
    }

    @Trace(dispatcher = true)
    private void runTestDatastoreAPI() {
        String vendor = "MongoDB";
        String collection = "Users";
        String operation = "SELECT";
        String host = "awesome-host";
        Integer port = 27017;

        NewRelic.getAgent().getTracedMethod().reportAsExternal(DatastoreParameters
                .product(vendor)
                .collection(collection)
                .operation(operation)
                .instance(host, port)
                .build());
    }

    /* Datastore/Slow Query - FIT to Public API */

    @Test
    public void testDatastoreSlowQueryAPI() {
        try {
            tripCircuitBreaker();
            runTestDatastoreSlowQueryAPI();
            Assert.assertNull(apiTestHelper.tranStats);
        } finally {
            com.newrelic.agent.Transaction.clearTransaction();
        }
    }

    @Trace(dispatcher = true)
    private void runTestDatastoreSlowQueryAPI() {
        String vendor = "MongoDB";
        String collection = "Users";
        String operation = "SELECT";
        String host = "awesome-host";
        Integer port = 27017;

        BsonDocument rawQuery = new BsonDocument("key", new BsonBoolean(true)); // the raw query object
        QueryConverter<BsonDocument> MONGO_QUERY_CONVERTER = new QueryConverter<BsonDocument>() {
            @Override
            public String toRawQueryString(BsonDocument query) {
                return query.toString();
            }

            @Override
            public String toObfuscatedQueryString(BsonDocument query) {
                return query.toString();
            }
        };

        NewRelic.getAgent().getTracedMethod().reportAsExternal(DatastoreParameters
                .product(vendor)
                .collection(collection)
                .operation(operation)
                .instance(host, port)
                .noDatabaseName()
                .slowQuery(rawQuery, MONGO_QUERY_CONVERTER)
                .build());

        try {
            Thread.sleep(600);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /* Messaging - FIT to Public API */

    @Test
    public void testMessagingAPI() {
        tripCircuitBreaker();
        runTestMessagingAPI();
        com.newrelic.agent.Transaction.clearTransaction();
    }

    @Trace(dispatcher = true)
    private void runTestMessagingAPI() {
        URL myURL = null;
        try {
            Thread.sleep(600);
            myURL = new URL("http://localhost:8080");
            HttpUriRequest request = RequestBuilder.get().setUri(myURL.toURI()).build();

            ApiTestHelper.OutboundWrapper outboundRequestWrapper = new ApiTestHelper.OutboundWrapper(request, HeaderType.MESSAGE);

            // MessageProducer
            NewRelic.getAgent().getTracedMethod().reportAsExternal(MessageProduceParameters
                    .library("JMS")
                    .destinationType(DestinationType.NAMED_QUEUE)
                    .destinationName("Message Destination")
                    .outboundHeaders(outboundRequestWrapper)
                    .build());

            Assert.assertTrue(request.getHeaders("NewRelicID").length == 0);
            Assert.assertTrue(request.getHeaders("NewRelicTransaction").length == 0);

            // MessageConsumer/Listener (serverside)
            ApiTestHelper.DummyRequest inboundRequest = new ApiTestHelper.DummyRequest(HeaderType.MESSAGE);
            NewRelic.getAgent().getTracedMethod().reportAsExternal(MessageConsumeParameters
                    .library("JMS")
                    .destinationType(DestinationType.NAMED_QUEUE)
                    .destinationName("Message Destination")
                    .inboundHeaders(inboundRequest)
                    .build());

            // MessageProducer (serverside)
            ApiTestHelper.DummyResponse outboundResponse = new ApiTestHelper.DummyResponse(HeaderType.MESSAGE);
            NewRelic.getAgent().getTracedMethod().reportAsExternal(MessageProduceParameters
                    .library("JMS")
                    .destinationType(DestinationType.TEMP_QUEUE)
                    .destinationName("Message Destination")
                    .outboundHeaders(outboundRequestWrapper)
                    .build());

            // MessageConsumer/Listener
            ApiTestHelper.DummyRequest inboundResponse = new ApiTestHelper.DummyRequest(HeaderType.MESSAGE);
            NewRelic.getAgent().getTracedMethod().reportAsExternal(MessageConsumeParameters
                    .library("JMS")
                    .destinationType(DestinationType.TEMP_QUEUE)
                    .destinationName("Message Destination")
                    .inboundHeaders(inboundRequest)
                    .build());

            Assert.assertFalse(inboundRequest.didGetHeader());
            Assert.assertFalse(outboundResponse.didSetHeader());
            Assert.assertFalse(inboundResponse.didGetHeader());

        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    @Test
    public void testIgnore() throws Exception {
        tripCircuitBreaker();
        NewRelic.ignoreTransaction();
    }

    @Test
    public void testIgnoreApdex() throws Exception {
        tripCircuitBreaker();
        NewRelic.ignoreApdex();
    }

    @Test
    public void testNoticeError() throws Exception {
        tripCircuitBreaker();
        NewRelic.noticeError("myError");
    }

    @Test
    public void testSetTransactionName() throws Exception {
        tripCircuitBreaker();
        Transaction.clearTransaction();
        // Test DummyTransaction
        NewRelic.setTransactionName("MyCategory", "Name");
    }

    @Test
    public void testGetTransaction() throws Exception {
        tripCircuitBreaker();
        Transaction.clearTransaction();

        com.newrelic.api.agent.Transaction noOpTransaction = NewRelic.getAgent().getTransaction();
        Assert.assertNotNull(noOpTransaction);
    }

    @Test
    public void testCAT() throws Exception {
        tripCircuitBreaker();

        // Invocation point starts a transaction. We want to test NoOpTransaction in
        // action.
        Transaction.clearTransaction();
        NewRelic.getAgent().getTransaction().processRequestMetadata("123456");

        Assert.assertNull(NewRelic.getAgent().getTransaction().getResponseMetadata());
        Assert.assertNull(NewRelic.getAgent().getTransaction().getRequestMetadata());
    }

    private void tripCircuitBreaker() {
        try {
            ApiTestHelper.mockOutServiceManager();
        } catch (Exception e) {
            e.printStackTrace();
        }
        ServiceManager mockServiceManager = ServiceFactory.getServiceManager();
        
        CircuitBreakerService circuitBreakerService = spy(mockServiceManager.getCircuitBreakerService());
        Mockito.doReturn(true).when(circuitBreakerService).isTripped();
        Mockito.doReturn(true).when(circuitBreakerService).checkAndTrip();
        Mockito.doReturn(circuitBreakerService).when(mockServiceManager).getCircuitBreakerService();
    }
}
