/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package util;

import com.newrelic.agent.bridge.aimonitoring.LlmTokenCountCallbackHolder;
import com.newrelic.agent.introspec.Event;
import com.newrelic.api.agent.LlmTokenCountCallback;
import llm.completions.models.springai.SpringAiModelInvocation;
import llm.completions.vendor.Vendor;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static llm.completions.events.LlmEvent.LLM_CHAT_COMPLETION_MESSAGE;
import static llm.completions.events.LlmEvent.LLM_CHAT_COMPLETION_SUMMARY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CompletionUtil {
    public static Map<String, String> linkingMetadata = new HashMap<>();

    public static Map<String, Object> userAttributes = new HashMap<>();

    // Expected request values
    public static String expectedRequestModelId = "gpt-4o-request";
    public static String expectedResponseModelId = "gpt-4o-response";
    public static Double expectedTemp = 0.3;
    public static String expectedPromptUserMessage = "Tell me a joke!";
    public static UserMessage expectedUserMessage = new UserMessage(expectedPromptUserMessage);
    public static List<UserMessage> expectedPromptUserMessages = new ArrayList<>();
    public static Integer expectedMaxTokens = 1000;

    // Expected response values
    public static List<Generation> expectedResults = new ArrayList<>();
    public static String expectedGenerationMessage = "Why don't scientists trust atoms? Because they make up everything!";
    public static String expectedFinishReason = "stop";

    public static Generation expectedGeneration = new Generation(new AssistantMessage(expectedGenerationMessage),
            ChatGenerationMetadata.builder().finishReason(expectedFinishReason).build());
    public static String expectedConversationId = "conversation-id-value";
    public static String expectedTestPrefix = "testPrefix";
    public static Integer expectedPromptTokens = 123;
    public static Integer expectedCompletionTokens = 456;
    public static Integer expectedTotalTokens = 579;
    public static String expectedCompletionId = "chatcmpl-DQiVf9ymJ30wEL1PROnY8JKZHA3Ku";

    public static void assertLlmChatCompletionMessageAttributes(Event event, String modelId, String content, boolean isResponse) {
        assertEquals(LLM_CHAT_COMPLETION_MESSAGE, event.getType());

        Map<String, Object> attributes = event.getAttributes();
        assertEquals(Vendor.INGEST_SOURCE, attributes.get("ingest_source"));
        assertFalse(((String) attributes.get("completion_id")).isEmpty());
        assertFalse(((String) attributes.get("id")).isEmpty());
        assertFalse(((String) attributes.get("request_id")).isEmpty());
        assertEquals(Vendor.VENDOR, attributes.get("vendor"));
        assertEquals(modelId, attributes.get("response.model"));
        assertEquals(expectedTestPrefix, attributes.get("llm.testPrefix"));
        assertEquals(expectedConversationId, attributes.get("llm.conversation_id"));
        assertEquals(13, attributes.get("token_count"));

        assertEquals(content, attributes.get("content"));
        if (isResponse) {
            assertEquals("assistant", attributes.get("role"));
            assertEquals(true, attributes.get("is_response"));
            assertEquals(1, attributes.get("sequence"));
        } else {
            assertEquals("user", attributes.get("role"));
            assertEquals(false, attributes.get("is_response"));
            assertEquals(0, attributes.get("sequence"));
        }
    }

    public static void assertLlmChatCompletionSummaryAttributes(Event event, String requestModelId, String responseModelId, String finishReason,
            Double expectedTemp) {
        assertEquals(LLM_CHAT_COMPLETION_SUMMARY, event.getType());

        Map<String, Object> attributes = event.getAttributes();
        assertEquals(Vendor.INGEST_SOURCE, attributes.get("ingest_source"));
        assertEquals(expectedTemp.floatValue(), attributes.get("request.temperature"));
        assertTrue(((Float) attributes.get("duration")) >= 0);
        assertEquals(finishReason, attributes.get("response.choices.finish_reason"));
        assertEquals(requestModelId, attributes.get("request.model"));
        assertEquals(Vendor.VENDOR, attributes.get("vendor"));
        assertEquals(responseModelId, attributes.get("response.model"));
        assertFalse(((String) attributes.get("id")).isEmpty());
        assertFalse(((String) attributes.get("request_id")).isEmpty());
        assertEquals(2, attributes.get("response.number_of_messages"));
        assertEquals(1000, attributes.get("request.max_tokens"));
        assertEquals(expectedTestPrefix, attributes.get("llm.testPrefix"));
        assertEquals(expectedConversationId, attributes.get("llm.conversation_id"));
    }

    public static SpringAiModelInvocation mockSpringAiModelInvocation() {
        // Mock out ModelRequest
        ChatClientRequest mockChatClientRequest = mock(ChatClientRequest.class);
        Prompt mockPrompt = mock(Prompt.class);
        ChatOptions mockChatOptions = mock(ChatOptions.class);

        when(mockPrompt.getOptions()).thenReturn(mockChatOptions);
        when(mockPrompt.getUserMessages()).thenReturn(expectedPromptUserMessages);
        when(mockPrompt.getUserMessage()).thenReturn(expectedUserMessage);

        when(mockChatOptions.getModel()).thenReturn(expectedRequestModelId);
        when(mockChatOptions.getTemperature()).thenReturn(expectedTemp);
        when(mockChatOptions.getMaxTokens()).thenReturn(expectedMaxTokens);

        when(mockChatClientRequest.prompt()).thenReturn(mockPrompt);

        // Mock out ModelResponse
        ChatClientResponse mockChatClientResponse = mock(ChatClientResponse.class);

        ChatResponse mockChatResponse = mock(ChatResponse.class);
        when(mockChatClientResponse.chatResponse()).thenReturn(mockChatResponse);
        when(mockChatResponse.getResults()).thenReturn(expectedResults);
        when(mockChatResponse.getResult()).thenReturn(expectedGeneration);

        ChatResponseMetadata chatResponseMetadata = mock(ChatResponseMetadata.class);
        when(mockChatResponse.getMetadata()).thenReturn(chatResponseMetadata);
        Usage mockUsage = mock(Usage.class);
        when(chatResponseMetadata.getModel()).thenReturn(expectedResponseModelId);
        when(chatResponseMetadata.getUsage()).thenReturn(mockUsage);
        when(mockUsage.getPromptTokens()).thenReturn(expectedPromptTokens);
        when(mockUsage.getCompletionTokens()).thenReturn(expectedCompletionTokens);
        when(mockUsage.getTotalTokens()).thenReturn(expectedTotalTokens);

        // Instantiate ModelInvocation
        return new SpringAiModelInvocation(linkingMetadata, userAttributes, mockChatClientRequest, mockChatClientResponse);
    }

    public static void setupMockTestEnv() {
        LlmTokenCountCallback llmTokenCountCallback = (model, content) -> 13;
        LlmTokenCountCallbackHolder.setLlmTokenCountCallback(llmTokenCountCallback);

        linkingMetadata.put("span.id", "span-id-123");
        linkingMetadata.put("trace.id", "trace-id-xyz");

        userAttributes.put("llm.conversation_id", expectedConversationId);
        userAttributes.put("llm.testPrefix", expectedTestPrefix);
        userAttributes.put("test", "test");

        expectedPromptUserMessages.add(expectedUserMessage);
        expectedResults.add(expectedGeneration);
    }
}
