/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package llm.completions.events;

import com.newrelic.agent.bridge.aimonitoring.LlmTokenCountCallbackHolder;
import com.newrelic.agent.introspec.Event;
import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.api.agent.LlmTokenCountCallback;
import llm.completions.models.ModelInvocation;
import llm.completions.models.springai.SpringAiModelInvocation;
import llm.completions.vendor.Vendor;
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
import java.util.List;
import java.util.Map;

import static llm.completions.events.LlmEvent.LLM_CHAT_COMPLETION_MESSAGE;
import static llm.completions.events.LlmEvent.LLM_CHAT_COMPLETION_SUMMARY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "org.springframework.ai.chat.client" }, configName = "llm_enabled.yml")
public class LlmEventTest {
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
        setUp();
    }

    public void setUp() {
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
    public void testRecordLlmChatCompletionMessageEvent() {
        // Instantiate ModelInvocation
        SpringAiModelInvocation springAiModelInvocation = mockSpringAiModelInvocation();

        // When
        // Build LlmEmbedding event
        LlmEvent.Builder builder = new LlmEvent.Builder(springAiModelInvocation);
        LlmEvent llmChatCompletionMessageEvent = builder
                .spanId() // attribute 1
                .traceId() // attribute 2
                .vendor() // attribute 3
                .ingestSource() // attribute 4
                .id(ModelInvocation.getRandomGuid()) // attribute 5
                .content(expectedPromptUserMessage) // attribute 6
                .role(true) // attribute 7
                .isResponse(true) // attribute 8
                .requestId() // attribute 9
                .responseModel() // attribute 10
                .sequence(0) // attribute 11
                .completionId() // attribute 12
                .tokenCount(LlmTokenCountCallbackHolder.getLlmTokenCountCallback().calculateLlmTokenCount("model", "content")) // attribute 13
                .build();

        // attributes 14 & 15 should be the two llm.* prefixed userAttributes

        // Record LlmChatCompletionMessage event
        llmChatCompletionMessageEvent.recordLlmChatCompletionMessageEvent();

        // Then
        Collection<Event> customEvents = introspector.getCustomEvents(LLM_CHAT_COMPLETION_MESSAGE);
        assertEquals(1, customEvents.size());

        Event event = customEvents.iterator().next();
        assertEquals(LLM_CHAT_COMPLETION_MESSAGE, event.getType());

        Map<String, Object> attributes = event.getAttributes();
        assertEquals(15, attributes.size());
        assertEquals("span-id-123", attributes.get("span_id"));
        assertEquals("trace-id-xyz", attributes.get("trace_id"));
        assertEquals(Vendor.VENDOR, attributes.get("vendor"));
        assertEquals(Vendor.INGEST_SOURCE, attributes.get("ingest_source"));
        assertFalse(((String) attributes.get("id")).isEmpty());
        assertEquals(expectedPromptUserMessage, attributes.get("content"));
        assertEquals("user", attributes.get("role"));
        assertEquals(false, attributes.get("is_response"));
        assertNotNull("request_id should not be null", attributes.get("request_id"));
        assertEquals(expectedResponseModelId, attributes.get("response.model"));
        assertEquals(0, attributes.get("sequence"));
        assertFalse(((String) attributes.get("completion_id")).isEmpty());
        assertEquals(13, attributes.get("token_count"));
        assertEquals("conversation-id-value", attributes.get("llm.conversation_id"));
        assertEquals("testPrefix", attributes.get("llm.testPrefix"));
    }

    @Test
    public void testRecordLlmChatCompletionSummaryEvent() {
        // Instantiate ModelInvocation
        SpringAiModelInvocation springAiModelInvocation = mockSpringAiModelInvocation();

        LlmEvent.Builder builder = new LlmEvent.Builder(springAiModelInvocation);
        LlmEvent llmChatCompletionSummaryEvent = builder
                .spanId() // attribute 1
                .traceId() // attribute 2
                .vendor() // attribute 3
                .ingestSource() // attribute 4
                .id(springAiModelInvocation.getModelResponse().getLlmChatCompletionSummaryId()) // attribute 5
                .requestId() // attribute 6
                .requestTemperature() // attribute 7
                .requestMaxTokens() // attribute 8
                .requestModel() // attribute 9
                .responseModel() // attribute 10
                .responseNumberOfMessages(2) // attribute 11
                .responseChoicesFinishReason() // attribute 12
                .error() // not added
                .duration(9000f) // attribute 13
                .build();

        // attributes 14 & 15 should be the two llm.* prefixed userAttributes

        // Record LlmChatCompletionSummary event
        llmChatCompletionSummaryEvent.recordLlmChatCompletionSummaryEvent();

        // Then
        Collection<Event> customEvents = introspector.getCustomEvents(LLM_CHAT_COMPLETION_SUMMARY);
        assertEquals(1, customEvents.size());

        Event event = customEvents.iterator().next();
        assertEquals(LLM_CHAT_COMPLETION_SUMMARY, event.getType());

        Map<String, Object> attributes = event.getAttributes();
        assertEquals(15, attributes.size());
        assertEquals("span-id-123", attributes.get("span_id"));
        assertEquals("trace-id-xyz", attributes.get("trace_id"));
        assertEquals(Vendor.VENDOR, attributes.get("vendor"));
        assertEquals(Vendor.INGEST_SOURCE, attributes.get("ingest_source"));
        assertFalse(((String) attributes.get("id")).isEmpty());
        assertNotNull("request_id should not be null", attributes.get("request_id"));
        assertEquals(expectedTemp.floatValue(), attributes.get("request.temperature"));
        assertEquals(expectedMaxTokens, attributes.get("request.max_tokens"));
        assertEquals(expectedRequestModelId, attributes.get("request.model"));
        assertEquals(expectedResponseModelId, attributes.get("response.model"));
        assertEquals(2, attributes.get("response.number_of_messages"));
        assertEquals(expectedFinishReason, attributes.get("response.choices.finish_reason"));
        assertEquals(9000f, attributes.get("duration"));
        assertEquals("conversation-id-value", attributes.get("llm.conversation_id"));
        assertEquals("testPrefix", attributes.get("llm.testPrefix"));
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
