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
import java.util.Iterator;

import static llm.embeddings.events.LlmEvent.LLM_EMBEDDING;
import static org.junit.Assert.assertEquals;
import static util.EmbeddingUtil.assertLlmEmbeddingAttributes;
import static util.EmbeddingUtil.mockSpringAiModelInvocation;
import static util.EmbeddingUtil.expectedEmbeddingInputString;
import static util.EmbeddingUtil.expectedEmbeddingModelId;
import static util.EmbeddingUtil.setupMockTestEnv;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "org.springframework.ai.embedding" }, configName = "llm_enabled.yml")
public class SpringAiModelInvocationTest {
    private final Introspector introspector = InstrumentationTestRunner.getIntrospector();

    @Before
    public void before() {
        introspector.clear();
        setupMockTestEnv();
    }

    @Test
    public void testCompletion() {
        // Instantiate ModelInvocation
        SpringAiModelInvocation springAiModelInvocation = mockSpringAiModelInvocation();
        springAiModelInvocation.recordLlmEvents(System.currentTimeMillis());

        Collection<Event> llmEmbeddingEvents = introspector.getCustomEvents(LLM_EMBEDDING);
        assertEquals(1, llmEmbeddingEvents.size());
        Iterator<Event> llmEmbeddingEventIterator = llmEmbeddingEvents.iterator();
        Event llmEmbeddingEvent = llmEmbeddingEventIterator.next();

        assertLlmEmbeddingAttributes(llmEmbeddingEvent, expectedEmbeddingModelId, expectedEmbeddingInputString);
    }
}
