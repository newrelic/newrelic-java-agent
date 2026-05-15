
/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package llm.embeddings.events;

import com.newrelic.agent.introspec.Event;
import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import llm.embeddings.models.springai.SpringAiModelInvocation;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collection;

import static llm.embeddings.events.LlmEvent.LLM_EMBEDDING;
import static org.junit.Assert.assertEquals;
import static util.EmbeddingUtil.assertLlmEmbeddingAttributes;
import static util.EmbeddingUtil.mockSpringAiModelInvocation;
import static util.EmbeddingUtil.expectedEmbeddingInputString;
import static util.EmbeddingUtil.expectedEmbeddingModelId;
import static util.EmbeddingUtil.setupMockTestEnv;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "org.springframework.ai.embedding" }, configName = "llm_enabled.yml")
public class LlmEventTest {
    private final Introspector introspector = InstrumentationTestRunner.getIntrospector();

    @Before
    public void before() {
        introspector.clear();
        setupMockTestEnv();
    }

    @Test
    public void testRecordLlmChatCompletionMessageEvent() {
        // Instantiate ModelInvocation
        SpringAiModelInvocation springAiModelInvocation = mockSpringAiModelInvocation();

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
        Collection<Event> llmEmbeddingEvents = introspector.getCustomEvents(LLM_EMBEDDING);
        assertEquals(1, llmEmbeddingEvents.size());

        Event event = llmEmbeddingEvents.iterator().next();
        assertEquals(LLM_EMBEDDING, event.getType());

        assertLlmEmbeddingAttributes(event, expectedEmbeddingModelId, expectedEmbeddingInputString);
    }
}
