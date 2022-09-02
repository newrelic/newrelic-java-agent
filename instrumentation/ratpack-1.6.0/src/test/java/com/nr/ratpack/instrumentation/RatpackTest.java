package com.nr.ratpack.instrumentation;

import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.agent.introspec.TracedMetricData;
import com.newrelic.agent.introspec.TransactionEvent;
import com.newrelic.api.agent.Trace;
import com.nr.ratpack.instrumentation.handlers.AsyncFolk;
import com.nr.ratpack.instrumentation.handlers.BlockingFolk;
import com.nr.ratpack.instrumentation.handlers.BodyReader;
import com.nr.ratpack.instrumentation.handlers.ParallelBatchForEach;
import com.nr.ratpack.instrumentation.handlers.ParallelBatchHandler;
import com.nr.ratpack.instrumentation.handlers.SerialBatchHandlerNoError;
import com.nr.ratpack.instrumentation.handlers.SerialBatchHandlerWithError;
import com.nr.ratpack.instrumentation.handlers.Spectator;
import org.junit.Test;
import org.junit.runner.RunWith;
import ratpack.exec.Promise;
import ratpack.http.client.ReceivedResponse;
import ratpack.server.RatpackServer;
import ratpack.test.embed.EmbeddedApp;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "ratpack" })
public class RatpackTest {
    @Test
    public void testDefaultNullPromise() throws Exception {
        simplePromises();

        Introspector introspector = InstrumentationTestRunner.getIntrospector();

        int finishedTransactionCount = introspector.getFinishedTransactionCount(2000);
        assertEquals(1, finishedTransactionCount);
        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(
                "OtherTransaction/Custom/com.nr.ratpack.instrumentation.RatpackTest/simplePromises");
    }

    @Trace(dispatcher = true)
    private void simplePromises() throws Exception {
        final Promise<Object> nullPromise = Promise.ofNull();
        final Promise<String> stringPromise = Promise.value("String");
        final Promise<Boolean> booleanPromise = Promise.value(true);
        final Promise<Object> object = Promise.value(new HashMap<>());
    }

    @Test
    public void testHandlers() throws Exception {
        try (EmbeddedApp app = EmbeddedApp.fromHandlers(
                chain -> chain.all(new ParallelBatchHandler())
                              .all(new ParallelBatchForEach())
                              .all(new Spectator())
                              .all(new BodyReader())
                              .all(new BlockingFolk())
                              .all(new AsyncFolk()))) {

            RatpackServer server = app.getServer();
            server.start();

            ReceivedResponse response = app.getHttpClient().get();
            assertEquals(200, response.getStatusCode());
            assertEquals("yes", response.getBody().getText());

            server.stop();

            Introspector introspector = InstrumentationTestRunner.getIntrospector();
            Collection<String> transactionNames = introspector.getTransactionNames();

            int finishedTransactionCount = introspector.getFinishedTransactionCount(2000);
            assertEquals(1, finishedTransactionCount);

            // This name is set by the Spectator handler
            String expectedTxnName = "WebTransaction/Ratpack/spectator";
            assertTrue(transactionNames.contains(expectedTxnName));

            Map<String, TracedMetricData> metrics = introspector.getMetricsForTransaction(expectedTxnName);

            // Instrumentation point that starts the transaction
            assertTrue(metrics.containsKey("Java/ratpack.server.internal.NettyHandlerAdapter/newRequest"));

            // Check that we linked the hop from ratpack.server.internal.NettyHandlerAdapter to the chain of handlers
            assertTrue(metrics.containsKey("DefaultContext.next()"));

            assertEquals(1, metrics.get("Java/com.nr.ratpack.instrumentation.handlers.Spectator/handle").getCallCount());
            assertEquals(1, metrics.get("Java/com.nr.ratpack.instrumentation.handlers.BodyReader/handle").getCallCount());
            assertEquals(1, metrics.get("Java/com.nr.ratpack.instrumentation.handlers.BlockingFolk/handle").getCallCount());
            assertEquals(1, metrics.get("Java/com.nr.ratpack.instrumentation.handlers.AsyncFolk/handle").getCallCount());

            Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(expectedTxnName);
            assertFalse(transactionEvents.isEmpty());
            TransactionEvent event = transactionEvents.iterator().next();
            Map<String, Object> attributes = event.getAttributes();

            assertNotNull(attributes.get("request.method"));
            assertNotNull(attributes.get("request.headers.host"));
            assertNotNull(attributes.get("request.uri"));

            assertNotNull(attributes.get("httpResponseCode"));
            assertNotNull(attributes.get("response.headers.contentType"));

            assertEquals("yes", attributes.get("SpectatorHandler"));
            assertEquals("yes", attributes.get("BodyReaderHandler"));
            assertEquals("yes", attributes.get("BlockingFolkHandler"));
            assertEquals("yes", attributes.get("AsyncFolkHandler"));

            // Async work in handlers
            assertEquals("yes", attributes.get("BodyReaderHandlerThen"));
            assertEquals("yes", attributes.get("AsyncFolkHandlerUpstream"));
            assertEquals("yes", attributes.get("AsyncFolkHandlerThen"));

            // ParallelBatch async handling
            assertEquals("yes", attributes.get("ParallelBatch1"));
            assertEquals("yes", attributes.get("ParallelBatch2"));
            assertEquals("yes", attributes.get("ParallelBatch3"));
            assertEquals("yes", attributes.get("ParallelBatch4"));
            assertEquals("yes", attributes.get("ParallelBatch5"));
            assertEquals("yes", attributes.get("ParallelBatch6"));
            assertEquals("yes", attributes.get("ParallelBatch7"));
            assertNull(attributes.get("ParallelBatch8"));
        }
    }

    @Test
    public void serialBatchWithError() throws Exception {
        try (EmbeddedApp app = EmbeddedApp.fromHandlers(
                chain -> chain.all(new SerialBatchHandlerWithError()))) {

            RatpackServer server = app.getServer();
            server.start();

            ReceivedResponse response = app.getHttpClient().get();
            assertEquals(200, response.getStatusCode());
            assertEquals("success", response.getBody().getText());

            server.stop();

            Introspector introspector = InstrumentationTestRunner.getIntrospector();
            Collection<String> transactionNames = introspector.getTransactionNames();

            int finishedTransactionCount = introspector.getFinishedTransactionCount(2000);
            assertEquals(1, finishedTransactionCount);

            String expectedTxnName = "WebTransaction/Ratpack/serialBatchWithError";
            assertTrue(transactionNames.contains(expectedTxnName));

            Map<String, TracedMetricData> metrics = introspector.getMetricsForTransaction(expectedTxnName);

            assertEquals(1, metrics.get("Java/com.nr.ratpack.instrumentation.handlers.SerialBatchHandlerWithError/handle").getCallCount());

            Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(expectedTxnName);
            assertFalse(transactionEvents.isEmpty());
            TransactionEvent event = transactionEvents.iterator().next();
            Map<String, Object> attributes = event.getAttributes();

            assertEquals("true", attributes.get("SerialBatchHandlerWithError1"));
            assertEquals("true", attributes.get("SerialBatchHandlerWithErrorBang"));
            assertNull(attributes.get("SerialBatchHandlerWithError2"));
        }
    }

    @Test
    public void serialBatchNoError() throws Exception {
        try (EmbeddedApp app = EmbeddedApp.fromHandlers(
                chain -> chain.all(new SerialBatchHandlerNoError()))) {

            RatpackServer server = app.getServer();
            server.start();

            ReceivedResponse response = app.getHttpClient().get();
            assertEquals(200, response.getStatusCode());
            assertEquals("success", response.getBody().getText());

            server.stop();

            Introspector introspector = InstrumentationTestRunner.getIntrospector();
            Collection<String> transactionNames = introspector.getTransactionNames();

            int finishedTransactionCount = introspector.getFinishedTransactionCount(2000);
            assertEquals(1, finishedTransactionCount);

            String expectedTxnName = "WebTransaction/Ratpack/serialBatchNoError";
            assertTrue(transactionNames.contains(expectedTxnName));

            Map<String, TracedMetricData> metrics = introspector.getMetricsForTransaction(expectedTxnName);

            assertEquals(1, metrics.get("Java/com.nr.ratpack.instrumentation.handlers.SerialBatchHandlerNoError/handle").getCallCount());

            Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(expectedTxnName);
            assertFalse(transactionEvents.isEmpty());
            TransactionEvent event = transactionEvents.iterator().next();
            Map<String, Object> attributes = event.getAttributes();

            assertEquals("true", attributes.get("SerialBatchHandlerNoError1"));
            assertEquals("true", attributes.get("SerialBatchHandlerNoError2"));
            assertEquals("true", attributes.get("SerialBatchAllResults"));
        }
    }

}
