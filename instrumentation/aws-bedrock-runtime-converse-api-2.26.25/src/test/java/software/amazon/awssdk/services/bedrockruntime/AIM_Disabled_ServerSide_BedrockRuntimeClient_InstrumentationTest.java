/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package software.amazon.awssdk.services.bedrockruntime;

import com.newrelic.agent.introspec.Event;
import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.agent.introspec.TracedMetricData;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseResponse;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static llm.converse.events.LlmEvent.LLM_CHAT_COMPLETION_MESSAGE;
import static llm.converse.events.LlmEvent.LLM_CHAT_COMPLETION_SUMMARY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static software.amazon.awssdk.services.bedrockruntime.MockConverseRequest.converseRequest;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "software.amazon.awssdk.services.bedrockruntime" }, configName = "llm_disabled_server_side.yml")

public class AIM_Disabled_ServerSide_BedrockRuntimeClient_InstrumentationTest {
    private static final BedrockRuntimeClientMock mockBedrockRuntimeClient = new BedrockRuntimeClientMock();
    private final Introspector introspector = InstrumentationTestRunner.getIntrospector();

    @Before
    public void before() {
        introspector.clear();
    }

    @Test
    public void testConverseCompletion() {
        boolean isError = false;
        ConverseResponse converseResponse = converseRequestInTransaction(converseRequest(isError));

        assertNotNull(converseResponse);
        assertNoLlmTransaction();
        assertNoLlmSupportabilityMetrics();
        assertNoLlmEvents();
        assertTrue(introspector.getErrorEvents().isEmpty());
    }

    @Test
    public void testConverseCompletionError() {
        boolean isError = true;
        ConverseResponse converseResponse = converseRequestInTransaction(converseRequest(isError));

        assertNotNull(converseResponse);
        assertNoLlmTransaction();
        assertNoLlmSupportabilityMetrics();
        assertNoLlmEvents();
        assertTrue(introspector.getErrorEvents().isEmpty());
    }

    @Trace(dispatcher = true)
    private ConverseResponse converseRequestInTransaction(ConverseRequest converseRequest) {
        addCustomParameters();
        return mockBedrockRuntimeClient.converse(converseRequest);
    }

    private void addCustomParameters() {
        NewRelic.addCustomParameter("llm.conversation_id", "conversation-id-value"); // Will be added to LLM events
        NewRelic.addCustomParameter("llm.testPrefix", "testPrefix"); // Will be added to LLM events
        NewRelic.addCustomParameter("test", "test"); // Will NOT be added to LLM events
    }

    private void assertNoLlmTransaction() {
        assertEquals(1, introspector.getFinishedTransactionCount(TimeUnit.SECONDS.toMillis(2)));
        Collection<String> transactionNames = introspector.getTransactionNames();
        String transactionName = transactionNames.iterator().next();
        Map<String, TracedMetricData> metrics = introspector.getMetricsForTransaction(transactionName);
        assertFalse(metrics.containsKey("Llm/completion/Bedrock/converse"));
    }

    private void assertNoLlmSupportabilityMetrics() {
        Map<String, TracedMetricData> unscopedMetrics = introspector.getUnscopedMetrics();
        assertFalse(unscopedMetrics.containsKey("Supportability/Java/ML/Bedrock/2.26.25"));
    }

    private void assertNoLlmEvents() {
        // LlmChatCompletionMessage events
        Collection<Event> llmChatCompletionMessageEvents = introspector.getCustomEvents(LLM_CHAT_COMPLETION_MESSAGE);
        assertEquals(0, llmChatCompletionMessageEvents.size());

        // LlmCompletionSummary events
        Collection<Event> llmCompletionSummaryEvents = introspector.getCustomEvents(LLM_CHAT_COMPLETION_SUMMARY);
        assertEquals(0, llmCompletionSummaryEvents.size());
    }
}
