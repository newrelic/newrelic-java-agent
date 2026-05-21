/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package llm.completions.events;

import com.newrelic.agent.bridge.aimonitoring.LlmTokenCountResolver;
import com.newrelic.agent.introspec.Event;
import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import llm.completions.models.ModelInvocation;
import llm.completions.models.springai.SpringAiModelInvocation;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collection;

import static llm.completions.events.LlmEvent.LLM_CHAT_COMPLETION_MESSAGE;
import static llm.completions.events.LlmEvent.LLM_CHAT_COMPLETION_SUMMARY;
import static org.junit.Assert.assertEquals;
import static util.CompletionUtil.assertLlmChatCompletionMessageAttributes;
import static util.CompletionUtil.assertLlmChatCompletionSummaryAttributes;
import static util.CompletionUtil.expectedFinishReason;
import static util.CompletionUtil.expectedPromptUserMessage;
import static util.CompletionUtil.expectedRequestModelId;
import static util.CompletionUtil.expectedResponseModelId;
import static util.CompletionUtil.expectedTemp;
import static util.CompletionUtil.mockSpringAiModelInvocation;
import static util.CompletionUtil.setupMockTestEnv;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "org.springframework.ai.chat.client" }, configName = "llm_enabled.yml")
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

        boolean hasCompleteUsage = LlmTokenCountResolver.hasCompleteUsageData(
                springAiModelInvocation.getModelResponse().getResponseUsagePromptTokens(),
                springAiModelInvocation.getModelResponse().getResponseUsageCompletionTokens(),
                springAiModelInvocation.getModelResponse().getResponseUsageTotalTokens()
        );

        // When
        // Build LlmEmbedding event
        LlmEvent.Builder builder = new LlmEvent.Builder(springAiModelInvocation);
        LlmEvent llmChatCompletionMessageEvent = builder
                .spanId() // attribute 1
                .traceId() // attribute 2
                .vendor() // attribute 3
                .ingestSource() // attribute 4
                .id(ModelInvocation.getRandomGuid()) // attribute 5
                .content(expectedPromptUserMessage) // attribute 6
                .role(true) // attribute 7
                .isResponse(true) // attribute 8
                .requestId() // attribute 9
                .responseModel() // attribute 10
                .sequence(0) // attribute 11
                .completionId() // attribute 12
                .tokenCount(LlmTokenCountResolver.getMessageTokenCount(hasCompleteUsage, expectedResponseModelId, expectedPromptUserMessage)) // attribute 13
                .build();

        // attributes 14 & 15 should be the two llm.* prefixed userAttributes

        // Record LlmChatCompletionMessage event
        llmChatCompletionMessageEvent.recordLlmChatCompletionMessageEvent();

        // Then
        Collection<Event> customEvents = introspector.getCustomEvents(LLM_CHAT_COMPLETION_MESSAGE);
        assertEquals(1, customEvents.size());

        Event event = customEvents.iterator().next();

        assertLlmChatCompletionMessageAttributes(event, expectedResponseModelId, expectedPromptUserMessage, false);
    }

    @Test
    public void testRecordLlmChatCompletionSummaryEvent() {
        // Instantiate ModelInvocation
        SpringAiModelInvocation springAiModelInvocation = mockSpringAiModelInvocation();

        LlmEvent.Builder builder = new LlmEvent.Builder(springAiModelInvocation);
        LlmEvent llmChatCompletionSummaryEvent = builder
                .spanId() // attribute 1
                .traceId() // attribute 2
                .vendor() // attribute 3
                .ingestSource() // attribute 4
                .id(springAiModelInvocation.getModelResponse().getLlmChatCompletionSummaryId()) // attribute 5
                .requestId() // attribute 6
                .requestTemperature() // attribute 7
                .requestMaxTokens() // attribute 8
                .requestModel() // attribute 9
                .responseModel() // attribute 10
                .responseNumberOfMessages(2) // attribute 11
                .responseChoicesFinishReason() // attribute 12
                .error() // not added
                .duration(9000f) // attribute 13
                .timeToFirstToken(1234) // attribute 14
                .build();

        // attributes 15 & 16 should be the two llm.* prefixed userAttributes

        // Record LlmChatCompletionSummary event
        llmChatCompletionSummaryEvent.recordLlmChatCompletionSummaryEvent();

        // Then
        Collection<Event> customEvents = introspector.getCustomEvents(LLM_CHAT_COMPLETION_SUMMARY);
        assertEquals(1, customEvents.size());

        Event event = customEvents.iterator().next();

        assertLlmChatCompletionSummaryAttributes(event, expectedRequestModelId, expectedResponseModelId, expectedFinishReason, expectedTemp);
    }
}
