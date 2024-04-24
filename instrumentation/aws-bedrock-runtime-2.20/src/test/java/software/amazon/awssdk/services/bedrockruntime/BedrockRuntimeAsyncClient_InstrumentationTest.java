/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
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
import llm.models.ModelResponse;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static llm.events.LlmEvent.LLM_CHAT_COMPLETION_MESSAGE;
import static llm.events.LlmEvent.LLM_CHAT_COMPLETION_SUMMARY;
import static llm.events.LlmEvent.LLM_EMBEDDING;
import static llm.models.TestUtil.assertErrorEvent;
import static llm.models.TestUtil.assertLlmChatCompletionMessageAttributes;
import static llm.models.TestUtil.assertLlmChatCompletionSummaryAttributes;
import static llm.models.TestUtil.assertLlmEmbeddingAttributes;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClientMock.completionModelId;
import static software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClientMock.completionRequestInput;
import static software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClientMock.completionResponseContent;
import static software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClientMock.embeddingModelId;
import static software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClientMock.embeddingRequestInput;
import static software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClientMock.finishReason;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "software.amazon.awssdk.services.bedrockruntime" }, configName = "llm_enabled.yml")

public class BedrockRuntimeAsyncClient_InstrumentationTest {
    private static final BedrockRuntimeAsyncClientMock mockBedrockRuntimeAsyncClient = new BedrockRuntimeAsyncClientMock();
    private final Introspector introspector = InstrumentationTestRunner.getIntrospector();

    @Before
    public void before() {
        introspector.clear();
    }

    @Test
    public void testInvokeModelCompletion() throws ExecutionException, InterruptedException {
        boolean isError = false;
        InvokeModelRequest invokeModelRequest = buildAmazonTitanCompletionRequest(isError);
        InvokeModelResponse invokeModelResponse = invokeModelInTransaction(invokeModelRequest);

        assertNotNull(invokeModelResponse);
        assertTransaction(ModelResponse.COMPLETION);
        assertSupportabilityMetrics();
        assertLlmEvents(ModelResponse.COMPLETION);
        assertErrorEvent(isError, introspector.getErrorEvents());
    }

    @Test
    public void testInvokeModelEmbedding() throws ExecutionException, InterruptedException {
        boolean isError = false;
        InvokeModelRequest invokeModelRequest = buildAmazonTitanEmbeddingRequest(isError);
        InvokeModelResponse invokeModelResponse = invokeModelInTransaction(invokeModelRequest);

        assertNotNull(invokeModelResponse);
        assertTransaction(ModelResponse.EMBEDDING);
        assertSupportabilityMetrics();
        assertLlmEvents(ModelResponse.EMBEDDING);
        assertErrorEvent(isError, introspector.getErrorEvents());
    }

    @Test
    public void testInvokeModelCompletionError() throws ExecutionException, InterruptedException {
        boolean isError = true;
        InvokeModelRequest invokeModelRequest = buildAmazonTitanCompletionRequest(isError);
        InvokeModelResponse invokeModelResponse = invokeModelInTransaction(invokeModelRequest);

        assertNotNull(invokeModelResponse);
        assertTransaction(ModelResponse.COMPLETION);
        assertSupportabilityMetrics();
        assertLlmEvents(ModelResponse.COMPLETION);
        assertErrorEvent(isError, introspector.getErrorEvents());
    }

    @Test
    public void testInvokeModelEmbeddingError() throws ExecutionException, InterruptedException {
        boolean isError = true;
        InvokeModelRequest invokeModelRequest = buildAmazonTitanEmbeddingRequest(isError);
        InvokeModelResponse invokeModelResponse = invokeModelInTransaction(invokeModelRequest);

        assertNotNull(invokeModelResponse);
        assertTransaction(ModelResponse.EMBEDDING);
        assertSupportabilityMetrics();
        assertLlmEvents(ModelResponse.EMBEDDING);
        assertErrorEvent(isError, introspector.getErrorEvents());
    }

    private static InvokeModelRequest buildAmazonTitanCompletionRequest(boolean isError) {
        JSONObject textGenerationConfig = new JSONObject()
                .put("maxTokenCount", 1000)
                .put("stopSequences", Collections.singletonList("User:"))
                .put("temperature", 0.5)
                .put("topP", 0.9);

        String payload = new JSONObject()
                .put("inputText", completionRequestInput)
                .put("textGenerationConfig", textGenerationConfig)
                .put("errorTest", isError) // this is not a real model attribute, just adding for testing
                .toString();

        return InvokeModelRequest.builder()
                .body(SdkBytes.fromUtf8String(payload))
                .modelId(completionModelId)
                .contentType("application/json")
                .accept("application/json")
                .build();
    }

    private static InvokeModelRequest buildAmazonTitanEmbeddingRequest(boolean isError) {
        String payload = new JSONObject()
                .put("inputText", embeddingRequestInput)
                .put("errorTest", isError) // this is not a real model attribute, just adding for testing
                .toString();

        return InvokeModelRequest.builder()
                .body(SdkBytes.fromUtf8String(payload))
                .modelId(embeddingModelId)
                .contentType("application/json")
                .accept("application/json")
                .build();
    }

    @Trace(dispatcher = true)
    private InvokeModelResponse invokeModelInTransaction(InvokeModelRequest invokeModelRequest) throws ExecutionException, InterruptedException {
        NewRelic.addCustomParameter("llm.conversation_id", "conversation-id-value"); // Will be added to LLM events
        NewRelic.addCustomParameter("llm.testPrefix", "testPrefix"); // Will be added to LLM events
        NewRelic.addCustomParameter("test", "test"); // Will NOT be added to LLM events
        CompletableFuture<InvokeModelResponse> invokeModelResponseCompletableFuture = mockBedrockRuntimeAsyncClient.invokeModel(invokeModelRequest);
        return invokeModelResponseCompletableFuture.get();
    }

    private void assertTransaction(String operationType) {
        assertEquals(1, introspector.getFinishedTransactionCount(TimeUnit.SECONDS.toMillis(2)));
        Collection<String> transactionNames = introspector.getTransactionNames();
        String transactionName = transactionNames.iterator().next();
        Map<String, TracedMetricData> metrics = introspector.getMetricsForTransaction(transactionName);
        assertTrue(metrics.containsKey("Llm/" + operationType + "/Bedrock/invokeModel"));
        assertEquals(1, metrics.get("Llm/" + operationType + "/Bedrock/invokeModel").getCallCount());
    }

    private void assertSupportabilityMetrics() {
        Map<String, TracedMetricData> unscopedMetrics = introspector.getUnscopedMetrics();
        assertTrue(unscopedMetrics.containsKey("Supportability/Java/ML/Bedrock/2.20"));
    }

    private void assertLlmEvents(String operationType) {
        if (ModelResponse.COMPLETION.equals(operationType)) {
            // LlmChatCompletionMessage events
            Collection<Event> llmChatCompletionMessageEvents = introspector.getCustomEvents(LLM_CHAT_COMPLETION_MESSAGE);
            assertEquals(2, llmChatCompletionMessageEvents.size());

            Iterator<Event> llmChatCompletionMessageEventIterator = llmChatCompletionMessageEvents.iterator();
            // LlmChatCompletionMessage event for user request message
            Event llmChatCompletionMessageEventOne = llmChatCompletionMessageEventIterator.next();
            assertLlmChatCompletionMessageAttributes(llmChatCompletionMessageEventOne, completionModelId, completionRequestInput, completionResponseContent,
                    false);

            // LlmChatCompletionMessage event for assistant response message
            Event llmChatCompletionMessageEventTwo = llmChatCompletionMessageEventIterator.next();
            assertLlmChatCompletionMessageAttributes(llmChatCompletionMessageEventTwo, completionModelId, completionRequestInput, completionResponseContent,
                    true);

            // LlmCompletionSummary events
            Collection<Event> llmCompletionSummaryEvents = introspector.getCustomEvents(LLM_CHAT_COMPLETION_SUMMARY);
            assertEquals(1, llmCompletionSummaryEvents.size());

            Iterator<Event> llmCompletionSummaryEventIterator = llmCompletionSummaryEvents.iterator();
            // Summary event for both LlmChatCompletionMessage events
            Event llmCompletionSummaryEvent = llmCompletionSummaryEventIterator.next();
            assertLlmChatCompletionSummaryAttributes(llmCompletionSummaryEvent, completionModelId, finishReason);
        } else if (ModelResponse.EMBEDDING.equals(operationType)) {
            // LlmEmbedding events
            Collection<Event> llmEmbeddingEvents = introspector.getCustomEvents(LLM_EMBEDDING);
            assertEquals(1, llmEmbeddingEvents.size());

            Iterator<Event> llmEmbeddingEventIterator = llmEmbeddingEvents.iterator();
            // LlmEmbedding event
            Event llmEmbeddingEvent = llmEmbeddingEventIterator.next();
            assertLlmEmbeddingAttributes(llmEmbeddingEvent, embeddingModelId, embeddingRequestInput);
        }
    }
}
