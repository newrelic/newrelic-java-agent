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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static llm.completions.events.LlmEvent.LLM_CHAT_COMPLETION_MESSAGE;
import static llm.completions.events.LlmEvent.LLM_CHAT_COMPLETION_SUMMARY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static util.CompletionUtil.assertLlmChatCompletionMessageAttributes;
import static util.CompletionUtil.assertLlmChatCompletionSummaryAttributes;
import static util.CompletionUtil.expectedConversationId;
import static util.CompletionUtil.expectedFinishReason;
import static util.CompletionUtil.expectedGenerationMessage;
import static util.CompletionUtil.expectedPromptUserMessage;
import static util.CompletionUtil.expectedRequestModelId;
import static util.CompletionUtil.expectedResponseModelId;
import static util.CompletionUtil.expectedTemp;
import static util.CompletionUtil.expectedTestPrefix;

@Category({ Java8IncompatibleTest.class, Java11IncompatibleTest.class })
@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "org.springframework.ai.chat.client" }, configName = "llm_enabled.yml")
public class DefaultChatClient_InstrumentationTest {
    private final Introspector introspector = InstrumentationTestRunner.getIntrospector();

    ChatClient chatClient;

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
        assertTransaction("call");
        assertSupportabilityMetrics();
        assertLlmEvents();
        assertTrue(introspector.getErrorEvents().isEmpty());
    }

    @Test
    public void testStreamModelCompletion() {
        Flux<ChatClientResponse> chatClientResponseFlux = streamModelInTransaction();
        List<ChatClientResponse> chatClientResponses = chatClientResponseFlux.collectList().block();
        assertNotNull(chatClientResponses);
        assertNotNull(chatClientResponses.get(0));
        assertTransaction("stream");
        assertSupportabilityMetrics();
        assertLlmEvents();
        assertTrue(introspector.getErrorEvents().isEmpty());
    }

    @Trace(dispatcher = true)
    private ChatClientResponse callModelInTransaction() {
        NewRelic.getAgent().getAiMonitoring().setLlmTokenCountCallback(new TokenCountCallback());
        NewRelic.addCustomParameter("llm.conversation_id", expectedConversationId); // Will be added to LLM events
        NewRelic.addCustomParameter("llm.testPrefix", expectedTestPrefix); // Will be added to LLM events
        NewRelic.addCustomParameter("test", "test"); // Will NOT be added to LLM events
        return chatClient.prompt().user(expectedPromptUserMessage).call().chatClientResponse();
    }

    @Trace(dispatcher = true)
    private Flux<ChatClientResponse> streamModelInTransaction() {
        NewRelic.getAgent().getAiMonitoring().setLlmTokenCountCallback(new TokenCountCallback());
        NewRelic.addCustomParameter("llm.conversation_id", expectedConversationId); // Will be added to LLM events
        NewRelic.addCustomParameter("llm.testPrefix", expectedTestPrefix); // Will be added to LLM events
        NewRelic.addCustomParameter("test", "test"); // Will NOT be added to LLM events
        return chatClient.prompt()
                .user(expectedPromptUserMessage)
                .advisors()
                .stream()
                .chatClientResponse();
    }

    private void assertTransaction(String functionName) {
        assertEquals(1, introspector.getFinishedTransactionCount(TimeUnit.SECONDS.toMillis(2)));
        Collection<String> transactionNames = introspector.getTransactionNames();
        String transactionName = transactionNames.iterator().next();
        Map<String, TracedMetricData> metrics = introspector.getMetricsForTransaction(transactionName);
        assertTrue(metrics.containsKey("Llm/" + ModelResponse.COMPLETION + "/SpringAI/" + functionName));
        assertEquals(1, metrics.get("Llm/" + ModelResponse.COMPLETION + "/SpringAI/" + functionName).getCallCount());
    }

    private void assertSupportabilityMetrics() {
        Map<String, TracedMetricData> unscopedMetrics = introspector.getUnscopedMetrics();
        assertTrue(unscopedMetrics.containsKey("Supportability/Java/ML/SpringAI/1.0.0"));
    }

    private void assertLlmEvents() {
        // LlmChatCompletionMessage events
        Collection<Event> llmChatCompletionMessageEvents = introspector.getCustomEvents(LLM_CHAT_COMPLETION_MESSAGE);
        assertEquals(2, llmChatCompletionMessageEvents.size());

        Iterator<Event> llmChatCompletionMessageEventIterator = llmChatCompletionMessageEvents.iterator();
        // LlmChatCompletionMessage event for user request message
        Event llmChatCompletionMessageEventOne = llmChatCompletionMessageEventIterator.next();
        assertLlmChatCompletionMessageAttributes(llmChatCompletionMessageEventOne, expectedResponseModelId, expectedPromptUserMessage, false);

        // LlmChatCompletionMessage event for assistant response message
        Event llmChatCompletionMessageEventTwo = llmChatCompletionMessageEventIterator.next();
        assertLlmChatCompletionMessageAttributes(llmChatCompletionMessageEventTwo, expectedResponseModelId, expectedGenerationMessage, true);

        // LlmCompletionSummary events
        Collection<Event> llmCompletionSummaryEvents = introspector.getCustomEvents(LLM_CHAT_COMPLETION_SUMMARY);
        assertEquals(1, llmCompletionSummaryEvents.size());

        Iterator<Event> llmCompletionSummaryEventIterator = llmCompletionSummaryEvents.iterator();
        // Summary event for both LlmChatCompletionMessage events
        Event llmCompletionSummaryEvent = llmCompletionSummaryEventIterator.next();
        assertLlmChatCompletionSummaryAttributes(llmCompletionSummaryEvent, expectedRequestModelId, expectedResponseModelId, expectedFinishReason,
                expectedTemp);
    }
}
