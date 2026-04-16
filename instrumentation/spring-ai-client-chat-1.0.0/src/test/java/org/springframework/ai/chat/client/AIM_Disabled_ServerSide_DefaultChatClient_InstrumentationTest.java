/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.springframework.ai.chat.client;

import com.newrelic.agent.introspec.Event;
import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.agent.introspec.TracedMetricData;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.test.marker.Java11IncompatibleTest;
import com.newrelic.test.marker.Java8IncompatibleTest;
import io.micrometer.observation.ObservationRegistry;
import llm.completions.models.ModelResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.ai.chat.client.advisor.ChatModelCallAdvisor;
import org.springframework.ai.chat.client.advisor.ChatModelStreamAdvisor;
import reactor.core.publisher.Flux;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static llm.completions.events.LlmEvent.LLM_CHAT_COMPLETION_MESSAGE;
import static llm.completions.events.LlmEvent.LLM_CHAT_COMPLETION_SUMMARY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@Category({ Java8IncompatibleTest.class, Java11IncompatibleTest.class })
@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "org.springframework.ai.chat.client" }, configName = "llm_disabled_server_side.yml")
public class AIM_Disabled_ServerSide_DefaultChatClient_InstrumentationTest {
    private final Introspector introspector = InstrumentationTestRunner.getIntrospector();

    ChatClient chatClient;
    String userPrompt = "Hello, tell me a joke";

    @Before
    public void setup() {
        MockChatModel mockChatModel = new MockChatModel();

        ChatModelStreamAdvisor chatModelStreamAdvisor = ChatModelStreamAdvisor.builder()
                .chatModel(mockChatModel)
                .build();

        ChatModelCallAdvisor chatModelCallAdvisor = ChatModelCallAdvisor.builder().chatModel(mockChatModel).build();

        DefaultChatClientBuilder defaultChatClientBuilder = new DefaultChatClientBuilder(mockChatModel, ObservationRegistry.NOOP, null);

        chatClient = defaultChatClientBuilder.defaultAdvisors(chatModelStreamAdvisor, chatModelCallAdvisor).build();
    }

    @Before
    public void before() {
        introspector.clear();
    }

    @Test
    public void testCallModelCompletion() {
        ChatClientResponse chatClientResponse = callModelInTransaction();
        assertNotNull(chatClientResponse);
        assertNoLlmTransaction();
        assertNoLlmSupportabilityMetrics();
        assertNoLlmEvents();
        assertTrue(introspector.getErrorEvents().isEmpty());
    }

    @Test
    public void testStreamModelCompletion() {
        Flux<ChatClientResponse> chatClientResponseFlux = streamModelInTransaction();
        List<ChatClientResponse> chatClientResponses = chatClientResponseFlux.collectList().block();
        assertNotNull(chatClientResponses);
        assertNotNull(chatClientResponses.get(0));
        assertNoLlmTransaction();
        assertNoLlmSupportabilityMetrics();
        assertNoLlmEvents();
        assertTrue(introspector.getErrorEvents().isEmpty());
    }

    @Trace(dispatcher = true)
    private ChatClientResponse callModelInTransaction() {
        NewRelic.addCustomParameter("llm.conversation_id", "conversation-id-value"); // Will be added to LLM events
        NewRelic.addCustomParameter("llm.testPrefix", "testPrefix"); // Will be added to LLM events
        NewRelic.addCustomParameter("test", "test"); // Will NOT be added to LLM events
        return chatClient.prompt().user(userPrompt).call().chatClientResponse();
    }

    @Trace(dispatcher = true)
    private Flux<ChatClientResponse> streamModelInTransaction() {
        NewRelic.addCustomParameter("llm.conversation_id", "conversation-id-value"); // Will be added to LLM events
        NewRelic.addCustomParameter("llm.testPrefix", "testPrefix"); // Will be added to LLM events
        NewRelic.addCustomParameter("test", "test"); // Will NOT be added to LLM events
        return chatClient.prompt()
                .user(userPrompt)
                .advisors()
                .stream()
                .chatClientResponse();
    }

    private void assertNoLlmTransaction() {
        assertEquals(1, introspector.getFinishedTransactionCount(TimeUnit.SECONDS.toMillis(2)));
        Collection<String> transactionNames = introspector.getTransactionNames();
        String transactionName = transactionNames.iterator().next();
        Map<String, TracedMetricData> metrics = introspector.getMetricsForTransaction(transactionName);
        assertFalse(metrics.containsKey("Llm/" + ModelResponse.COMPLETION + "/Bedrock/invokeModel"));
    }

    private void assertNoLlmSupportabilityMetrics() {
        Map<String, TracedMetricData> unscopedMetrics = introspector.getUnscopedMetrics();
        assertFalse(unscopedMetrics.containsKey("Supportability/Java/ML/Bedrock/2.20"));
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
