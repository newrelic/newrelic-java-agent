package llm.models.converse;
/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

import com.newrelic.agent.bridge.aimonitoring.LlmTokenCountCallbackHolder;
import com.newrelic.agent.introspec.Event;
import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.api.agent.LlmTokenCountCallback;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import static llm.events.LlmEvent.LLM_CHAT_COMPLETION_MESSAGE;
import static llm.events.LlmEvent.LLM_CHAT_COMPLETION_SUMMARY;
import static llm.models.TestUtil.INPUT_TOKENS;
import static llm.models.TestUtil.OUTPUT_TOKENS;
import static llm.models.TestUtil.REQUEST_CONTENT_TEXT;
import static llm.models.TestUtil.REQUEST_MODEL_ID;
import static llm.models.TestUtil.RESPONSE_CONTENT_TEXT;
import static llm.models.TestUtil.STOP_REASON;
import static llm.models.TestUtil.TOTAL_TOKENS;
import static llm.models.TestUtil.assertErrorEvent;
import static llm.models.TestUtil.assertLlmChatCompletionMessageAttributes;
import static llm.models.TestUtil.assertLlmChatCompletionSummaryAttributes;
import static llm.models.TestUtil.mockConverseModelInvocation;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "software.amazon.awssdk.services.bedrockruntime" }, configName = "llm_enabled.yml")
public class ConverseModelInvocationTest {
    private final Introspector introspector = InstrumentationTestRunner.getIntrospector();

    @Before
    public void before() {
        introspector.clear();
        LlmTokenCountCallback llmTokenCountCallback = (model, content) -> 13;
        LlmTokenCountCallbackHolder.setLlmTokenCountCallback(llmTokenCountCallback);
    }

    @Test
    public void testCompletion() {
        boolean isError = false;
        boolean isCompleteUsage = true;

        ConverseModelInvocation converseModelInvocation = mockConverseModelInvocation(REQUEST_MODEL_ID, REQUEST_CONTENT_TEXT, RESPONSE_CONTENT_TEXT,
                isError, isCompleteUsage);
        converseModelInvocation.recordLlmEvents(System.currentTimeMillis());

        Collection<Event> llmChatCompletionMessageEvents = introspector.getCustomEvents(LLM_CHAT_COMPLETION_MESSAGE);
        assertEquals(2, llmChatCompletionMessageEvents.size());
        Iterator<Event> llmChatCompletionMessageEventIterator = llmChatCompletionMessageEvents.iterator();
        Event llmChatCompletionMessageEventOne = llmChatCompletionMessageEventIterator.next();

        assertLlmChatCompletionMessageAttributes(llmChatCompletionMessageEventOne, REQUEST_MODEL_ID, REQUEST_CONTENT_TEXT, RESPONSE_CONTENT_TEXT, false);

        Event llmChatCompletionMessageEventTwo = llmChatCompletionMessageEventIterator.next();

        assertLlmChatCompletionMessageAttributes(llmChatCompletionMessageEventTwo, REQUEST_MODEL_ID, REQUEST_CONTENT_TEXT, RESPONSE_CONTENT_TEXT, true);

        Collection<Event> llmChatCompletionSummaryEvents = introspector.getCustomEvents(LLM_CHAT_COMPLETION_SUMMARY);
        assertEquals(1, llmChatCompletionSummaryEvents.size());
        Iterator<Event> llmChatCompletionSummaryEventIterator = llmChatCompletionSummaryEvents.iterator();
        Event llmChatCompletionSummaryEvent = llmChatCompletionSummaryEventIterator.next();

        assertLlmChatCompletionSummaryAttributes(llmChatCompletionSummaryEvent, REQUEST_MODEL_ID, STOP_REASON);

        assertErrorEvent(isError, introspector.getErrorEvents());
    }

    @Test
    public void testCompletionError() {
        boolean isError = true;
        boolean isCompleteUsage = true;

        ConverseModelInvocation converseModelInvocation = mockConverseModelInvocation(REQUEST_MODEL_ID, REQUEST_CONTENT_TEXT, RESPONSE_CONTENT_TEXT,
                isError, isCompleteUsage);
        converseModelInvocation.recordLlmEvents(System.currentTimeMillis());

        Collection<Event> llmChatCompletionMessageEvents = introspector.getCustomEvents(LLM_CHAT_COMPLETION_MESSAGE);
        assertEquals(2, llmChatCompletionMessageEvents.size());
        Iterator<Event> llmChatCompletionMessageEventIterator = llmChatCompletionMessageEvents.iterator();
        Event llmChatCompletionMessageEventOne = llmChatCompletionMessageEventIterator.next();

        assertLlmChatCompletionMessageAttributes(llmChatCompletionMessageEventOne, REQUEST_MODEL_ID, REQUEST_CONTENT_TEXT, RESPONSE_CONTENT_TEXT, false);

        Event llmChatCompletionMessageEventTwo = llmChatCompletionMessageEventIterator.next();

        assertLlmChatCompletionMessageAttributes(llmChatCompletionMessageEventTwo, REQUEST_MODEL_ID, REQUEST_CONTENT_TEXT, RESPONSE_CONTENT_TEXT, true);

        Collection<Event> llmChatCompletionSummaryEvents = introspector.getCustomEvents(LLM_CHAT_COMPLETION_SUMMARY);
        assertEquals(1, llmChatCompletionSummaryEvents.size());
        Iterator<Event> llmChatCompletionSummaryEventIterator = llmChatCompletionSummaryEvents.iterator();
        Event llmChatCompletionSummaryEvent = llmChatCompletionSummaryEventIterator.next();

        assertLlmChatCompletionSummaryAttributes(llmChatCompletionSummaryEvent, REQUEST_MODEL_ID, STOP_REASON);

        assertErrorEvent(isError, introspector.getErrorEvents());
    }

    @Test
    public void testCompletionWithCompleteUsageData() {
        boolean isError = false;
        boolean isCompleteUsage = true;

        ConverseModelInvocation converseModelInvocation = mockConverseModelInvocation(REQUEST_MODEL_ID, REQUEST_CONTENT_TEXT, RESPONSE_CONTENT_TEXT,
                isError, isCompleteUsage);
        converseModelInvocation.recordLlmEvents(System.currentTimeMillis());

        Collection<Event> llmChatCompletionMessageEvents = introspector.getCustomEvents(LLM_CHAT_COMPLETION_MESSAGE);
        assertEquals(2, llmChatCompletionMessageEvents.size());

        for (Event messageEvent : llmChatCompletionMessageEvents) {
            Map<String, Object> attributes = messageEvent.getAttributes();
            assertEquals(0, attributes.get("token_count"));
        }

        Collection<Event> llmChatCompletionSummaryEvents = introspector.getCustomEvents(LLM_CHAT_COMPLETION_SUMMARY);
        assertEquals(1, llmChatCompletionSummaryEvents.size());
        Event summaryEvent = llmChatCompletionSummaryEvents.iterator().next();
        Map<String, Object> summaryAttributes = summaryEvent.getAttributes();

        assertEquals(INPUT_TOKENS, summaryAttributes.get("response.usage.prompt_tokens"));
        assertEquals(OUTPUT_TOKENS, summaryAttributes.get("response.usage.completion_tokens"));
        assertEquals(TOTAL_TOKENS, summaryAttributes.get("response.usage.total_tokens"));
    }

    @Test
    public void testCompletionWithIncompleteUsageData() {
        boolean isError = false;
        boolean isCompleteUsage = false;

        ConverseModelInvocation converseModelInvocation = mockConverseModelInvocation(REQUEST_MODEL_ID, REQUEST_CONTENT_TEXT, RESPONSE_CONTENT_TEXT,
                isError, isCompleteUsage);
        converseModelInvocation.recordLlmEvents(System.currentTimeMillis());

        Collection<Event> llmChatCompletionMessageEvents = introspector.getCustomEvents(LLM_CHAT_COMPLETION_MESSAGE);
        assertEquals(2, llmChatCompletionMessageEvents.size());

        for (Event messageEvent : llmChatCompletionMessageEvents) {
            Map<String, Object> attributes = messageEvent.getAttributes();
            assertEquals(13, attributes.get("token_count"));
        }

        Collection<Event> llmChatCompletionSummaryEvents = introspector.getCustomEvents(LLM_CHAT_COMPLETION_SUMMARY);
        assertEquals(1, llmChatCompletionSummaryEvents.size());
        Event summaryEvent = llmChatCompletionSummaryEvents.iterator().next();
        Map<String, Object> summaryAttributes = summaryEvent.getAttributes();

        assertFalse(summaryAttributes.containsKey("response.usage.prompt_tokens"));
        assertFalse(summaryAttributes.containsKey("response.usage.completion_tokens"));
        assertFalse(summaryAttributes.containsKey("response.usage.total_tokens"));
    }

    @Test
    public void testCompletionWithNoCallback() {
        boolean isError = false;
        boolean isCompleteUsage = true;

        LlmTokenCountCallbackHolder.setLlmTokenCountCallback(null);

        ConverseModelInvocation converseModelInvocation = mockConverseModelInvocation(REQUEST_MODEL_ID, REQUEST_CONTENT_TEXT, RESPONSE_CONTENT_TEXT,
                isError, isCompleteUsage);
        converseModelInvocation.recordLlmEvents(System.currentTimeMillis());

        Collection<Event> llmChatCompletionMessageEvents = introspector.getCustomEvents(LLM_CHAT_COMPLETION_MESSAGE);
        assertEquals(2, llmChatCompletionMessageEvents.size());

        for (Event messageEvent : llmChatCompletionMessageEvents) {
            Map<String, Object> attributes = messageEvent.getAttributes();
            assertEquals(0, attributes.get("token_count"));
        }
    }
}
