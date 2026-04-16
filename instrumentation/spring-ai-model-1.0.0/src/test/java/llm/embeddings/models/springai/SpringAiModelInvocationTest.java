/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package llm.embeddings.models.springai;

import com.newrelic.agent.introspec.Event;
import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
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
public class SpringAiModelInvocationTest {

    private final Introspector introspector = InstrumentationTestRunner.getIntrospector();

    // Given
    Map<String, String> linkingMetadata = new HashMap<>();

    Map<String, Object> userAttributes = new HashMap<>();

    @Before
    public void before() {
        introspector.clear();

        linkingMetadata.put("span.id", "span-id-123");
        linkingMetadata.put("trace.id", "trace-id-xyz");

        userAttributes.put("llm.conversation_id", "conversation-id-value");
        userAttributes.put("llm.testPrefix", "testPrefix");
        userAttributes.put("test", "test");
    }

    @Test
    public void testCompletion() {
        // Instantiate ModelInvocation
        SpringAiModelInvocation springAiModelInvocation = new SpringAiModelInvocation(linkingMetadata, userAttributes, buildEmbeddingRequest(),
                buildEmbeddingResponse());
        springAiModelInvocation.recordLlmEvents(System.currentTimeMillis());

        Collection<Event> llmEmbeddingEvents = introspector.getCustomEvents(LLM_EMBEDDING);
        assertEquals(1, llmEmbeddingEvents.size());
        Iterator<Event> llmEmbeddingEventIterator = llmEmbeddingEvents.iterator();
        Event llmEmbeddingEvent = llmEmbeddingEventIterator.next();

        assertLlmEmbeddingAttributes(llmEmbeddingEvent, embeddingModelId, embeddingInputString);
    }
}
