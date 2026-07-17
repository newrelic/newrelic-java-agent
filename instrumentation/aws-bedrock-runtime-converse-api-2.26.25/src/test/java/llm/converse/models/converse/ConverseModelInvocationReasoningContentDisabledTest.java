/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package llm.converse.models.converse;

import com.newrelic.agent.introspec.Event;
import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlock;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import static llm.converse.events.LlmEvent.LLM_CHAT_COMPLETION_MESSAGE;
import static llm.converse.models.TestUtil.REQUEST_CONTENT_TEXT;
import static llm.converse.models.TestUtil.REQUEST_MODEL_ID;
import static llm.converse.models.TestUtil.RESPONSE_CONTENT_TEXT;
import static llm.converse.models.TestUtil.RESPONSE_REASONING_SIGNATURE;
import static llm.converse.models.TestUtil.RESPONSE_REASONING_TEXT;
import static llm.converse.models.TestUtil.buildReasoningContentBlock;
import static llm.converse.models.TestUtil.mockConverseModelInvocationWithReasoning;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Verifies that reasoning_content is gated by ai_monitoring.record_content.enabled exactly like the existing
 * content attribute, while reasoning_content_signature and reasoning_content_redacted (structural facts, not
 * semantic content) are still captured when record_content is disabled.
 */
@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "software.amazon.awssdk.services.bedrockruntime" }, configName = "llm_record_content_disabled.yml")
public class ConverseModelInvocationReasoningContentDisabledTest {
    private final Introspector introspector = InstrumentationTestRunner.getIntrospector();

    @Before
    public void before() {
        introspector.clear();
    }

    @Test
    public void testReasoningContentGatedByRecordContentEnabled() {
        ContentBlock reasoningBlock = buildReasoningContentBlock(RESPONSE_REASONING_TEXT, RESPONSE_REASONING_SIGNATURE);
        ConverseModelInvocation converseModelInvocation = mockConverseModelInvocationWithReasoning(REQUEST_MODEL_ID, REQUEST_CONTENT_TEXT,
                reasoningBlock, RESPONSE_CONTENT_TEXT);
        converseModelInvocation.recordLlmEvents(System.currentTimeMillis());

        Collection<Event> llmChatCompletionMessageEvents = introspector.getCustomEvents(LLM_CHAT_COMPLETION_MESSAGE);
        assertEquals(3, llmChatCompletionMessageEvents.size());
        Iterator<Event> llmChatCompletionMessageEventIterator = llmChatCompletionMessageEvents.iterator();

        llmChatCompletionMessageEventIterator.next(); // request message, not under test here

        Event reasoningMessageEvent = llmChatCompletionMessageEventIterator.next();
        Map<String, Object> attributes = reasoningMessageEvent.getAttributes();

        assertFalse("content is disabled, reasoning_content should not be recorded", attributes.containsKey("reasoning_content"));
        assertFalse("content is disabled, content should not be recorded", attributes.containsKey("content"));
        assertEquals(RESPONSE_REASONING_SIGNATURE, attributes.get("reasoning_content_signature"));
    }
}
