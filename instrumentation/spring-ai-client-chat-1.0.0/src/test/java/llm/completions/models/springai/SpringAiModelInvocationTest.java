/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package llm.completions.models.springai;

import com.newrelic.agent.introspec.Event;
import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collection;
import java.util.Iterator;

import static llm.completions.events.LlmEvent.LLM_CHAT_COMPLETION_MESSAGE;
import static llm.completions.events.LlmEvent.LLM_CHAT_COMPLETION_SUMMARY;
import static org.junit.Assert.assertEquals;
import static util.CompletionUtil.assertLlmChatCompletionMessageAttributes;
import static util.CompletionUtil.assertLlmChatCompletionSummaryAttributes;
import static util.CompletionUtil.expectedFinishReason;
import static util.CompletionUtil.expectedGenerationMessage;
import static util.CompletionUtil.expectedPromptUserMessage;
import static util.CompletionUtil.expectedRequestModelId;
import static util.CompletionUtil.expectedResponseModelId;
import static util.CompletionUtil.expectedTemp;
import static util.CompletionUtil.mockSpringAiModelInvocation;
import static util.CompletionUtil.setupMockTestEnv;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "org.springframework.ai.chat.client" }, configName = "llm_enabled.yml")
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

        Collection<Event> llmChatCompletionMessageEvents = introspector.getCustomEvents(LLM_CHAT_COMPLETION_MESSAGE);
        assertEquals(2, llmChatCompletionMessageEvents.size());
        Iterator<Event> llmChatCompletionMessageEventIterator = llmChatCompletionMessageEvents.iterator();
        Event llmChatCompletionMessageEventOne = llmChatCompletionMessageEventIterator.next();

        assertLlmChatCompletionMessageAttributes(llmChatCompletionMessageEventOne, expectedResponseModelId, expectedPromptUserMessage, false);

        Event llmChatCompletionMessageEventTwo = llmChatCompletionMessageEventIterator.next();

        assertLlmChatCompletionMessageAttributes(llmChatCompletionMessageEventTwo, expectedResponseModelId, expectedGenerationMessage, true);

        Collection<Event> llmChatCompletionSummaryEvents = introspector.getCustomEvents(LLM_CHAT_COMPLETION_SUMMARY);
        assertEquals(1, llmChatCompletionSummaryEvents.size());
        Iterator<Event> llmChatCompletionSummaryEventIterator = llmChatCompletionSummaryEvents.iterator();
        Event llmChatCompletionSummaryEvent = llmChatCompletionSummaryEventIterator.next();

        assertLlmChatCompletionSummaryAttributes(llmChatCompletionSummaryEvent, expectedRequestModelId, expectedResponseModelId, expectedFinishReason,
                expectedTemp);
    }
}
