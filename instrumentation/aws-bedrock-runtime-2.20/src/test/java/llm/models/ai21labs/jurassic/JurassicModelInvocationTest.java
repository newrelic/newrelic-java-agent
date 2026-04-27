/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package llm.models.ai21labs.jurassic;

import com.newrelic.agent.bridge.aimonitoring.LlmTokenCountCallbackHolder;
import com.newrelic.agent.introspec.Event;
import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.api.agent.LlmTokenCountCallback;
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
import static llm.models.TestUtil.assertErrorEvent;
import static llm.models.TestUtil.assertLlmChatCompletionMessageAttributes;
import static llm.models.TestUtil.assertLlmChatCompletionSummaryAttributes;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "software.amazon.awssdk.services.bedrockruntime" }, configName = "llm_enabled.yml")
public class JurassicModelInvocationTest {

    private final Introspector introspector = InstrumentationTestRunner.getIntrospector();

    // Completion
    private final String completionModelId = "ai21.j2-mid-v1";
    private final String completionRequestBody = "{\"temperature\":0.5,\"maxTokens\":1000,\"prompt\":\"What is the color of the sky?\"}";
    private final String completionRequestInput = "What is the color of the sky?";
    private final String completionResponseContent = "\nThe color of the sky is blue.";
    private final String finishReason = "endoftext";

    // Completion with token arrays (complete usage data))
    private final String completionResponseBodyWithTokens =
            "{\"id\":1234,\"prompt\":{\"text\":\"What is the color of the sky?\",\"tokens\":[" +
                    "{\"generatedToken\":{\"token\":\"▁What\"},\"textRange\":{\"start\":0,\"end\":5}}," +
                    "{\"generatedToken\":{\"token\":\"▁is\"},\"textRange\":{\"start\":5,\"end\":8}}," +
                    "{\"generatedToken\":{\"token\":\"▁the\"},\"textRange\":{\"start\":8,\"end\":12}}," +
                    "{\"generatedToken\":{\"token\":\"▁color\"},\"textRange\":{\"start\":12,\"end\":18}}" +
                    "]},\"completions\":[{\"data\":{\"text\":\"\\nThe color of the sky is blue.\",\"tokens\":[" +
                    "{\"generatedToken\":{\"token\":\"<|newline|>\"},\"textRange\":{\"start\":0,\"end\":1}}," +
                    "{\"generatedToken\":{\"token\":\"▁The\"},\"textRange\":{\"start\":1,\"end\":5}}," +
                    "{\"generatedToken\":{\"token\":\"▁color\"},\"textRange\":{\"start\":5,\"end\":11}}," +
                    "{\"generatedToken\":{\"token\":\"▁of\"},\"textRange\":{\"start\":11,\"end\":14}}," +
                    "{\"generatedToken\":{\"token\":\"▁the\"},\"textRange\":{\"start\":14,\"end\":18}}," +
                    "{\"generatedToken\":{\"token\":\"▁sky\"},\"textRange\":{\"start\":18,\"end\":22}}," +
                    "{\"generatedToken\":{\"token\":\"▁is\"},\"textRange\":{\"start\":22,\"end\":25}}," +
                    "{\"generatedToken\":{\"token\":\"▁blue\"},\"textRange\":{\"start\":25,\"end\":30}}," +
                    "{\"generatedToken\":{\"token\":\".\"},\"textRange\":{\"start\":30,\"end\":31}}" +
                    "]},\"finishReason\":{\"reason\":\"endoftext\"}}]}";

    // Completion with missing token arrays (incomplete usage data)
    private final String completionResponseBodyWithoutTokens =
            "{\"id\":1234,\"prompt\":{\"text\":\"What is the color of the sky?\"},\"completions\":[{\"data\":{\"text\":\"\\nThe color of the sky is blue.\"},\"finishReason\":{\"reason\":\"endoftext\"}}]}";

    @Before
    public void before() {
        introspector.clear();
        LlmTokenCountCallback llmTokenCountCallback = (model, content) -> 13;
        LlmTokenCountCallbackHolder.setLlmTokenCountCallback(llmTokenCountCallback);
    }

    @Test
    public void testCompletion() {
        boolean isError = false;

        JurassicModelInvocation jurassicModelInvocation = mockJurassicModelInvocation(completionModelId, completionRequestBody,
                completionResponseBodyWithoutTokens, isError);
        jurassicModelInvocation.recordLlmEvents(System.currentTimeMillis());

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
    public void testCompletionError() {
        boolean isError = true;

        JurassicModelInvocation jurassicModelInvocation = mockJurassicModelInvocation(completionModelId, completionRequestBody,
                completionResponseBodyWithoutTokens, isError);
        jurassicModelInvocation.recordLlmEvents(System.currentTimeMillis());

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
    public void testCompletionWithCompleteUsageData() {

        boolean isError = false;

        JurassicModelInvocation jurassicModelInvocation = mockJurassicModelInvocation(completionModelId, completionRequestBody,
                completionResponseBodyWithTokens, isError);
        jurassicModelInvocation.recordLlmEvents(System.currentTimeMillis());

        Collection<Event> llmChatCompletionMessageEvents = introspector.getCustomEvents(LLM_CHAT_COMPLETION_MESSAGE);
        assertEquals(2, llmChatCompletionMessageEvents.size());

        for (Event messageEvent : llmChatCompletionMessageEvents) {
            Map<String, Object> attributes = messageEvent.getAttributes();
            assertEquals(0, attributes.get("token_count"));
        }

        Collection<Event> llmChatCompletionSummaryEvents = introspector.getCustomEvents(LLM_CHAT_COMPLETION_SUMMARY);
        assertEquals(1, llmChatCompletionSummaryEvents.size());
        Event summaryEvent = llmChatCompletionSummaryEvents.iterator().next();
        Map<String, Object> summaryAttributes = summaryEvent.getAttributes();

        assertEquals(4, summaryAttributes.get("response.usage.prompt_tokens"));
        assertEquals(9, summaryAttributes.get("response.usage.completion_tokens"));
        assertEquals(13, summaryAttributes.get("response.usage.total_tokens"));
    }

    @Test
    public void testCompletionWithIncompleteUsageData() {
        boolean isError = false;

        JurassicModelInvocation jurassicModelInvocation = mockJurassicModelInvocation(completionModelId, completionRequestBody,
                completionResponseBodyWithoutTokens, isError);
        jurassicModelInvocation.recordLlmEvents(System.currentTimeMillis());

        Collection<Event> llmChatCompletionMessageEvents = introspector.getCustomEvents(LLM_CHAT_COMPLETION_MESSAGE);
        assertEquals(2, llmChatCompletionMessageEvents.size());

        for (Event messageEvent : llmChatCompletionMessageEvents) {
            Map<String, Object> attributes = messageEvent.getAttributes();
            assertEquals(13, attributes.get("token_count"));
        }

        Collection<Event> llmChatCompletionSummaryEvents = introspector.getCustomEvents(LLM_CHAT_COMPLETION_SUMMARY);
        assertEquals(1, llmChatCompletionSummaryEvents.size());
        Event summaryEvent = llmChatCompletionSummaryEvents.iterator().next();
        Map<String, Object> summaryAttributes = summaryEvent.getAttributes();

        assertFalse(summaryAttributes.containsKey("response.usage.prompt_tokens"));
        assertFalse(summaryAttributes.containsKey("response.usage.completion_tokens"));
        assertFalse(summaryAttributes.containsKey("response.usage.total_tokens"));
    }

    @Test
    public void testCompletionWithNoCallback() {
        boolean isError = false;

        LlmTokenCountCallbackHolder.setLlmTokenCountCallback(null);

        JurassicModelInvocation jurassicModelInvocation = mockJurassicModelInvocation(completionModelId, completionRequestBody,
                completionResponseBodyWithoutTokens, isError);
        jurassicModelInvocation.recordLlmEvents(System.currentTimeMillis());

        Collection<Event> llmChatCompletionMessageEvents = introspector.getCustomEvents(LLM_CHAT_COMPLETION_MESSAGE);
        assertEquals(2, llmChatCompletionMessageEvents.size());

        for (Event messageEvent : llmChatCompletionMessageEvents) {
            Map<String, Object> attributes = messageEvent.getAttributes();
            assertFalse(attributes.containsKey("token_count"));
        }
    }

    private JurassicModelInvocation mockJurassicModelInvocation(String modelId, String requestBody, String responseBody, boolean isError) {
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
        return new JurassicModelInvocation(linkingMetadata, userAttributes, mockInvokeModelRequest,
                mockInvokeModelResponse);
    }
}
