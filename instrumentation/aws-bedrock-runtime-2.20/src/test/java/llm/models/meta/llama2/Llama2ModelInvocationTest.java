/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package llm.models.meta.llama2;

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
import static llm.models.TestUtil.assertErrorEvent;
import static llm.models.TestUtil.assertLlmChatCompletionMessageAttributes;
import static llm.models.TestUtil.assertLlmChatCompletionSummaryAttributes;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "software.amazon.awssdk.services.bedrockruntime" }, configName = "llm_enabled.yml")
public class Llama2ModelInvocationTest {

    private final Introspector introspector = InstrumentationTestRunner.getIntrospector();

    // Completion
    private final String completionModelId = "meta.llama2-13b-chat-v1";
    private final String completionRequestBody = "{\"top_p\":0.9,\"max_gen_len\":1000,\"temperature\":0.5,\"prompt\":\"What is the color of the sky?\"}";
    private final String completionResponseBody = "{\"generation\":\"\\n\\nThe color of the sky is blue.\",\"prompt_token_count\":9,\"generation_token_count\":306,\"stop_reason\":\"stop\"}";
    private final String completionRequestInput = "What is the color of the sky?";
    private final String completionResponseContent = "\n\nThe color of the sky is blue.";
    private final String finishReason = "stop";

    @Before
    public void before() {
        introspector.clear();
        LlmTokenCountCallback llmTokenCountCallback = (model, content) -> 13;
        LlmTokenCountCallbackHolder.setLlmTokenCountCallback(llmTokenCountCallback);
    }

    @Test
    public void testCompletion() {
        boolean isError = false;

        Llama2ModelInvocation llama2ModelInvocation = mockLlama2ModelInvocation(completionModelId, completionRequestBody, completionResponseBody,
                isError);
        llama2ModelInvocation.recordLlmEvents(System.currentTimeMillis());

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

        Llama2ModelInvocation llama2ModelInvocation = mockLlama2ModelInvocation(completionModelId, completionRequestBody, completionResponseBody,
                isError);
        llama2ModelInvocation.recordLlmEvents(System.currentTimeMillis());

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

    private Llama2ModelInvocation mockLlama2ModelInvocation(String modelId, String requestBody, String responseBody, boolean isError) {
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
        return new Llama2ModelInvocation(linkingMetadata, userAttributes, mockInvokeModelRequest,
                mockInvokeModelResponse);
    }
}
