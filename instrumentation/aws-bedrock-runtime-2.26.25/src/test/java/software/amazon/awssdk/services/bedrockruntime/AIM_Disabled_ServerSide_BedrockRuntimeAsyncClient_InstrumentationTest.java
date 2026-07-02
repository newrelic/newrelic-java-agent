/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package software.amazon.awssdk.services.bedrockruntime;

import com.newrelic.agent.introspec.Event;
import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.agent.introspec.TracedMetricData;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseResponse;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseStreamRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseStreamResponseHandler;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static llm.events.LlmEvent.LLM_CHAT_COMPLETION_MESSAGE;
import static llm.events.LlmEvent.LLM_CHAT_COMPLETION_SUMMARY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static software.amazon.awssdk.services.bedrockruntime.MockConverseRequest.converseRequest;
import static software.amazon.awssdk.services.bedrockruntime.MockConverseRequest.converseStreamRequest;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "software.amazon.awssdk.services.bedrockruntime" }, configName = "llm_disabled_server_side.yml")

public class AIM_Disabled_ServerSide_BedrockRuntimeAsyncClient_InstrumentationTest {
    private static final BedrockRuntimeAsyncClientMock mockBedrockRuntimeAsyncClient = new BedrockRuntimeAsyncClientMock();
    private final Introspector introspector = InstrumentationTestRunner.getIntrospector();

    @Before
    public void before() {
        introspector.clear();
    }

    @Test
    public void testConverseAsyncCompletion() throws ExecutionException, InterruptedException {
        boolean isError = false; // TODO might need to add a custom error flag on the request
        ConverseResponse converseResponse = converseAsyncRequestInTransaction(converseRequest());
        assertNotNull(converseResponse);
        assertNoLlmTransaction();
        assertNoLlmSupportabilityMetrics();
        assertNoLlmEvents();
        assertTrue(introspector.getErrorEvents().isEmpty());
    }

    @Test
    public void testConverseAsyncCompletionError() throws ExecutionException, InterruptedException {
        boolean isError = true; // TODO might need to add a custom error flag on the request
        ConverseResponse converseResponse = converseAsyncRequestInTransaction(converseRequest());
        assertNotNull(converseResponse);
        assertNoLlmTransaction();
        assertNoLlmSupportabilityMetrics();
        assertNoLlmEvents();
        assertTrue(introspector.getErrorEvents().isEmpty());
    }

    @Trace(dispatcher = true)
    private ConverseResponse converseAsyncRequestInTransaction(ConverseRequest converseRequest) throws ExecutionException, InterruptedException {
        addCustomParameters();
        CompletableFuture<ConverseResponse> converseResponseFuture = mockBedrockRuntimeAsyncClient.converse(converseRequest);
        return converseResponseFuture.get();
    }

    // TODO Stream support not implemented
//    @Test
//    public void testConverseStreamCompletion() throws ExecutionException, InterruptedException {
//        boolean isError = false; // TODO might need to add a custom error flag on the request
//        Void unused = converseStreamRequestInTransaction(converseStreamRequest());
////        assertNotNull(converseResponse);
//        assertNoLlmTransaction();
//        assertNoLlmSupportabilityMetrics();
//        assertNoLlmEvents();
//        assertTrue(introspector.getErrorEvents().isEmpty());
//    }

    // TODO Stream support not implemented
//    @Test
//    public void testConverseStreamCompletionError() throws ExecutionException, InterruptedException {
//        boolean isError = true; // TODO might need to add a custom error flag on the request
//        Void unused = converseStreamRequestInTransaction(converseStreamRequest());
////        assertNotNull(converseResponse);
//        assertNoLlmTransaction();
//        assertNoLlmSupportabilityMetrics();
//        assertNoLlmEvents();
//        assertTrue(introspector.getErrorEvents().isEmpty());
//    }

    // TODO Stream support not implemented
//    @Trace(dispatcher = true)
//    private Void converseStreamRequestInTransaction(ConverseStreamRequest converseStreamRequest) throws ExecutionException, InterruptedException {
//        addCustomParameters();
//        ConverseStreamResponseHandler converseStreamResponseHandler = ConverseStreamResponseHandler.builder().build();
//        CompletableFuture<Void> converseResponseFuture = mockBedrockRuntimeAsyncClient.converseStream(converseStreamRequest, converseStreamResponseHandler);
//        return converseResponseFuture.get();
//    }

    private void addCustomParameters() {
        NewRelic.addCustomParameter("llm.conversation_id", "conversation-id-value"); // Will be added to LLM events
        NewRelic.addCustomParameter("llm.testPrefix", "testPrefix"); // Will be added to LLM events
        NewRelic.addCustomParameter("test", "test"); // Will NOT be added to LLM events
    }

    private void assertNoLlmTransaction() {
        assertEquals(1, introspector.getFinishedTransactionCount(TimeUnit.SECONDS.toMillis(2)));
        Collection<String> transactionNames = introspector.getTransactionNames();
        String transactionName = transactionNames.iterator().next();
        Map<String, TracedMetricData> metrics = introspector.getMetricsForTransaction(transactionName);
        assertFalse(metrics.containsKey("Llm/completion/Bedrock/converse"));
    }

    private void assertNoLlmSupportabilityMetrics() {
        Map<String, TracedMetricData> unscopedMetrics = introspector.getUnscopedMetrics();
        assertFalse(unscopedMetrics.containsKey("Supportability/Java/ML/Bedrock/2.26.25"));
    }

    private void assertNoLlmEvents() {
        // LlmChatCompletionMessage events
        Collection<Event> llmChatCompletionMessageEvents = introspector.getCustomEvents(LLM_CHAT_COMPLETION_MESSAGE);
        assertEquals(0, llmChatCompletionMessageEvents.size());

        // LlmCompletionSummary events
        Collection<Event> llmCompletionSummaryEvents = introspector.getCustomEvents(LLM_CHAT_COMPLETION_SUMMARY);
        assertEquals(0, llmCompletionSummaryEvents.size());
    }
}
