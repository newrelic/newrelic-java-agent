/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package llm.completions.models.springai;

import com.newrelic.agent.bridge.aimonitoring.LlmTokenCountCallbackHolder;
import com.newrelic.agent.introspec.Event;
import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.api.agent.LlmTokenCountCallback;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static llm.completions.TestUtil.assertLlmChatCompletionMessageAttributes;
import static llm.completions.TestUtil.assertLlmChatCompletionSummaryAttributes;
import static llm.completions.events.LlmEvent.LLM_CHAT_COMPLETION_MESSAGE;
import static llm.completions.events.LlmEvent.LLM_CHAT_COMPLETION_SUMMARY;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "org.springframework.ai.chat.client" }, configName = "llm_enabled.yml")
public class SpringAiModelInvocationTest {

    private final Introspector introspector = InstrumentationTestRunner.getIntrospector();

    // Given
    Map<String, String> linkingMetadata = new HashMap<>();

    Map<String, Object> userAttributes = new HashMap<>();

    // Expected request values
    String expectedRequestModelId = "request-model";
    Double expectedTemp = 0.3;
    String expectedPromptUserMessage = "tell me a joke";
    UserMessage expectedUserMessage = new UserMessage(expectedPromptUserMessage);
    List<UserMessage> expectedPromptUserMessages = new ArrayList<>();
    Integer expectedMaxTokens = 1000;

    // Expected response values
    List<Generation> expectedResults = new ArrayList<>();
    String expectedGenerationMessage = "generated results";
    String expectedFinishReason = "stop";

    Generation expectedGeneration = new Generation(new AssistantMessage(expectedGenerationMessage),
            ChatGenerationMetadata.builder().finishReason(expectedFinishReason).build());
    String expectedResponseModelId = "response-model";
    Integer expectedPromptTokens = 123;
    Integer expectedCompletionTokens = 456;
    Integer expectedTotalTokens = 579;

    @Before
    public void before() {
        introspector.clear();
        LlmTokenCountCallback llmTokenCountCallback = (model, content) -> 13;
        LlmTokenCountCallbackHolder.setLlmTokenCountCallback(llmTokenCountCallback);

        linkingMetadata.put("span.id", "span-id-123");
        linkingMetadata.put("trace.id", "trace-id-xyz");

        userAttributes.put("llm.conversation_id", "conversation-id-value");
        userAttributes.put("llm.testPrefix", "testPrefix");
        userAttributes.put("test", "test");

        expectedPromptUserMessages.add(expectedUserMessage);
        expectedResults.add(expectedGeneration);
    }

    @Test
    public void testCompletion() {
        // Instantiate ModelInvocation
        SpringAiModelInvocation springAiModelInvocation = mockSpringAiModelInvocation();
        springAiModelInvocation.recordLlmEvents(System.currentTimeMillis());

        Collection<Event> llmChatCompletionMessageEvents = introspector.getCustomEvents(LLM_CHAT_COMPLETION_MESSAGE);
        assertEquals(2, llmChatCompletionMessageEvents.size());
        Iterator<Event> llmChatCompletionMessageEventIterator = llmChatCompletionMessageEvents.iterator();
        Event llmChatCompletionMessageEventOne = llmChatCompletionMessageEventIterator.next();

        assertLlmChatCompletionMessageAttributes(llmChatCompletionMessageEventOne, expectedResponseModelId, expectedPromptUserMessage, false);

        Event llmChatCompletionMessageEventTwo = llmChatCompletionMessageEventIterator.next();

        assertLlmChatCompletionMessageAttributes(llmChatCompletionMessageEventTwo, expectedResponseModelId, expectedGenerationMessage, true);

        Collection<Event> llmChatCompletionSummaryEvents = introspector.getCustomEvents(LLM_CHAT_COMPLETION_SUMMARY);
        assertEquals(1, llmChatCompletionSummaryEvents.size());
        Iterator<Event> llmChatCompletionSummaryEventIterator = llmChatCompletionSummaryEvents.iterator();
        Event llmChatCompletionSummaryEvent = llmChatCompletionSummaryEventIterator.next();

        assertLlmChatCompletionSummaryAttributes(llmChatCompletionSummaryEvent, expectedRequestModelId, expectedResponseModelId, expectedFinishReason,
                expectedTemp);
    }

    private SpringAiModelInvocation mockSpringAiModelInvocation() {
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

}
