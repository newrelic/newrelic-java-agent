/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package test.newrelic.test.agent.spans;

import com.newrelic.agent.TracerList;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionData;
import com.newrelic.agent.TransactionDataList;
import com.newrelic.agent.errors.ErrorServiceImpl;
import com.newrelic.agent.errors.TracedError;
import com.newrelic.agent.interfaces.SamplingPriorityQueue;
import com.newrelic.agent.introspec.HttpTestServer;
import com.newrelic.agent.introspec.internal.HttpServerLocator;
import com.newrelic.agent.model.SpanCategory;
import com.newrelic.agent.model.SpanEvent;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.analytics.DistributedSamplingPriorityQueue;
import com.newrelic.agent.service.analytics.SpanEventsService;
import com.newrelic.agent.service.analytics.TransactionEvent;
import com.newrelic.agent.service.analytics.TransactionEventsService;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracing.DistributedTracePayloadImpl;
import com.newrelic.api.agent.*;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Assert;
import org.junit.Test;
import test.newrelic.EnvironmentHolderSettingsGenerator;
import test.newrelic.test.agent.EnvironmentHolder;
import test.newrelic.test.agent.api.ApiTestHelper;
import test.newrelic.test.agent.api.MessagingTestServer;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

import static org.junit.Assert.*;

public class SpanParentTest {
    private static final String CONFIG_FILE = "configs/span_events_test.yml";
    private static final ClassLoader CLASS_LOADER = SpanParentTest.class.getClassLoader();

    public EnvironmentHolder setupEnvironemntHolder(String environment) throws Exception {
        EnvironmentHolderSettingsGenerator envHolderSettings = new EnvironmentHolderSettingsGenerator(CONFIG_FILE, environment, CLASS_LOADER);
        EnvironmentHolder environmentHolder = new EnvironmentHolder(envHolderSettings);
        environmentHolder.setupEnvironment();
        return environmentHolder;
    }

    @Test
    public void testSpanParenting() throws Exception {
        EnvironmentHolder holder = setupEnvironemntHolder("all_enabled_test");
        Header actualHeader = txnStarter(false);

        try {
            TransactionDataList transactionList = holder.getTransactionList();
            ServiceFactory.getHarvestService().harvestNow();
            assertEquals(1, transactionList.size());
            Collection<Tracer> tracers = transactionList.get(0).getTracers();
            String expectedGuid = ((TracerList) tracers).get(0).getGuid();
            String actualGuid = findGuid(actualHeader.getValue());
            Assert.assertEquals(expectedGuid, actualGuid);
        } finally {
            holder.close();
        }
    }

    @Test
    public void testErrorAttributes() throws Exception {
        EnvironmentHolder holder = setupEnvironemntHolder("all_enabled_test");
        Header actualHeader = txnStarter(true);

        try {
            TransactionDataList transactionList = holder.getTransactionList();
            ServiceFactory.getHarvestService().harvestNow();
            assertEquals(1, transactionList.size());
            Collection<Tracer> tracers = transactionList.get(0).getTracers();
            Tracer tracer = ((TracerList) tracers).get(0);
            String expectedGuid = tracer.getGuid();
            String actualGuid = findGuid(actualHeader.getValue());
            Assert.assertEquals(expectedGuid, actualGuid);

            ErrorServiceImpl errorService = (ErrorServiceImpl) ServiceFactory.getRPMService().getErrorService();
            List<TracedError> tracedErrors = errorService.getAndClearTracedErrors();
            assertEquals(1, tracedErrors.size());
            TracedError errorTrace = tracedErrors.get(0);
            Map<String, ?> errorAtts = errorTrace.getIntrinsicAtts();
            Assert.assertNotNull(errorAtts.get("traceId"));
            Assert.assertNotNull(errorAtts.get("guid"));
            Assert.assertNotNull(errorAtts.get("priority"));
            Assert.assertNotNull(errorAtts.get("sampled"));
        } finally {
            holder.close();
        }
    }

    @Test
    public void testSpanAndTransactionParenting() throws Exception {
        EnvironmentHolder holder = setupEnvironemntHolder("all_enabled_test");
        executeSpanAndTransactionParentingTest();

        try {
            TransactionDataList transactionList = holder.getTransactionList();
            assertEquals(2, transactionList.size());
            TransactionData tx1 = transactionList.get(0);
            TransactionData tx2 = transactionList.get(1);
            Collection<Tracer> tracers1 = tx1.getTracers();
            assertEquals(0, tracers1.size()); // Only a "rootTracer" on this transaction (root tracer is not in this list)
            Collection<Tracer> tracers2 = tx2.getTracers();
            assertEquals(4, tracers2.size()); // 1 "rootTracer" (not in this list) + 2 non-external/datastore tracers + 2 external datastore tracers

            SpanEventsService spanEventsService = ServiceFactory.getServiceManager().getSpanEventsService();
            String appName = ServiceFactory.getConfigService().getDefaultAgentConfig().getApplicationName();
            SamplingPriorityQueue<SpanEvent> spanEventsPool = spanEventsService.getOrCreateDistributedSamplingReservoir(appName);
            assertNotNull(spanEventsPool);
            List<SpanEvent> spanEvents = spanEventsPool.asList();
            spanEventsPool.clear();
            assertNotNull(spanEvents);
            assertEquals(6, spanEvents.size());

            SpanEvent rootSpanEvent = null;
            Set<String> spanEventGuids = new HashSet<>();
            for (SpanEvent spanEvent : spanEvents) {
                if (spanEvent.getParentId() == null) {
                    rootSpanEvent = spanEvent;
                }
                spanEventGuids.add(spanEvent.getGuid());
            }
            assertNotNull(rootSpanEvent);
            assertEquals(6, spanEventGuids.size());

            // Ensure that spans are only parented to other spans
            for (SpanEvent spanEvent : spanEvents) {
                if (spanEvent.getParentId() == null) {
                    continue;
                }

                assertTrue(spanEventGuids.contains(spanEvent.getParentId()));
            }

            TransactionEventsService transactionEventsService = ServiceFactory.getServiceManager().getTransactionEventsService();
            DistributedSamplingPriorityQueue<TransactionEvent> txEventPool = transactionEventsService.getOrCreateDistributedSamplingReservoir(appName);
            assertNotNull(txEventPool);
            List<TransactionEvent> txEvents = txEventPool.asList();
            txEventPool.clear();
            assertNotNull(txEvents);
            assertEquals(2, txEvents.size());

            TransactionEvent parent = null;
            TransactionEvent child = null;
            for (TransactionEvent txEvent : txEvents) {
                if (txEvent.getParentId() == null) {
                    parent = txEvent;
                } else {
                    child = txEvent;
                }
            }

            assertNotNull(parent);
            assertNotNull(child);
            assertNull(parent.getParentId());
            assertEquals(parent.getGuid(), child.getParentId());
        } finally {
            holder.close();
        }
    }

    @Trace(dispatcher = true)
    private Header txnStarter(boolean noticeError) {
        Transaction txn = ServiceFactory.getServiceManager().getTransactionService().getTransaction(false);
        DistributedTracePayloadImpl payload = DistributedTracePayloadImpl.createDistributedTracePayload(null, txn.getGuid(), txn.getGuid(), 1.0f);
        txn.acceptDistributedTracePayload(payload.httpSafe());
        MessagingTestServer server = new MessagingTestServer(8093);
        Header header = null;

        if (noticeError) {
            NewRelic.noticeError(new RuntimeException("I am something"));
        }
        try {
            server.start();
            header = runTestMessagingAPI();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            server.stop();
        }
        return header;
    }

    @Trace
    private Header runTestMessagingAPI() {
        URL myURL;
        try {
            Thread.sleep(600);
            myURL = new URL("http://localhost:8093");
            HttpUriRequest request = RequestBuilder.get().setUri(myURL.toURI()).build();

            ApiTestHelper.OutboundWrapper outboundRequestWrapper = new ApiTestHelper.OutboundWrapper(request,
                    HeaderType.MESSAGE);

            // MessageProducer
            ExternalParameters messageProduceParameters = MessageProduceParameters
                    .library("JMS")
                    .destinationType(DestinationType.NAMED_QUEUE)
                    .destinationName("Message Destination")
                    .outboundHeaders(outboundRequestWrapper)
                    .build();

            NewRelic.getAgent().getTracedMethod().reportAsExternal(messageProduceParameters);
            Header[] headers = request.getHeaders("Newrelic");

            Assert.assertTrue(headers.length != 0);
            return headers[0];

        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
        return null;
    }

    @Trace(dispatcher = true)
    private void executeSpanAndTransactionParentingTest() throws URISyntaxException, IOException {
        try (HttpTestServer server = HttpServerLocator.createAndStart()) {
            nonExternalOrDatastore();
            executeHttpRequest(true, server.getEndPoint().getPort());
            executeFakeDatastoreRequest();
        }
    }

    @Trace
    private void nonExternalOrDatastore() {
        for (int i = 0; i < 1000; i++) {
            // This is here to test a tracer that isn't an external or datastore
        }
    }

    @Trace
    private void executeHttpRequest(boolean doBetterCAT, int port) {
        URL myURL;
        try {
            Thread.sleep(600);
            myURL = new URL("http://localhost:" + port);
            HttpUriRequest request = RequestBuilder.get()
                    .setUri(myURL.toURI())
                    .setHeader(HttpTestServer.DO_BETTER_CAT, Boolean.toString(doBetterCAT))
                    .build();

            CloseableHttpClient httpclient = HttpClients.createDefault();
            CloseableHttpResponse response = httpclient.execute(request);
            assertEquals(200, response.getStatusLine().getStatusCode());
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    @Trace
    private void executeFakeDatastoreRequest() {
        try {
            Thread.sleep(800);
            ExternalParameters datastoreParameters = DatastoreParameters
                    .product("FakeDB")
                    .collection("Collection")
                    .operation("Operation")
                    .noInstance()
                    .databaseName("DbName")
                    .noSlowQuery()
                    .build();
            NewRelic.getAgent().getTracedMethod().reportAsExternal(datastoreParameters);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    private String findGuid(String headerValue) throws ParseException {
        byte[] ar = Base64.getDecoder().decode(headerValue);
        String s = new String(ar);
        JSONParser parser = new JSONParser();
        JSONObject jsonObject = (JSONObject) parser.parse(s);
        JSONObject d = (JSONObject) jsonObject.get("d");
        return (String) d.get("id");
    }
}
