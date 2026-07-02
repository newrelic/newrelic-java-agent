/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package llm.models;

import com.newrelic.agent.introspec.ErrorEvent;
import com.newrelic.agent.introspec.Event;
import llm.models.converse.ConverseModelInvocation;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.bedrockruntime.model.BedrockRuntimeResponseMetadata;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseOutput;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseResponse;
import software.amazon.awssdk.services.bedrockruntime.model.InferenceConfiguration;
import software.amazon.awssdk.services.bedrockruntime.model.Message;
import software.amazon.awssdk.services.bedrockruntime.model.TokenUsage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static llm.events.LlmEvent.LLM_CHAT_COMPLETION_MESSAGE;
import static llm.events.LlmEvent.LLM_CHAT_COMPLETION_SUMMARY;
import static llm.vendor.Vendor.INGEST_SOURCE;
import static llm.vendor.Vendor.VENDOR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestUtil {
    // Completion request
    public static final String REQUEST_MODEL_ID = "anthropic.claude-3-haiku-20240307-v1:0";
    public static final String REQUEST_ROLE = "user";
    public static final String REQUEST_ID = "90a22e92-db1d-4474-97a9-28b143846301";
    public static final String REQUEST_CONTENT_TEXT = "What is the color of the sky?";
    public static final Integer REQUEST_MAX_TOKENS = 1000;
    public static final Float REQUEST_TEMPERATURE = 0.7F;

    // Completion response
    public static final String RESPONSE_CONTENT_TEXT = "The color of the sky is blue.";
    public static final String STOP_REASON = "end_turn";
    public static final String AWS_REQUEST_ID = "9d32a71a-e285-4b14-a23d-4f7d67b50ac3";
    public static final int SUCCESS_STATUS_CODE = 200;
    public static final String SUCCESS_STATUS_TEXT = "OK";
    public static final int ERROR_STATUS_CODE = 408;
    public static final String ERROR_STATUS_TEXT = "REQUEST TIMEOUT";
    public static final String RESPONSE_ROLE = "assistant";
    public static final int INPUT_TOKENS = 17;
    public static final int OUTPUT_TOKENS = 23;
    public static final int TOTAL_TOKENS = 40;

    public static void assertLlmChatCompletionMessageAttributes(Event event, String modelId, String requestInput, String responseContent, boolean isResponse) {
        assertEquals(LLM_CHAT_COMPLETION_MESSAGE, event.getType());

        Map<String, Object> attributes = event.getAttributes();
        assertEquals(INGEST_SOURCE, attributes.get("ingest_source"));
        assertFalse(((String) attributes.get("completion_id")).isEmpty());
        assertFalse(((String) attributes.get("id")).isEmpty());
        assertFalse(((String) attributes.get("request_id")).isEmpty());
        assertEquals(VENDOR, attributes.get("vendor"));
        assertEquals(modelId, attributes.get("response.model"));
        assertEquals("testPrefix", attributes.get("llm.testPrefix"));
        assertEquals("conversation-id-value", attributes.get("llm.conversation_id"));

        if (isResponse) {
            assertEquals(RESPONSE_ROLE, attributes.get("role"));
            assertEquals(responseContent, attributes.get("content"));
            assertEquals(true, attributes.get("is_response"));
            assertEquals(1, attributes.get("sequence"));
        } else {
            assertEquals(REQUEST_ROLE, attributes.get("role"));
            assertEquals(requestInput, attributes.get("content"));
            assertEquals(false, attributes.get("is_response"));
            assertEquals(0, attributes.get("sequence"));
        }

        if (attributes.containsKey("token_count")) {
            Object tokenCount = attributes.get("token_count");
            assertTrue("token_count should be 0 or 13, was: " + tokenCount, tokenCount.equals(0) || tokenCount.equals(13));
        }
    }

    public static void assertLlmChatCompletionSummaryAttributes(Event event, String modelId, String finishReason) {
        assertEquals(LLM_CHAT_COMPLETION_SUMMARY, event.getType());

        Map<String, Object> attributes = event.getAttributes();
        assertEquals(INGEST_SOURCE, attributes.get("ingest_source"));
        assertEquals(REQUEST_TEMPERATURE, attributes.get("request.temperature"));
        assertTrue(((Float) attributes.get("duration")) >= 0);
        assertEquals(finishReason, attributes.get("response.choices.finish_reason"));
        assertEquals(modelId, attributes.get("request.model"));
        assertEquals(VENDOR, attributes.get("vendor"));
        assertEquals(modelId, attributes.get("response.model"));
        assertFalse(((String) attributes.get("id")).isEmpty());
        assertFalse(((String) attributes.get("request_id")).isEmpty());
        assertEquals(2, attributes.get("response.number_of_messages"));
        assertEquals(REQUEST_MAX_TOKENS, attributes.get("request.max_tokens"));
        assertEquals("testPrefix", attributes.get("llm.testPrefix"));
        assertEquals("conversation-id-value", attributes.get("llm.conversation_id"));

        if (attributes.containsKey("response.usage.prompt_tokens")) {
            assertTrue((Integer) attributes.get("response.usage.prompt_tokens") > 0);
            assertTrue((Integer) attributes.get("response.usage.completion_tokens") > 0);
            assertTrue((Integer) attributes.get("response.usage.total_tokens") > 0);
        }
    }

    public static void assertErrorEvent(boolean isError, Collection<ErrorEvent> errorEvents) {
        if (isError) {
            assertEquals(1, errorEvents.size());
            Iterator<ErrorEvent> errorEventIterator = errorEvents.iterator();
            ErrorEvent errorEvent = errorEventIterator.next();

            assertEquals("LlmError: " + ERROR_STATUS_TEXT, errorEvent.getErrorClass());
            assertEquals("LlmError: " + ERROR_STATUS_TEXT, errorEvent.getErrorMessage());

            Map<String, Object> errorEventAttributes = errorEvent.getAttributes();
            assertFalse(errorEventAttributes.isEmpty());
            assertEquals(ERROR_STATUS_CODE, errorEventAttributes.get("error.code"));
            assertEquals(ERROR_STATUS_CODE, errorEventAttributes.get("http.statusCode"));
        } else {
            assertTrue(errorEvents.isEmpty());
        }
    }

    public static ConverseModelInvocation mockConverseModelInvocation(String modelId, String requestBody, String responseBody, boolean isError, boolean completeUsage) {
        // Given
        Map<String, String> linkingMetadata = new HashMap<>();
        linkingMetadata.put("span.id", "span-id-123");
        linkingMetadata.put("trace.id", "trace-id-xyz");

        Map<String, Object> userAttributes = new HashMap<>();
        userAttributes.put("llm.conversation_id", "conversation-id-value");
        userAttributes.put("llm.testPrefix", "testPrefix");
        userAttributes.put("test", "test");

        // Mock out ModelRequest
        ConverseRequest mockConverseRequest = mock(ConverseRequest.class);
        InferenceConfiguration mockInferenceConfiguration = mock(InferenceConfiguration.class);
        when(mockInferenceConfiguration.maxTokens()).thenReturn(REQUEST_MAX_TOKENS);
        when(mockInferenceConfiguration.temperature()).thenReturn(REQUEST_TEMPERATURE);
        when(mockConverseRequest.inferenceConfig()).thenReturn(mockInferenceConfiguration);
        when(mockConverseRequest.modelId()).thenReturn(modelId);

        when(mockConverseRequest.hasMessages()).thenReturn(true);

        List<ContentBlock> requestContent = new ArrayList<>();
        ContentBlock requestContentBlock = ContentBlock.builder().text(requestBody).build();
        requestContent.add(requestContentBlock);

        List<Message> requestMessages = new ArrayList<>();
        Message requestMessage = Message.builder().role(REQUEST_ROLE).content(requestContent).build();
        requestMessages.add(requestMessage);

        when(mockConverseRequest.messages()).thenReturn(requestMessages);

        // Mock out ModelResponse
        ConverseResponse mockConverseResponse = mock(ConverseResponse.class);

        when(mockConverseResponse.stopReasonAsString()).thenReturn(STOP_REASON);

        ContentBlock responseContentText = ContentBlock.builder().text(responseBody).build();
        Message responseMessage = Message.builder().role(RESPONSE_ROLE).content(responseContentText).build();

        ConverseOutput converseOutput = ConverseOutput.builder().message(responseMessage).build();

        TokenUsage tokenUsage;
        if (completeUsage) {
            tokenUsage = TokenUsage.builder().inputTokens(INPUT_TOKENS).outputTokens(OUTPUT_TOKENS).totalTokens(TOTAL_TOKENS).build();
        } else {
            tokenUsage = TokenUsage.builder().inputTokens(-1).outputTokens(-1).totalTokens(-1).build();
        }
        when(mockConverseResponse.usage()).thenReturn(tokenUsage);
        when(mockConverseResponse.output()).thenReturn(converseOutput);

        SdkHttpResponse mockSdkHttpResponse = mock(SdkHttpResponse.class);
        when(mockConverseResponse.sdkHttpResponse()).thenReturn(mockSdkHttpResponse);

        if (isError) {
            when(mockSdkHttpResponse.statusCode()).thenReturn(ERROR_STATUS_CODE);
            when(mockSdkHttpResponse.statusText()).thenReturn(Optional.of(ERROR_STATUS_TEXT));
            when(mockSdkHttpResponse.isSuccessful()).thenReturn(false);
        } else {
            when(mockSdkHttpResponse.statusCode()).thenReturn(SUCCESS_STATUS_CODE);
            when(mockSdkHttpResponse.statusText()).thenReturn(Optional.of(SUCCESS_STATUS_TEXT));
            when(mockSdkHttpResponse.isSuccessful()).thenReturn(true);
        }

        BedrockRuntimeResponseMetadata mockBedrockRuntimeResponseMetadata = mock(BedrockRuntimeResponseMetadata.class);
        when(mockConverseResponse.responseMetadata()).thenReturn(mockBedrockRuntimeResponseMetadata);
        when(mockBedrockRuntimeResponseMetadata.requestId()).thenReturn(REQUEST_ID);

        // Instantiate ModelInvocation
        return new ConverseModelInvocation(linkingMetadata, userAttributes, mockConverseRequest,
                mockConverseResponse);
    }
}
