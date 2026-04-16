
/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package llm.embeddings.events;

import com.newrelic.agent.bridge.aimonitoring.LlmTokenCountCallbackHolder;
import com.newrelic.agent.introspec.Event;
import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.api.agent.LlmTokenCountCallback;
import llm.embeddings.models.springai.SpringAiModelInvocation;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static llm.embeddings.events.LlmEvent.LLM_EMBEDDING;
import static org.junit.Assert.assertEquals;
import static util.EmbeddingUtil.assertLlmEmbeddingAttributes;
import static util.EmbeddingUtil.buildEmbeddingRequest;
import static util.EmbeddingUtil.buildEmbeddingResponse;
import static util.EmbeddingUtil.embeddingInputString;
import static util.EmbeddingUtil.embeddingModelId;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "org.springframework.ai.embedding" }, configName = "llm_enabled.yml")
public class LlmEventTest {
    private final Introspector introspector = InstrumentationTestRunner.getIntrospector();

    // Given
    Map<String, String> linkingMetadata = new HashMap<>();

    Map<String, Object> userAttributes = new HashMap<>();

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
    }

    @Test
    public void testRecordLlmChatCompletionMessageEvent() {
        // Instantiate ModelInvocation
        SpringAiModelInvocation springAiModelInvocation = new SpringAiModelInvocation(linkingMetadata, userAttributes, buildEmbeddingRequest(),
                buildEmbeddingResponse());

        // When
        // Build LlmEmbedding event
        LlmEvent.Builder builder = new LlmEvent.Builder(springAiModelInvocation);
        LlmEvent llmEmbeddingEvent = builder
                .id(springAiModelInvocation.getModelResponse().getLlmEmbeddingId())
                .requestId()
                .spanId()
                .traceId()
                .input(0)
                .requestModel()
                .responseModel()
                .responseOrganization()
                .responseUsageTotalTokens()
                .vendor()
                .ingestSource()
                .duration(9000f)
                .error()
                .build();

        // Record LlmEmbedding event
        llmEmbeddingEvent.recordLlmEmbeddingEvent();

        // Then
        Collection<Event> customEvents = introspector.getCustomEvents(LLM_EMBEDDING);
        assertEquals(1, customEvents.size());

        Event event = customEvents.iterator().next();
        assertEquals(LLM_EMBEDDING, event.getType());

        assertLlmEmbeddingAttributes(event, embeddingModelId, embeddingInputString);
    }
}
