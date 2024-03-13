/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package software.amazon.awssdk.services.bedrockruntime;

import com.newrelic.agent.introspec.ErrorEvent;
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
import java.util.concurrent.TimeUnit;

import static llm.events.LlmEvent.LLM_CHAT_COMPLETION_MESSAGE;
import static llm.events.LlmEvent.LLM_CHAT_COMPLETION_SUMMARY;
import static llm.events.LlmEvent.LLM_EMBEDDING;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "software.amazon.awssdk.services.bedrockruntime" }, configName = "llm_enabled.yml")

public class BedrockRuntimeClient_InstrumentationTest {
    private static final BedrockRuntimeClientMock mockBedrockRuntimeClient = new BedrockRuntimeClientMock();
    private final Introspector introspector = InstrumentationTestRunner.getIntrospector();

    @Before
    public void before() {
        introspector.clear();
    }

    @Test
    public void testInvokeModelCompletion() {
        boolean isError = false;
        InvokeModelRequest invokeModelRequest = buildAnthropicClaudeCompletionRequest(isError);
        InvokeModelResponse invokeModelResponse = invokeModelInTransaction(invokeModelRequest);

        assertNotNull(invokeModelResponse);

        verifyTransactionResults(ModelResponse.COMPLETION);
        verifySupportabilityMetricResults();
        verifyEventResults(ModelResponse.COMPLETION);
        verifyErrorResults(isError);
    }

    @Test
    public void testInvokeModelEmbedding() {
        boolean isError = false;
        InvokeModelRequest invokeModelRequest = buildAmazonTitanEmbeddingRequest(isError);
        InvokeModelResponse invokeModelResponse = invokeModelInTransaction(invokeModelRequest);

        assertNotNull(invokeModelResponse);

        verifyTransactionResults(ModelResponse.EMBEDDING);
        verifySupportabilityMetricResults();
        verifyEventResults(ModelResponse.EMBEDDING);
        verifyErrorResults(isError);
    }

    @Test
    public void testInvokeModelCompletionError() {
        boolean isError = true;
        InvokeModelRequest invokeModelRequest = buildAnthropicClaudeCompletionRequest(isError);
        InvokeModelResponse invokeModelResponse = invokeModelInTransaction(invokeModelRequest);

        assertNotNull(invokeModelResponse);

        verifyTransactionResults(ModelResponse.COMPLETION);
        verifySupportabilityMetricResults();
        verifyEventResults(ModelResponse.COMPLETION);
        verifyErrorResults(isError);
    }

    @Test
    public void testInvokeModelEmbeddingError() {
        boolean isError = true;
        InvokeModelRequest invokeModelRequest = buildAmazonTitanEmbeddingRequest(isError);
        InvokeModelResponse invokeModelResponse = invokeModelInTransaction(invokeModelRequest);

        assertNotNull(invokeModelResponse);

        verifyTransactionResults(ModelResponse.EMBEDDING);
        verifySupportabilityMetricResults();
        verifyEventResults(ModelResponse.EMBEDDING);
        verifyErrorResults(isError);
    }

    private static InvokeModelRequest buildAnthropicClaudeCompletionRequest(boolean isError) {
        String prompt = "Human: What is the color of the sky?\n\nAssistant:";
        String modelId = "anthropic.claude-v2";

        String payload = new JSONObject()
                .put("prompt", prompt)
                .put("max_tokens_to_sample", 1000)
                .put("temperature", 0.5)
                .put("stop_sequences", Collections.singletonList("\n\nHuman:"))
                .put("errorTest", isError) // this is not a real model attribute, just adding for testing
                .toString();

        return InvokeModelRequest.builder()
                .body(SdkBytes.fromUtf8String(payload))
                .modelId(modelId)
                .contentType("application/json")
                .accept("application/json")
                .build();
    }

    private static InvokeModelRequest buildAmazonTitanEmbeddingRequest(boolean isError) {
        String prompt = "{\"inputText\":\"What is the color of the sky?\"}";
        String modelId = "amazon.titan-embed-text-v1";

        String payload = new JSONObject()
                .put("inputText", prompt)
                .put("errorTest", isError) // this is not a real model attribute, just adding for testing
                .toString();

        return InvokeModelRequest.builder()
                .body(SdkBytes.fromUtf8String(payload))
                .modelId(modelId)
                .contentType("application/json")
                .accept("application/json")
                .build();
    }

    @Trace(dispatcher = true)
    private InvokeModelResponse invokeModelInTransaction(InvokeModelRequest invokeModelRequest) {
        NewRelic.addCustomParameter("llm.conversation_id", "conversation-id-value"); // Will be added to LLM events
        NewRelic.addCustomParameter("llm.testPrefix", "testPrefix"); // Will be added to LLM events
        NewRelic.addCustomParameter("test", "test"); // Will NOT be added to LLM events
        return mockBedrockRuntimeClient.invokeModel(invokeModelRequest);
    }

    private void verifyTransactionResults(String operationType) {
        assertEquals(1, introspector.getFinishedTransactionCount(TimeUnit.SECONDS.toMillis(2)));
        Collection<String> transactionNames = introspector.getTransactionNames();
        String transactionName = transactionNames.iterator().next();
        Map<String, TracedMetricData> metrics = introspector.getMetricsForTransaction(transactionName);
        assertTrue(metrics.containsKey("Llm/" + operationType + "/Bedrock/invokeModel"));
        assertEquals(1, metrics.get("Llm/" + operationType + "/Bedrock/invokeModel").getCallCount());
    }

    private void verifySupportabilityMetricResults() {
        Map<String, TracedMetricData> unscopedMetrics = introspector.getUnscopedMetrics();
        assertTrue(unscopedMetrics.containsKey("Supportability/Java/ML/Bedrock/2.20"));
    }

    private void verifyErrorResults(boolean isError) {
        Collection<ErrorEvent> errorEvents = introspector.getErrorEvents();
        if (isError) {
            assertEquals(1, errorEvents.size());
            Iterator<ErrorEvent> errorEventIterator = errorEvents.iterator();
            ErrorEvent errorEvent = errorEventIterator.next();

            assertEquals("LlmError: BAD_REQUEST", errorEvent.getErrorClass());
            assertEquals("LlmError: BAD_REQUEST", errorEvent.getErrorMessage());

            Map<String, Object> errorEventAttributes = errorEvent.getAttributes();
            assertFalse(errorEventAttributes.isEmpty());
            assertEquals(400, errorEventAttributes.get("error.code"));
            assertEquals(400, errorEventAttributes.get("http.statusCode"));
        } else {
            assertTrue(errorEvents.isEmpty());
        }
        Map<String, TracedMetricData> unscopedMetrics = introspector.getUnscopedMetrics();
        assertTrue(unscopedMetrics.containsKey("Supportability/Java/ML/Bedrock/2.20"));
    }

    private void verifyEventResults(String operationType) {
        if (ModelResponse.COMPLETION.equals(operationType)) {
            // LlmChatCompletionMessage events
            Collection<Event> llmChatCompletionMessageEvents = introspector.getCustomEvents(LLM_CHAT_COMPLETION_MESSAGE);
            assertEquals(2, llmChatCompletionMessageEvents.size());

            Iterator<Event> llmChatCompletionMessageEventIterator = llmChatCompletionMessageEvents.iterator();
            // LlmChatCompletionMessage event for user request message
            Event llmChatCompletionMessageEventOne = llmChatCompletionMessageEventIterator.next();
            assertLlmChatCompletionMessageAttributes(llmChatCompletionMessageEventOne, false);

            // LlmChatCompletionMessage event for assistant response message
            Event llmChatCompletionMessageEventTwo = llmChatCompletionMessageEventIterator.next();
            assertLlmChatCompletionMessageAttributes(llmChatCompletionMessageEventTwo, true);

            // LlmCompletionSummary events
            Collection<Event> llmCompletionSummaryEvents = introspector.getCustomEvents(LLM_CHAT_COMPLETION_SUMMARY);
            assertEquals(1, llmCompletionSummaryEvents.size());

            Iterator<Event> llmCompletionSummaryEventIterator = llmCompletionSummaryEvents.iterator();
            // Summary event for both LlmChatCompletionMessage events
            Event llmCompletionSummaryEvent = llmCompletionSummaryEventIterator.next();
            assertLlmChatCompletionSummaryAttributes(llmCompletionSummaryEvent);
        } else if (ModelResponse.EMBEDDING.equals(operationType)) {
            // LlmEmbedding events
            Collection<Event> llmEmbeddingEvents = introspector.getCustomEvents(LLM_EMBEDDING);
            assertEquals(1, llmEmbeddingEvents.size());

            Iterator<Event> llmEmbeddingEventIterator = llmEmbeddingEvents.iterator();
            // LlmEmbedding event
            Event llmEmbeddingEvent = llmEmbeddingEventIterator.next();
            assertLlmEmbeddingAttributes(llmEmbeddingEvent);
        }
    }

    private void assertLlmChatCompletionMessageAttributes(Event event, boolean isResponse) {
        assertEquals(LLM_CHAT_COMPLETION_MESSAGE, event.getType());

        Map<String, Object> attributes = event.getAttributes();
        assertEquals("Java", attributes.get("ingest_source"));
        assertFalse(((String) attributes.get("completion_id")).isEmpty());
        assertFalse(((String) attributes.get("id")).isEmpty());
        assertFalse(((String) attributes.get("request_id")).isEmpty());
        assertEquals("bedrock", attributes.get("vendor"));
        assertEquals("anthropic.claude-v2", attributes.get("response.model"));
        assertEquals("testPrefix", attributes.get("llm.testPrefix"));
        assertEquals("conversation-id-value", attributes.get("llm.conversation_id"));

        if (isResponse) {
            assertEquals("assistant", attributes.get("role"));
            assertEquals(
                    " The sky appears blue during the day because of how sunlight interacts with the gases in Earth's atmosphere. The main gases in our atmosphere are nitrogen and oxygen. These gases are transparent to visible light wavelengths, but they scatter shorter wavelengths more, specifically blue light. This scattering makes the sky look blue from the ground.",
                    attributes.get("content"));
            assertEquals(true, attributes.get("is_response"));
            assertEquals(1, attributes.get("sequence"));
        } else {
            assertEquals("user", attributes.get("role"));
            assertEquals("Human: What is the color of the sky?\n\nAssistant:", attributes.get("content"));
            assertEquals(false, attributes.get("is_response"));
            assertEquals(0, attributes.get("sequence"));
        }
    }

    private void assertLlmChatCompletionSummaryAttributes(Event event) {
        assertEquals(LLM_CHAT_COMPLETION_SUMMARY, event.getType());

        Map<String, Object> attributes = event.getAttributes();
        assertEquals("Java", attributes.get("ingest_source"));
        assertEquals(0.5f, attributes.get("request.temperature"));
        assertTrue(((Float) attributes.get("duration")) > 0);
        assertEquals("stop_sequence", attributes.get("response.choices.finish_reason"));
        assertEquals("anthropic.claude-v2", attributes.get("request.model"));
        assertEquals("bedrock", attributes.get("vendor"));
        assertEquals("anthropic.claude-v2", attributes.get("response.model"));
        assertFalse(((String) attributes.get("id")).isEmpty());
        assertFalse(((String) attributes.get("request_id")).isEmpty());
        assertEquals(2, attributes.get("response.number_of_messages"));
        assertEquals(1000, attributes.get("request.max_tokens"));
        assertEquals("testPrefix", attributes.get("llm.testPrefix"));
        assertEquals("conversation-id-value", attributes.get("llm.conversation_id"));
    }

    private void assertLlmEmbeddingAttributes(Event event) {
        assertEquals(LLM_EMBEDDING, event.getType());

        Map<String, Object> attributes = event.getAttributes();
        assertEquals("Java", attributes.get("ingest_source"));
        assertTrue(((Float) attributes.get("duration")) >= 0);
        assertEquals("{\"inputText\":\"What is the color of the sky?\"}", attributes.get("input"));
        assertEquals("amazon.titan-embed-text-v1", attributes.get("request.model"));
        assertEquals("amazon.titan-embed-text-v1", attributes.get("response.model"));
        assertEquals("bedrock", attributes.get("vendor"));
        assertFalse(((String) attributes.get("id")).isEmpty());
        assertFalse(((String) attributes.get("request_id")).isEmpty());
        assertEquals("testPrefix", attributes.get("llm.testPrefix"));
        assertEquals("conversation-id-value", attributes.get("llm.conversation_id"));
    }
}