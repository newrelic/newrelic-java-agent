/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.springframework.ai.embedding;

import com.newrelic.agent.bridge.aimonitoring.LlmTokenCountCallbackHolder;
import com.newrelic.agent.introspec.Event;
import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.agent.introspec.TracedMetricData;
import com.newrelic.api.agent.LlmTokenCountCallback;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.test.marker.Java11IncompatibleTest;
import com.newrelic.test.marker.Java8IncompatibleTest;
import llm.embeddings.models.ModelResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static llm.embeddings.TestUtil.assertLlmEmbeddingAttributes;
import static llm.embeddings.events.LlmEvent.LLM_EMBEDDING;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.ai.embedding.EmbeddingUtil.buildEmbeddingRequest;
import static org.springframework.ai.embedding.EmbeddingUtil.embeddingInputString;
import static org.springframework.ai.embedding.EmbeddingUtil.embeddingModelId;

@Category({ Java8IncompatibleTest.class, Java11IncompatibleTest.class })
@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "org.springframework.ai.embedding" }, configName = "llm_enabled.yml")
public class EmbeddingModel_InstrumentationTest {
    private final Introspector introspector = InstrumentationTestRunner.getIntrospector();
    EmbeddingModel embeddingModel = new MockEmbeddingModel();

    @Before
    public void before() {
        LlmTokenCountCallback llmTokenCountCallback = (model, content) -> 13;
        LlmTokenCountCallbackHolder.setLlmTokenCountCallback(llmTokenCountCallback);

        introspector.clear();
    }

    @Test
    public void testInvokeModelEmbedding() {
        EmbeddingResponse embeddingResponse = callModelInTransaction();
        assertNotNull(embeddingResponse);
        assertTransaction();
        assertSupportabilityMetrics();
        assertLlmEvents();
        assertTrue(introspector.getErrorEvents().isEmpty());
    }

    @Trace(dispatcher = true)
    private EmbeddingResponse callModelInTransaction() {
        NewRelic.addCustomParameter("llm.conversation_id", "conversation-id-value"); // Will be added to LLM events
        NewRelic.addCustomParameter("llm.testPrefix", "testPrefix"); // Will be added to LLM events
        NewRelic.addCustomParameter("test", "test"); // Will NOT be added to LLM events
        return embeddingModel.call(buildEmbeddingRequest());
    }

    private void assertTransaction() {
        assertEquals(1, introspector.getFinishedTransactionCount(TimeUnit.SECONDS.toMillis(2)));
        Collection<String> transactionNames = introspector.getTransactionNames();
        String transactionName = transactionNames.iterator().next();
        Map<String, TracedMetricData> metrics = introspector.getMetricsForTransaction(transactionName);
        assertTrue(metrics.containsKey("Llm/" + ModelResponse.EMBEDDING + "/SpringAI/call"));
        assertEquals(1, metrics.get("Llm/" + ModelResponse.EMBEDDING + "/SpringAI/call").getCallCount());
    }

    private void assertSupportabilityMetrics() {
        Map<String, TracedMetricData> unscopedMetrics = introspector.getUnscopedMetrics();
        assertTrue(unscopedMetrics.containsKey("Supportability/Java/ML/SpringAI/1.0.0"));
    }

    private void assertLlmEvents() {
        // LlmEmbedding events
        Collection<Event> llmEmbeddingEvents = introspector.getCustomEvents(LLM_EMBEDDING);
        assertEquals(1, llmEmbeddingEvents.size());

        Iterator<Event> llmEmbeddingEventIterator = llmEmbeddingEvents.iterator();
        // LlmEmbedding event
        Event llmEmbeddingEvent = llmEmbeddingEventIterator.next();
        assertLlmEmbeddingAttributes(llmEmbeddingEvent, embeddingModelId, embeddingInputString);
    }
}
