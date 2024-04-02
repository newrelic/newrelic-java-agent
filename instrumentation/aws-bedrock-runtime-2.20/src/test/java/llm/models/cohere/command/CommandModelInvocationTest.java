/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package llm.models.cohere.command;

import com.newrelic.agent.introspec.Event;
import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.api.agent.LlmTokenCountCallback;
import com.newrelic.agent.bridge.aimonitoring.LlmTokenCountCallbackHolder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.bedrockruntime.model.BedrockRuntimeResponseMetadata;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import static llm.events.LlmEvent.LLM_CHAT_COMPLETION_MESSAGE;
import static llm.events.LlmEvent.LLM_CHAT_COMPLETION_SUMMARY;
import static llm.events.LlmEvent.LLM_EMBEDDING;
import static llm.models.TestUtil.assertErrorEvent;
import static llm.models.TestUtil.assertLlmChatCompletionMessageAttributes;
import static llm.models.TestUtil.assertLlmChatCompletionSummaryAttributes;
import static llm.models.TestUtil.assertLlmEmbeddingAttributes;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "software.amazon.awssdk.services.bedrockruntime" }, configName = "llm_enabled.yml")
public class CommandModelInvocationTest {

    private final Introspector introspector = InstrumentationTestRunner.getIntrospector();

    // Embedding
    private final String embeddingModelId = "cohere.embed-english-v3";
    private final String embeddingRequestBody = "{\"texts\":[\"What is the color of the sky?\"],\"truncate\":\"NONE\",\"input_type\":\"search_document\"}";
    private final String embeddingResponseBody = "{\"embeddings\":[[-0.002828598,0.012145996]],\"id\":\"c2c5c119-1268-4155-8c98-50ae199ffa16\",\"response_type\":\"embeddings_floats\",\"texts\":[\"what is the color of the sky?\"]}";
    private final String embeddingRequestInput = "What is the color of the sky?";

    // Completion
    private final String completionModelId = "cohere.command-light-text-v14";
    private final String completionRequestBody = "{\"p\":0.9,\"stop_sequences\":[\"User:\"],\"truncate\":\"END\",\"max_tokens\":1000,\"stream\":false,\"temperature\":0.5,\"k\":0,\"return_likelihoods\":\"NONE\",\"prompt\":\"What is the color of the sky?\"}";
    private final String completionResponseBody = "{\"generations\":[{\"finish_reason\":\"COMPLETE\",\"id\":\"314ba8cf-778d-49ed-a2cb-cf260008a2cc\",\"text\":\" The color of the sky is blue.\"}],\"id\":\"3070a2a7-b5a3-44cf-9908-554fa25473a6\",\"prompt\":\"What is the color of the sky?\"}";
    private final String completionRequestInput = "What is the color of the sky?";
    private final String completionResponseContent = " The color of the sky is blue.";
    private final String finishReason = "COMPLETE";

    @Before
    public void before() {
        introspector.clear();
        LlmTokenCountCallback llmTokenCountCallback = (model, content) -> 13;
        LlmTokenCountCallbackHolder.setLlmTokenCountCallback(llmTokenCountCallback);
    }

    @Test
    public void testEmbedding() {
        boolean isError = false;

        CommandModelInvocation commandModelInvocation = mockCommandModelInvocation(embeddingModelId, embeddingRequestBody, embeddingResponseBody, isError);
        commandModelInvocation.recordLlmEvents(System.currentTimeMillis());

        Collection<Event> llmEmbeddingEvents = introspector.getCustomEvents(LLM_EMBEDDING);
        assertEquals(1, llmEmbeddingEvents.size());
        Iterator<Event> llmEmbeddingEventIterator = llmEmbeddingEvents.iterator();
        Event llmEmbeddingEvent = llmEmbeddingEventIterator.next();

        assertLlmEmbeddingAttributes(llmEmbeddingEvent, embeddingModelId, embeddingRequestInput);

        assertErrorEvent(isError, introspector.getErrorEvents());
    }

    @Test
    public void testCompletion() {
        boolean isError = false;

        CommandModelInvocation commandModelInvocation = mockCommandModelInvocation(completionModelId, completionRequestBody, completionResponseBody, isError);
        commandModelInvocation.recordLlmEvents(System.currentTimeMillis());

        Collection<Event> llmChatCompletionMessageEvents = introspector.getCustomEvents(LLM_CHAT_COMPLETION_MESSAGE);
        assertEquals(2, llmChatCompletionMessageEvents.size());
        Iterator<Event> llmChatCompletionMessageEventIterator = llmChatCompletionMessageEvents.iterator();
        Event llmChatCompletionMessageEventOne = llmChatCompletionMessageEventIterator.next();

        assertLlmChatCompletionMessageAttributes(llmChatCompletionMessageEventOne, completionModelId, completionRequestInput, completionResponseContent, false);

        Event llmChatCompletionMessageEventTwo = llmChatCompletionMessageEventIterator.next();

        assertLlmChatCompletionMessageAttributes(llmChatCompletionMessageEventTwo, completionModelId, completionRequestInput, completionResponseContent, true);

        Collection<Event> llmChatCompletionSummaryEvents = introspector.getCustomEvents(LLM_CHAT_COMPLETION_SUMMARY);
        assertEquals(1, llmChatCompletionSummaryEvents.size());
        Iterator<Event> llmChatCompletionSummaryEventIterator = llmChatCompletionSummaryEvents.iterator();
        Event llmChatCompletionSummaryEvent = llmChatCompletionSummaryEventIterator.next();

        assertLlmChatCompletionSummaryAttributes(llmChatCompletionSummaryEvent, completionModelId, finishReason);

        assertErrorEvent(isError, introspector.getErrorEvents());
    }

    @Test
    public void testEmbeddingError() {
        boolean isError = true;

        CommandModelInvocation commandModelInvocation = mockCommandModelInvocation(embeddingModelId, embeddingRequestBody, embeddingResponseBody, isError);
        commandModelInvocation.recordLlmEvents(System.currentTimeMillis());

        Collection<Event> llmEmbeddingEvents = introspector.getCustomEvents(LLM_EMBEDDING);
        assertEquals(1, llmEmbeddingEvents.size());
        Iterator<Event> llmEmbeddingEventIterator = llmEmbeddingEvents.iterator();
        Event llmEmbeddingEvent = llmEmbeddingEventIterator.next();

        assertLlmEmbeddingAttributes(llmEmbeddingEvent, embeddingModelId, embeddingRequestInput);

        assertErrorEvent(isError, introspector.getErrorEvents());
    }

    @Test
    public void testCompletionError() {
        boolean isError = true;

        CommandModelInvocation commandModelInvocation = mockCommandModelInvocation(completionModelId, completionRequestBody, completionResponseBody, isError);
        commandModelInvocation.recordLlmEvents(System.currentTimeMillis());

        Collection<Event> llmChatCompletionMessageEvents = introspector.getCustomEvents(LLM_CHAT_COMPLETION_MESSAGE);
        assertEquals(2, llmChatCompletionMessageEvents.size());
        Iterator<Event> llmChatCompletionMessageEventIterator = llmChatCompletionMessageEvents.iterator();
        Event llmChatCompletionMessageEventOne = llmChatCompletionMessageEventIterator.next();

        assertLlmChatCompletionMessageAttributes(llmChatCompletionMessageEventOne, completionModelId, completionRequestInput, completionResponseContent, false);

        Event llmChatCompletionMessageEventTwo = llmChatCompletionMessageEventIterator.next();

        assertLlmChatCompletionMessageAttributes(llmChatCompletionMessageEventTwo, completionModelId, completionRequestInput, completionResponseContent, true);

        Collection<Event> llmChatCompletionSummaryEvents = introspector.getCustomEvents(LLM_CHAT_COMPLETION_SUMMARY);
        assertEquals(1, llmChatCompletionSummaryEvents.size());
        Iterator<Event> llmChatCompletionSummaryEventIterator = llmChatCompletionSummaryEvents.iterator();
        Event llmChatCompletionSummaryEvent = llmChatCompletionSummaryEventIterator.next();

        assertLlmChatCompletionSummaryAttributes(llmChatCompletionSummaryEvent, completionModelId, finishReason);

        assertErrorEvent(isError, introspector.getErrorEvents());
    }

    private CommandModelInvocation mockCommandModelInvocation(String modelId, String requestBody, String responseBody, boolean isError) {
        // Given
        Map<String, String> linkingMetadata = new HashMap<>();
        linkingMetadata.put("span.id", "span-id-123");
        linkingMetadata.put("trace.id", "trace-id-xyz");

        Map<String, Object> userAttributes = new HashMap<>();
        userAttributes.put("llm.conversation_id", "conversation-id-value");
        userAttributes.put("llm.testPrefix", "testPrefix");
        userAttributes.put("test", "test");

        // Mock out ModelRequest
        InvokeModelRequest mockInvokeModelRequest = mock(InvokeModelRequest.class);
        SdkBytes mockRequestSdkBytes = mock(SdkBytes.class);
        when(mockInvokeModelRequest.body()).thenReturn(mockRequestSdkBytes);
        when(mockRequestSdkBytes.asUtf8String()).thenReturn(requestBody);
        when(mockInvokeModelRequest.modelId()).thenReturn(modelId);

        // Mock out ModelResponse
        InvokeModelResponse mockInvokeModelResponse = mock(InvokeModelResponse.class);
        SdkBytes mockResponseSdkBytes = mock(SdkBytes.class);
        when(mockInvokeModelResponse.body()).thenReturn(mockResponseSdkBytes);
        when(mockResponseSdkBytes.asUtf8String()).thenReturn(responseBody);

        SdkHttpResponse mockSdkHttpResponse = mock(SdkHttpResponse.class);
        when(mockInvokeModelResponse.sdkHttpResponse()).thenReturn(mockSdkHttpResponse);

        if (isError) {
            when(mockSdkHttpResponse.statusCode()).thenReturn(400);
            when(mockSdkHttpResponse.statusText()).thenReturn(Optional.of("BAD_REQUEST"));
            when(mockSdkHttpResponse.isSuccessful()).thenReturn(false);
        } else {
            when(mockSdkHttpResponse.statusCode()).thenReturn(200);
            when(mockSdkHttpResponse.statusText()).thenReturn(Optional.of("OK"));
            when(mockSdkHttpResponse.isSuccessful()).thenReturn(true);
        }

        BedrockRuntimeResponseMetadata mockBedrockRuntimeResponseMetadata = mock(BedrockRuntimeResponseMetadata.class);
        when(mockInvokeModelResponse.responseMetadata()).thenReturn(mockBedrockRuntimeResponseMetadata);
        when(mockBedrockRuntimeResponseMetadata.requestId()).thenReturn("90a22e92-db1d-4474-97a9-28b143846301");

        // Instantiate ModelInvocation
        return new CommandModelInvocation(linkingMetadata, userAttributes, mockInvokeModelRequest,
                mockInvokeModelResponse);
    }
}
