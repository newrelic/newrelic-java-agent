/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package llm.events;

import com.newrelic.agent.bridge.aimonitoring.LlmTokenCountCallbackHolder;
import com.newrelic.agent.introspec.Event;
import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.api.agent.LlmTokenCountCallback;
import llm.models.ModelInvocation;
import llm.models.converse.ConverseModelInvocation;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collection;

import static llm.events.LlmEvent.LLM_CHAT_COMPLETION_MESSAGE;
import static llm.events.LlmEvent.LLM_CHAT_COMPLETION_SUMMARY;
import static llm.models.TestUtil.REQUEST_CONTENT_TEXT;
import static llm.models.TestUtil.REQUEST_MODEL_ID;
import static llm.models.TestUtil.RESPONSE_CONTENT_TEXT;
import static llm.models.TestUtil.STOP_REASON;
import static llm.models.TestUtil.assertLlmChatCompletionMessageAttributes;
import static llm.models.TestUtil.assertLlmChatCompletionSummaryAttributes;
import static llm.models.TestUtil.mockConverseModelInvocation;
import static org.junit.Assert.assertEquals;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "software.amazon.awssdk.services.bedrockruntime" }, configName = "llm_enabled.yml")
public class LlmEventTest {
    private final Introspector introspector = InstrumentationTestRunner.getIntrospector();

    @Before
    public void before() {
        introspector.clear();
        setUp();
    }

    public void setUp() {
        LlmTokenCountCallback llmTokenCountCallback = (model, content) -> 13;
        LlmTokenCountCallbackHolder.setLlmTokenCountCallback(llmTokenCountCallback);
    }

    @Test
    public void testRecordLlmChatCompletionMessageEvent() {
        // Given
        boolean isError = false;
        boolean isCompleteUsage = true;

        ConverseModelInvocation converseModelInvocation = mockConverseModelInvocation(REQUEST_MODEL_ID, REQUEST_CONTENT_TEXT, RESPONSE_CONTENT_TEXT,
                isError, isCompleteUsage);

        LlmEvent.Builder builder = new LlmEvent.Builder(converseModelInvocation);
        LlmEvent llmChatCompletionMessageEvent = builder
                .spanId()
                .traceId()
                .vendor()
                .ingestSource()
                .id(ModelInvocation.getRandomGuid())
                .content(REQUEST_CONTENT_TEXT)
                .role(true)
                .isResponse(true)
                .requestId()
                .responseModel()
                .sequence(0)
                .completionId()
                .tokenCount(LlmTokenCountCallbackHolder.getLlmTokenCountCallback().calculateLlmTokenCount("model", "content"))
                .build();

        // Record LlmChatCompletionMessage event
        llmChatCompletionMessageEvent.recordLlmChatCompletionMessageEvent();

        // Then
        Collection<Event> customEvents = introspector.getCustomEvents(LLM_CHAT_COMPLETION_MESSAGE);
        assertEquals(1, customEvents.size());

        Event event = customEvents.iterator().next();
        assertEquals(LLM_CHAT_COMPLETION_MESSAGE, event.getType());
        assertLlmChatCompletionMessageAttributes(event, REQUEST_MODEL_ID, REQUEST_CONTENT_TEXT, RESPONSE_CONTENT_TEXT, false);
    }

    @Test
    public void testRecordLlmChatCompletionSummaryEvent() {
        // Given
        boolean isError = false;
        boolean isCompleteUsage = true;

        ConverseModelInvocation converseModelInvocation = mockConverseModelInvocation(REQUEST_MODEL_ID, REQUEST_CONTENT_TEXT, RESPONSE_CONTENT_TEXT,
                isError, isCompleteUsage);

        LlmEvent.Builder builder = new LlmEvent.Builder(converseModelInvocation);
        LlmEvent llmChatCompletionSummaryEvent = builder
                .spanId()
                .traceId()
                .vendor()
                .ingestSource()
                .id(converseModelInvocation.getModelResponse().getLlmChatCompletionSummaryId())
                .requestId()
                .requestTemperature()
                .requestMaxTokens()
                .requestModel()
                .responseModel()
                .responseNumberOfMessages(2)
                .responseChoicesFinishReason()
                .responseOrganization() // not added
                .responseUsagePromptTokens()
                .responseUsageCompletionTokens()
                .responseUsageTotalTokens()
                .timeToFirstToken(3)
                .error() // not added
                .duration(9000f)
                .build();

        // Record LlmChatCompletionSummary event
        llmChatCompletionSummaryEvent.recordLlmChatCompletionSummaryEvent();

        // Then
        Collection<Event> customEvents = introspector.getCustomEvents(LLM_CHAT_COMPLETION_SUMMARY);
        assertEquals(1, customEvents.size());

        Event event = customEvents.iterator().next();
        assertEquals(LLM_CHAT_COMPLETION_SUMMARY, event.getType());
        assertLlmChatCompletionSummaryAttributes(event, REQUEST_MODEL_ID, STOP_REASON);
    }
}
