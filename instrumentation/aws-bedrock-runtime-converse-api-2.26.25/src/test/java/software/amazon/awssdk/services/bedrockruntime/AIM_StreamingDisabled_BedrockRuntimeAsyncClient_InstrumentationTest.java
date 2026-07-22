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
import software.amazon.awssdk.services.bedrockruntime.model.ConverseStreamRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseStreamResponseHandler;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static llm.converse.events.LlmEvent.LLM_CHAT_COMPLETION_MESSAGE;
import static llm.converse.events.LlmEvent.LLM_CHAT_COMPLETION_SUMMARY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static software.amazon.awssdk.services.bedrockruntime.MockConverseRequest.converseStreamRequest;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "software.amazon.awssdk.services.bedrockruntime" }, configName = "llm_streaming_disabled.yml")

public class AIM_StreamingDisabled_BedrockRuntimeAsyncClient_InstrumentationTest {
    private static final BedrockRuntimeAsyncClientMock mockBedrockRuntimeAsyncClient = new BedrockRuntimeAsyncClientMock();
    private final Introspector introspector = InstrumentationTestRunner.getIntrospector();

    @Before
    public void before() {
        introspector.clear();
    }

    @Test
    public void testConverseStreamCompletion() throws ExecutionException, InterruptedException {
        boolean isError = false;
        Void unused = converseStreamRequestInTransaction(converseStreamRequest(isError));
        assertNull(unused);
        assertStreamTransaction();
        assertSupportabilityMetrics();
        assertNoLlmEvents();
        assertTrue(introspector.getErrorEvents().isEmpty());
    }

    @Trace(dispatcher = true)
    private Void converseStreamRequestInTransaction(ConverseStreamRequest converseStreamRequest) throws ExecutionException, InterruptedException {
        addCustomParameters();

        ConverseStreamResponseHandler converseStreamResponseHandler = ConverseStreamResponseHandler.builder().subscriber(event -> { }).build();
        CompletableFuture<Void> converseResponseFuture = mockBedrockRuntimeAsyncClient.converseStream(converseStreamRequest, converseStreamResponseHandler);
        return converseResponseFuture.get();
    }

    private void addCustomParameters() {
        NewRelic.addCustomParameter("llm.conversation_id", "conversation-id-value");
        NewRelic.addCustomParameter("llm.testPrefix", "testPrefix");
        NewRelic.addCustomParameter("test", "test");
    }

    private void assertStreamTransaction() {
        assertEquals(1, introspector.getFinishedTransactionCount(TimeUnit.SECONDS.toMillis(2)));
        Collection<String> transactionNames = introspector.getTransactionNames();
        String transactionName = transactionNames.iterator().next();
        Map<String, TracedMetricData> metrics = introspector.getMetricsForTransaction(transactionName);

        assertTrue(metrics.containsKey("Llm/completion/Bedrock/converseStream"));
        assertEquals(1, metrics.get("Llm/completion/Bedrock/converseStream").getCallCount());
    }

    private void assertSupportabilityMetrics() {
        Map<String, TracedMetricData> unscopedMetrics = introspector.getUnscopedMetrics();
        assertTrue(unscopedMetrics.containsKey("Supportability/Java/ML/Bedrock/2.26.25"));
        assertTrue(unscopedMetrics.containsKey("Supportability/Java/ML/Streaming/Disabled"));
    }

    private void assertNoLlmEvents() {
        Collection<Event> llmChatCompletionMessageEvents = introspector.getCustomEvents(LLM_CHAT_COMPLETION_MESSAGE);
        assertEquals(0, llmChatCompletionMessageEvents.size());

        Collection<Event> llmCompletionSummaryEvents = introspector.getCustomEvents(LLM_CHAT_COMPLETION_SUMMARY);
        assertEquals(0, llmCompletionSummaryEvents.size());
    }
}
