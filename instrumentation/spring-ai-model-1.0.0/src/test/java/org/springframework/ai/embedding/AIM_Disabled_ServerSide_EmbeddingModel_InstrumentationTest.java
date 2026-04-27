/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.springframework.ai.embedding;

import com.newrelic.agent.introspec.Event;
import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.agent.introspec.TracedMetricData;
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
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static llm.embeddings.events.LlmEvent.LLM_EMBEDDING;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static util.EmbeddingUtil.buildEmbeddingRequest;
import static util.EmbeddingUtil.expectedConversationId;
import static util.EmbeddingUtil.expectedTestPrefix;

@Category({ Java8IncompatibleTest.class, Java11IncompatibleTest.class })
@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "org.springframework.ai.embedding" }, configName = "llm_disabled_server_side.yml")
public class AIM_Disabled_ServerSide_EmbeddingModel_InstrumentationTest {
    private final Introspector introspector = InstrumentationTestRunner.getIntrospector();

    @Before
    public void before() {
        introspector.clear();
    }

    @Test
    public void testCallEmbeddingModel() {
        EmbeddingResponse embeddingResponse = callModelInTransaction();
        assertNotNull(embeddingResponse);
        assertNoLlmTransaction("call");
        assertNoLlmSupportabilityMetrics();
        assertNoLlmEvents();
        assertTrue(introspector.getErrorEvents().isEmpty());
    }

    @Trace(dispatcher = true)
    private EmbeddingResponse callModelInTransaction() {
        EmbeddingModel embeddingModel = new MockEmbeddingModel();
        NewRelic.addCustomParameter("llm.conversation_id", expectedConversationId); // Will be added to LLM events
        NewRelic.addCustomParameter("llm.testPrefix", expectedTestPrefix); // Will be added to LLM events
        NewRelic.addCustomParameter("test", "test"); // Will NOT be added to LLM events
        return embeddingModel.call(buildEmbeddingRequest());
    }

    private void assertNoLlmTransaction(String functionName) {
        assertEquals(1, introspector.getFinishedTransactionCount(TimeUnit.SECONDS.toMillis(2)));
        Collection<String> transactionNames = introspector.getTransactionNames();
        String transactionName = transactionNames.iterator().next();
        Map<String, TracedMetricData> metrics = introspector.getMetricsForTransaction(transactionName);
        assertFalse(metrics.containsKey("Llm/" + ModelResponse.EMBEDDING + "/SpringAI/" + functionName));
    }

    private void assertNoLlmSupportabilityMetrics() {
        Map<String, TracedMetricData> unscopedMetrics = introspector.getUnscopedMetrics();
        assertFalse(unscopedMetrics.containsKey("Supportability/Java/ML/SpringAI/1.0.0"));
    }

    private void assertNoLlmEvents() {
        // LlmEmbedding events
        Collection<Event> llmEmbeddingEvents = introspector.getCustomEvents(LLM_EMBEDDING);
        assertEquals(0, llmEmbeddingEvents.size());
    }
}
