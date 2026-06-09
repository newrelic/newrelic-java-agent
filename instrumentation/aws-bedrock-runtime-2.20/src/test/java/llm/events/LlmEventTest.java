/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package llm.events;

import com.newrelic.agent.bridge.aimonitoring.LlmTokenCountCallbackHolder;
import com.newrelic.agent.introspec.Event;
import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.api.agent.LlmTokenCountCallback;
import llm.models.ModelInvocation;
import llm.models.amazon.titan.TitanModelInvocation;
import llm.models.anthropic.claude.ClaudeModelInvocation;
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
import java.util.Map;
import java.util.Optional;

import static llm.events.LlmEvent.Builder;
import static llm.events.LlmEvent.LLM_CHAT_COMPLETION_MESSAGE;
import static llm.events.LlmEvent.LLM_CHAT_COMPLETION_SUMMARY;
import static llm.events.LlmEvent.LLM_EMBEDDING;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "software.amazon.awssdk.services.bedrockruntime" }, configName = "llm_enabled.yml")
public class LlmEventTest {
    private final Introspector introspector = InstrumentationTestRunner.getIntrospector();

    @Before
    public void before() {
        introspector.clear();
        setUp();
    }

    public void setUp() {
        LlmTokenCountCallback llmTokenCountCallback = (model, content) -> 13;
        LlmTokenCountCallbackHolder.setLlmTokenCountCallback(llmTokenCountCallback);
    }

    @Test
    public void testRecordLlmEmbeddingEvent() {
        // Given
        Map<String, String> linkingMetadata = new HashMap<>();
        linkingMetadata.put("span.id", "span-id-123");
        linkingMetadata.put("trace.id", "trace-id-xyz");

        Map<String, Object> userAttributes = new HashMap<>();
        userAttributes.put("llm.conversation_id", "conversation-id-890");
        userAttributes.put("llm.testPrefix", "testPrefix");
        userAttributes.put("test", "test");

        // Mock out ModelRequest
        InvokeModelRequest mockInvokeModelRequest = mock(InvokeModelRequest.class);
        SdkBytes mockRequestSdkBytes = mock(SdkBytes.class);
        when(mockInvokeModelRequest.body()).thenReturn(mockRequestSdkBytes);
        when(mockRequestSdkBytes.asUtf8String()).thenReturn("{\"inputText\":\"What is the color of the sky?\"}");
        when(mockInvokeModelRequest.modelId()).thenReturn("amazon.titan-embed-text-v1");

        // Mock out ModelResponse
        InvokeModelResponse mockInvokeModelResponse = mock(InvokeModelResponse.class);
        SdkBytes mockResponseSdkBytes = mock(SdkBytes.class);
        when(mockInvokeModelResponse.body()).thenReturn(mockResponseSdkBytes);
        when(mockResponseSdkBytes.asUtf8String()).thenReturn("{\"embedding\":[0.328125,0.44335938],\"inputTextTokenCount\":8}");

        SdkHttpResponse mockSdkHttpResponse = mock(SdkHttpResponse.class);
        when(mockInvokeModelResponse.sdkHttpResponse()).thenReturn(mockSdkHttpResponse);
        when(mockSdkHttpResponse.isSuccessful()).thenReturn(true);
        when(mockSdkHttpResponse.statusCode()).thenReturn(200);
        when(mockSdkHttpResponse.statusText()).thenReturn(Optional.of("OK"));

        BedrockRuntimeResponseMetadata mockBedrockRuntimeResponseMetadata = mock(BedrockRuntimeResponseMetadata.class);
        when(mockInvokeModelResponse.responseMetadata()).thenReturn(mockBedrockRuntimeResponseMetadata);
        when(mockBedrockRuntimeResponseMetadata.requestId()).thenReturn("90a22e92-db1d-4474-97a9-28b143846301");

        // Instantiate ModelInvocation
        TitanModelInvocation titanModelInvocation = new TitanModelInvocation(linkingMetadata, userAttributes, mockInvokeModelRequest,
                mockInvokeModelResponse);

        // When
        // Build LlmEmbedding event
        Builder builder = new Builder(titanModelInvocation);
        LlmEvent llmEmbeddingEvent = builder
                .spanId() // attribute 1
                .traceId() // attribute 2
                .vendor() // attribute 3
                .ingestSource() // attribute 4
                .id(titanModelInvocation.getModelResponse().getLlmEmbeddingId()) // attribute 5
                .requestId() // attribute 6
                .input(0) // attribute 7
                .requestModel() // attribute 8
                .responseModel() // attribute 9
                .tokenCount(123) // attribute 10
                .error() // not added
                .duration(9000f) // attribute 11
                .build();

        // attributes 12 & 13 should be the two llm.* prefixed userAttributes

        // Record LlmEmbedding event
        llmEmbeddingEvent.recordLlmEmbeddingEvent();

        // Then
        Collection<Event> customEvents = introspector.getCustomEvents(LLM_EMBEDDING);
        assertEquals(1, customEvents.size());

        Event event = customEvents.iterator().next();
        assertEquals(LLM_EMBEDDING, event.getType());

        Map<String, Object> attributes = event.getAttributes();
        assertEquals(13, attributes.size());
        assertEquals("span-id-123", attributes.get("span_id"));
        assertEquals("trace-id-xyz", attributes.get("trace_id"));
        assertEquals("bedrock", attributes.get("vendor"));
        assertEquals("Java", attributes.get("ingest_source"));
        assertFalse(((String) attributes.get("id")).isEmpty());
        assertEquals("90a22e92-db1d-4474-97a9-28b143846301", attributes.get("request_id"));
        assertEquals("What is the color of the sky?", attributes.get("input"));
        assertEquals("amazon.titan-embed-text-v1", attributes.get("request.model"));
        assertEquals("amazon.titan-embed-text-v1", attributes.get("response.model"));
        assertEquals(123, attributes.get("token_count"));
        assertEquals(9000f, attributes.get("duration"));
        assertEquals("conversation-id-890", attributes.get("llm.conversation_id"));
        assertEquals("testPrefix", attributes.get("llm.testPrefix"));
    }

    @Test
    public void testRecordLlmChatCompletionMessageEvent() {
        // Given
        Map<String, String> linkingMetadata = new HashMap<>();
        linkingMetadata.put("span.id", "span-id-123");
        linkingMetadata.put("trace.id", "trace-id-xyz");

        Map<String, Object> userAttributes = new HashMap<>();
        userAttributes.put("llm.conversation_id", "conversation-id-890");
        userAttributes.put("llm.testPrefix", "testPrefix");
        userAttributes.put("test", "test");

        String expectedUserPrompt = "Human: What is the color of the sky?\n\nAssistant:";

        // Mock out ModelRequest
        InvokeModelRequest mockInvokeModelRequest = mock(InvokeModelRequest.class);
        SdkBytes mockRequestSdkBytes = mock(SdkBytes.class);
        when(mockInvokeModelRequest.body()).thenReturn(mockRequestSdkBytes);
        when(mockRequestSdkBytes.asUtf8String())
                .thenReturn(
                        "{\"stop_sequences\":[\"\\n\\nHuman:\"],\"max_tokens_to_sample\":1000,\"temperature\":0.5,\"prompt\":\"Human: What is the color of the sky?\\n\\nAssistant:\"}");
        when(mockInvokeModelRequest.modelId()).thenReturn("anthropic.claude-v2");

        // Mock out ModelResponse
        InvokeModelResponse mockInvokeModelResponse = mock(InvokeModelResponse.class);
        SdkBytes mockResponseSdkBytes = mock(SdkBytes.class);
        when(mockInvokeModelResponse.body()).thenReturn(mockResponseSdkBytes);
        when(mockResponseSdkBytes.asUtf8String())
                .thenReturn(
                        "{\"completion\":\" The sky appears blue during the day because of how sunlight interacts with the gases in Earth's atmosphere.\",\"stop_reason\":\"stop_sequence\",\"stop\":\"\\n\\nHuman:\"}");

        SdkHttpResponse mockSdkHttpResponse = mock(SdkHttpResponse.class);
        when(mockInvokeModelResponse.sdkHttpResponse()).thenReturn(mockSdkHttpResponse);
        when(mockSdkHttpResponse.isSuccessful()).thenReturn(true);
        when(mockSdkHttpResponse.statusCode()).thenReturn(200);
        when(mockSdkHttpResponse.statusText()).thenReturn(Optional.of("OK"));

        BedrockRuntimeResponseMetadata mockBedrockRuntimeResponseMetadata = mock(BedrockRuntimeResponseMetadata.class);
        when(mockInvokeModelResponse.responseMetadata()).thenReturn(mockBedrockRuntimeResponseMetadata);
        when(mockBedrockRuntimeResponseMetadata.requestId()).thenReturn("90a22e92-db1d-4474-97a9-28b143846301");

        ClaudeModelInvocation claudeModelInvocation = new ClaudeModelInvocation(linkingMetadata, userAttributes, mockInvokeModelRequest,
                mockInvokeModelResponse);

        LlmEvent.Builder builder = new LlmEvent.Builder(claudeModelInvocation);
        LlmEvent llmChatCompletionMessageEvent = builder
                .spanId() // attribute 1
                .traceId() // attribute 2
                .vendor() // attribute 3
                .ingestSource() // attribute 4
                .id(ModelInvocation.getRandomGuid()) // attribute 5
                .content(expectedUserPrompt) // attribute 6
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
        assertEquals("bedrock", attributes.get("vendor"));
        assertEquals("Java", attributes.get("ingest_source"));
        assertFalse(((String) attributes.get("id")).isEmpty());
        assertEquals(expectedUserPrompt, attributes.get("content"));
        assertEquals("user", attributes.get("role"));
        assertEquals(false, attributes.get("is_response"));
        assertEquals("90a22e92-db1d-4474-97a9-28b143846301", attributes.get("request_id"));
        assertEquals("anthropic.claude-v2", attributes.get("response.model"));
        assertEquals(0, attributes.get("sequence"));
        assertFalse(((String) attributes.get("completion_id")).isEmpty());
        assertEquals(13, attributes.get("token_count"));
        assertEquals("conversation-id-890", attributes.get("llm.conversation_id"));
        assertEquals("testPrefix", attributes.get("llm.testPrefix"));
    }

    @Test
    public void testRecordLlmChatCompletionSummaryEvent() {
        // Given
        Map<String, String> linkingMetadata = new HashMap<>();
        linkingMetadata.put("span.id", "span-id-123");
        linkingMetadata.put("trace.id", "trace-id-xyz");

        Map<String, Object> userAttributes = new HashMap<>();
        userAttributes.put("llm.conversation_id", "conversation-id-890");
        userAttributes.put("llm.testPrefix", "testPrefix");
        userAttributes.put("test", "test");

        // Mock out ModelRequest
        InvokeModelRequest mockInvokeModelRequest = mock(InvokeModelRequest.class);
        SdkBytes mockRequestSdkBytes = mock(SdkBytes.class);
        when(mockInvokeModelRequest.body()).thenReturn(mockRequestSdkBytes);
        when(mockRequestSdkBytes.asUtf8String())
                .thenReturn(
                        "{\"stop_sequences\":[\"\\n\\nHuman:\"],\"max_tokens_to_sample\":1000,\"temperature\":0.5,\"prompt\":\"Human: What is the color of the sky?\\n\\nAssistant:\"}");
        when(mockInvokeModelRequest.modelId()).thenReturn("anthropic.claude-v2");

        // Mock out ModelResponse
        InvokeModelResponse mockInvokeModelResponse = mock(InvokeModelResponse.class);
        SdkBytes mockResponseSdkBytes = mock(SdkBytes.class);
        when(mockInvokeModelResponse.body()).thenReturn(mockResponseSdkBytes);
        when(mockResponseSdkBytes.asUtf8String())
                .thenReturn(
                        "{\"completion\":\" The sky appears blue during the day because of how sunlight interacts with the gases in Earth's atmosphere.\",\"stop_reason\":\"stop_sequence\",\"stop\":\"\\n\\nHuman:\"}");

        SdkHttpResponse mockSdkHttpResponse = mock(SdkHttpResponse.class);
        when(mockInvokeModelResponse.sdkHttpResponse()).thenReturn(mockSdkHttpResponse);
        when(mockSdkHttpResponse.isSuccessful()).thenReturn(true);
        when(mockSdkHttpResponse.statusCode()).thenReturn(200);
        when(mockSdkHttpResponse.statusText()).thenReturn(Optional.of("OK"));

        BedrockRuntimeResponseMetadata mockBedrockRuntimeResponseMetadata = mock(BedrockRuntimeResponseMetadata.class);
        when(mockInvokeModelResponse.responseMetadata()).thenReturn(mockBedrockRuntimeResponseMetadata);
        when(mockBedrockRuntimeResponseMetadata.requestId()).thenReturn("90a22e92-db1d-4474-97a9-28b143846301");

        ClaudeModelInvocation claudeModelInvocation = new ClaudeModelInvocation(linkingMetadata, userAttributes, mockInvokeModelRequest,
                mockInvokeModelResponse);

        LlmEvent.Builder builder = new LlmEvent.Builder(claudeModelInvocation);
        LlmEvent llmChatCompletionSummaryEvent = builder
                .spanId() // attribute 1
                .traceId() // attribute 2
                .vendor() // attribute 3
                .ingestSource() // attribute 4
                .id(claudeModelInvocation.getModelResponse().getLlmChatCompletionSummaryId()) // attribute 5
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
        assertEquals("bedrock", attributes.get("vendor"));
        assertEquals("Java", attributes.get("ingest_source"));
        assertFalse(((String) attributes.get("id")).isEmpty());
        assertEquals("90a22e92-db1d-4474-97a9-28b143846301", attributes.get("request_id"));
        assertEquals(0.5f, attributes.get("request.temperature"));
        assertEquals(1000, attributes.get("request.max_tokens"));
        assertEquals("anthropic.claude-v2", attributes.get("request.model"));
        assertEquals("anthropic.claude-v2", attributes.get("response.model"));
        assertEquals(2, attributes.get("response.number_of_messages"));
        assertEquals("stop_sequence", attributes.get("response.choices.finish_reason"));
        assertEquals(9000f, attributes.get("duration"));
        assertEquals("conversation-id-890", attributes.get("llm.conversation_id"));
        assertEquals("testPrefix", attributes.get("llm.testPrefix"));
    }

    @Test
    public void testRecordLlmChatCompletionSummaryEventWithCompleteUsage() {

        Map<String, String> linkingMetadata = new HashMap<>();
        linkingMetadata.put("span.id", "span-id-123");
        linkingMetadata.put("trace.id", "trace-id-xyz");

        Map<String, Object> userAttributes = new HashMap<>();
        userAttributes.put("llm.conversation_id", "conversation-id-890");
        userAttributes.put("llm.testPrefix", "testPrefix");
        userAttributes.put("test", "test");

        InvokeModelRequest mockInvokeModelRequest = mock(InvokeModelRequest.class);
        SdkBytes mockRequestSdkBytes = mock(SdkBytes.class);
        when(mockInvokeModelRequest.body()).thenReturn(mockRequestSdkBytes);
        when(mockRequestSdkBytes.asUtf8String()).thenReturn("{\"inputText\":\"What is the color of the\n" +
                "  sky?\",\"textGenerationConfig\":{\"temperature\":0.5,\"maxTokenCount\":1000}}");
        when(mockInvokeModelRequest.modelId()).thenReturn("amazon.titan-text-express-v1");

        InvokeModelResponse mockInvokeModelResponse = mock(InvokeModelResponse.class);
        SdkBytes mockResponseSdkBytes = mock(SdkBytes.class);
        when(mockInvokeModelResponse.body()).thenReturn(mockResponseSdkBytes);
        when(mockResponseSdkBytes.asUtf8String()).thenReturn("{\"results\":[{\"tokenCount\":9,\"outputText\":\"The sky is blue.\",\"completionReason\":\"FINISH\"}],\"inputTextTokenCount\":8}");

        SdkHttpResponse mockSdkHttpResponse = mock(SdkHttpResponse.class);
        when(mockInvokeModelResponse.sdkHttpResponse()).thenReturn(mockSdkHttpResponse);
        when(mockSdkHttpResponse.isSuccessful()).thenReturn(true);
        when(mockSdkHttpResponse.statusCode()).thenReturn(200);
        when(mockSdkHttpResponse.statusText()).thenReturn(Optional.of("OK"));

        BedrockRuntimeResponseMetadata mockBedrockRuntimeResponseMetadata = mock(BedrockRuntimeResponseMetadata.class);
        when(mockInvokeModelResponse.responseMetadata()).thenReturn(mockBedrockRuntimeResponseMetadata);
        when(mockBedrockRuntimeResponseMetadata.requestId()).thenReturn("90a22e92-db1d-4474-97a9-28b143846301");

        TitanModelInvocation titanModelInvocation = new TitanModelInvocation(linkingMetadata, userAttributes, mockInvokeModelRequest,
                mockInvokeModelResponse);

        LlmEvent.Builder builder = new LlmEvent.Builder(titanModelInvocation);
        LlmEvent llmChatCompletionSummaryEvent = builder
                .spanId()
                .traceId()
                .vendor()
                .ingestSource()
                .id(titanModelInvocation.getModelResponse().getLlmChatCompletionSummaryId())
                .requestId()
                .requestTemperature()
                .requestModel()
                .responseModel()
                .responseNumberOfMessages(2)
                .responseChoicesFinishReason()
                .duration(9000f)
                .responseUsagePromptTokens()
                .responseUsageCompletionTokens()
                .responseUsageTotalTokens()
                .build();

        llmChatCompletionSummaryEvent.recordLlmChatCompletionSummaryEvent();

        Collection<Event> customEvents = introspector.getCustomEvents(LLM_CHAT_COMPLETION_SUMMARY);
        assertEquals(1, customEvents.size());

        Event event = customEvents.iterator().next();
        Map<String, Object> attributes = event.getAttributes();

        assertEquals(17, attributes.size());
        assertEquals(8, attributes.get("response.usage.prompt_tokens"));
        assertEquals(9, attributes.get("response.usage.completion_tokens"));
        assertEquals(17,  attributes.get("response.usage.total_tokens"));
    }

    @Test
    public void testResponseUsageFieldsOmittedWhenNull() {

        Map<String, String> linkingMetadata = new HashMap<>();
        linkingMetadata.put("span.id", "span-id-123");
        linkingMetadata.put("trace.id", "trace-id-xyz");

        Map<String, Object> userAttributes = new HashMap<>();
        userAttributes.put("llm.conversation_id", "conversation-id-890");
        userAttributes.put("llm.testPrefix", "testPrefix");
        userAttributes.put("test", "test");

        String expectedUserPrompt = "Human: What is the color of the sky?\n\nAssistant:";

        InvokeModelRequest mockInvokeModelRequest = mock(InvokeModelRequest.class);
        SdkBytes mockRequestSdkBytes = mock(SdkBytes.class);
        when(mockInvokeModelRequest.body()).thenReturn(mockRequestSdkBytes);
        when(mockRequestSdkBytes.asUtf8String()).thenReturn(
                        "{\"stop_sequences\":[\"\\n\\nHuman:\"],\"max_tokens_to_sample\":1000,\"temperature\":0.5,\"prompt\":\"Human: What is the color of the sky?\\n\\nAssistant:\"}");
        when(mockInvokeModelRequest.modelId()).thenReturn("anthropic.claude-v2");

        InvokeModelResponse mockInvokeModelResponse = mock(InvokeModelResponse.class);
        SdkBytes mockResponseSdkBytes = mock(SdkBytes.class);
        when(mockInvokeModelResponse.body()).thenReturn(mockResponseSdkBytes);
        when(mockResponseSdkBytes.asUtf8String()).thenReturn("{\"completion\":\" The sky appears blue during the day because of how sunlight interacts with the gases in Earth's atmosphere.\",\"stop_reason\":\"stop_sequence\",\"stop\":\"\\n\\nHuman:\"}");

        SdkHttpResponse mockSdkHttpResponse = mock(SdkHttpResponse.class);
        when(mockInvokeModelResponse.sdkHttpResponse()).thenReturn(mockSdkHttpResponse);
        when(mockSdkHttpResponse.isSuccessful()).thenReturn(true);
        when(mockSdkHttpResponse.statusCode()).thenReturn(200);
        when(mockSdkHttpResponse.statusText()).thenReturn(Optional.of("OK"));

        BedrockRuntimeResponseMetadata mockBedrockRuntimeResponseMetadata = mock(BedrockRuntimeResponseMetadata.class);
        when(mockInvokeModelResponse.responseMetadata()).thenReturn(mockBedrockRuntimeResponseMetadata);
        when(mockBedrockRuntimeResponseMetadata.requestId()).thenReturn("90a22e92-db1d-4474-97a9-28b143846301");

        ClaudeModelInvocation claudeModelInvocation = new ClaudeModelInvocation(linkingMetadata, userAttributes, mockInvokeModelRequest,
                mockInvokeModelResponse);

        LlmEvent.Builder builder = new LlmEvent.Builder(claudeModelInvocation);
        LlmEvent event = builder
                .spanId()
                .traceId()
                .vendor()
                .ingestSource()
                .id(ModelInvocation.getRandomGuid())
                .content(expectedUserPrompt)
                .role(true)
                .isResponse(true)
                .requestId()
                .responseModel()
                .sequence(0)
                .completionId()
                .responseUsagePromptTokens()
                .responseUsageCompletionTokens()
                .responseUsageTotalTokens()
                .tokenCount(LlmTokenCountCallbackHolder.getLlmTokenCountCallback().calculateLlmTokenCount("model", "content"))
                .build();

        event.recordLlmChatCompletionSummaryEvent();

        Collection<Event> customEvents = introspector.getCustomEvents(LLM_CHAT_COMPLETION_SUMMARY);
        Event recordedEvent = customEvents.iterator().next();
        Map<String, Object> attributes = recordedEvent.getAttributes();

        assertFalse(attributes.containsKey("response.usage.prompt_tokens"));
        assertFalse(attributes.containsKey("response.usage.completion_tokens"));
        assertFalse(attributes.containsKey("response.usage.total_tokens"));
    }

    @Test
    public void testResponseUsageFieldsIncludedWhenZero() {

        Map<String, String> linkingMetadata = new HashMap<>();
        Map<String, Object> userAttributes = new HashMap<>();

        InvokeModelRequest mockInvokeModelRequest = mock(InvokeModelRequest.class);
        SdkBytes mockRequestSdkBytes = mock(SdkBytes.class);
        when(mockInvokeModelRequest.body()).thenReturn(mockRequestSdkBytes);
        when(mockRequestSdkBytes.asUtf8String()).thenReturn("{\"inputText\":\"test\"}");
        when(mockInvokeModelRequest.modelId()).thenReturn("amazon.titan-text-express-v1");

        InvokeModelResponse mockInvokeModelResponse = mock(InvokeModelResponse.class);
        SdkBytes mockResponseSdkBytes = mock(SdkBytes.class);
        when(mockInvokeModelResponse.body()).thenReturn(mockResponseSdkBytes);

        when(mockResponseSdkBytes.asUtf8String()).thenReturn(
                "{\"results\":[{\"tokenCount\":0,\"outputText\":\"test\"}],\"inputTextTokenCount\":0}"
        );

        SdkHttpResponse mockSdkHttpResponse = mock(SdkHttpResponse.class);
        when(mockInvokeModelResponse.sdkHttpResponse()).thenReturn(mockSdkHttpResponse);
        when(mockSdkHttpResponse.isSuccessful()).thenReturn(true);

        BedrockRuntimeResponseMetadata mockMetadata = mock(BedrockRuntimeResponseMetadata.class);
        when(mockInvokeModelResponse.responseMetadata()).thenReturn(mockMetadata);
        when(mockMetadata.requestId()).thenReturn("test-id");

        TitanModelInvocation titanModelInvocation = new TitanModelInvocation(
                linkingMetadata,
                userAttributes,
                mockInvokeModelRequest,
                mockInvokeModelResponse
        );

        LlmEvent.Builder builder = new LlmEvent.Builder(titanModelInvocation);
        LlmEvent event = builder
                .spanId()
                .responseUsagePromptTokens()
                .responseUsageCompletionTokens()
                .responseUsageTotalTokens()
                .build();

        event.recordLlmChatCompletionSummaryEvent();

        Collection<Event> customEvents = introspector.getCustomEvents(LLM_CHAT_COMPLETION_SUMMARY);
        Event recordedEvent = customEvents.iterator().next();
        Map<String, Object> attributes = recordedEvent.getAttributes();

        assertTrue(attributes.containsKey("response.usage.prompt_tokens"));
        assertTrue(attributes.containsKey("response.usage.completion_tokens"));
        assertTrue(attributes.containsKey("response.usage.total_tokens"));
        assertEquals(0, attributes.get("response.usage.prompt_tokens"));
        assertEquals(0, attributes.get("response.usage.completion_tokens"));
        assertEquals(0, attributes.get("response.usage.total_tokens"));
    }

    @Test
    public void testRecordLlmEmbeddingEventWithPartialUsageOmitted() {

        Map<String, String> linkingMetadata = new HashMap<>();
        Map<String, Object> userAttributes = new HashMap<>();

        InvokeModelRequest mockInvokeModelRequest = mock(InvokeModelRequest.class);
        SdkBytes mockRequestSdkBytes = mock(SdkBytes.class);
        when(mockInvokeModelRequest.body()).thenReturn(mockRequestSdkBytes);
        when(mockRequestSdkBytes.asUtf8String()).thenReturn("{\"inputText\":\"test embedding\"}");
        when(mockInvokeModelRequest.modelId()).thenReturn("amazon.titan-embed-text-v1");

        InvokeModelResponse mockInvokeModelResponse = mock(InvokeModelResponse.class);
        SdkBytes mockResponseSdkBytes = mock(SdkBytes.class);
        when(mockInvokeModelResponse.body()).thenReturn(mockResponseSdkBytes);

        when(mockResponseSdkBytes.asUtf8String()).thenReturn(
                "{\"embedding\":[0.1,0.2,0.3],\"inputTextTokenCount\":8}"
        );

        SdkHttpResponse mockSdkHttpResponse = mock(SdkHttpResponse.class);
        when(mockInvokeModelResponse.sdkHttpResponse()).thenReturn(mockSdkHttpResponse);
        when(mockSdkHttpResponse.isSuccessful()).thenReturn(true);

        BedrockRuntimeResponseMetadata mockMetadata = mock(BedrockRuntimeResponseMetadata.class);
        when(mockInvokeModelResponse.responseMetadata()).thenReturn(mockMetadata);
        when(mockMetadata.requestId()).thenReturn("test-id");

        TitanModelInvocation titanModelInvocation = new TitanModelInvocation(
                linkingMetadata,
                userAttributes,
                mockInvokeModelRequest,
                mockInvokeModelResponse
        );

        LlmEvent.Builder builder = new LlmEvent.Builder(titanModelInvocation);
        LlmEvent llmEmbeddingEvent = builder
                .spanId()
                .traceId()
                .vendor()
                .ingestSource()
                .id(titanModelInvocation.getModelResponse().getLlmEmbeddingId())
                .requestId()
                .input(0)
                .requestModel()
                .responseModel()
                .tokenCount(13)
                .duration(9000f)
                .build();

        llmEmbeddingEvent.recordLlmEmbeddingEvent();

        Collection<Event> customEvents = introspector.getCustomEvents(LLM_EMBEDDING);
        Event event = customEvents.iterator().next();
        Map<String, Object> attributes = event.getAttributes();

        assertFalse(attributes.containsKey("response.usage.prompt_tokens"));
        assertFalse(attributes.containsKey("response.usage.completion_tokens"));
        assertFalse(attributes.containsKey("response.usage.total_tokens"));

        // token_count should use callback value (13) since usage is incomplete
        assertEquals(13, attributes.get("token_count"));
    }

}
