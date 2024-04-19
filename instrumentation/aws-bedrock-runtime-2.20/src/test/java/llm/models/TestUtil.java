/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package llm.models;

import com.newrelic.agent.introspec.ErrorEvent;
import com.newrelic.agent.introspec.Event;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import static llm.events.LlmEvent.LLM_CHAT_COMPLETION_MESSAGE;
import static llm.events.LlmEvent.LLM_CHAT_COMPLETION_SUMMARY;
import static llm.events.LlmEvent.LLM_EMBEDDING;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestUtil {
    public static void assertLlmChatCompletionMessageAttributes(Event event, String modelId, String requestInput, String responseContent, boolean isResponse) {
        assertEquals(LLM_CHAT_COMPLETION_MESSAGE, event.getType());

        Map<String, Object> attributes = event.getAttributes();
        assertEquals("Java", attributes.get("ingest_source"));
        assertFalse(((String) attributes.get("completion_id")).isEmpty());
        assertFalse(((String) attributes.get("id")).isEmpty());
        assertFalse(((String) attributes.get("request_id")).isEmpty());
        assertEquals("bedrock", attributes.get("vendor"));
        assertEquals(modelId, attributes.get("response.model"));
        assertEquals("testPrefix", attributes.get("llm.testPrefix"));
        assertEquals("conversation-id-value", attributes.get("llm.conversation_id"));
        assertEquals(13, attributes.get("token_count"));

        if (isResponse) {
            assertEquals("assistant", attributes.get("role"));
            assertEquals(responseContent, attributes.get("content"));
            assertEquals(true, attributes.get("is_response"));
            assertEquals(1, attributes.get("sequence"));
        } else {
            assertEquals("user", attributes.get("role"));
            assertEquals(requestInput, attributes.get("content"));
            assertEquals(false, attributes.get("is_response"));
            assertEquals(0, attributes.get("sequence"));
        }
    }

    public static void assertLlmChatCompletionSummaryAttributes(Event event, String modelId, String finishReason) {
        assertEquals(LLM_CHAT_COMPLETION_SUMMARY, event.getType());

        Map<String, Object> attributes = event.getAttributes();
        assertEquals("Java", attributes.get("ingest_source"));
        assertEquals(0.5f, attributes.get("request.temperature"));
        assertTrue(((Float) attributes.get("duration")) >= 0);
        assertEquals(finishReason, attributes.get("response.choices.finish_reason"));
        assertEquals(modelId, attributes.get("request.model"));
        assertEquals("bedrock", attributes.get("vendor"));
        assertEquals(modelId, attributes.get("response.model"));
        assertFalse(((String) attributes.get("id")).isEmpty());
        assertFalse(((String) attributes.get("request_id")).isEmpty());
        assertEquals(2, attributes.get("response.number_of_messages"));
        assertEquals(1000, attributes.get("request.max_tokens"));
        assertEquals("testPrefix", attributes.get("llm.testPrefix"));
        assertEquals("conversation-id-value", attributes.get("llm.conversation_id"));
    }

    public static void assertLlmEmbeddingAttributes(Event event, String modelId, String requestInput) {
        assertEquals(LLM_EMBEDDING, event.getType());

        Map<String, Object> attributes = event.getAttributes();
        assertEquals("Java", attributes.get("ingest_source"));
        assertTrue(((Float) attributes.get("duration")) >= 0);
        assertEquals(requestInput, attributes.get("input"));
        assertEquals(modelId, attributes.get("request.model"));
        assertEquals(modelId, attributes.get("response.model"));
        assertEquals("bedrock", attributes.get("vendor"));
        assertFalse(((String) attributes.get("id")).isEmpty());
        assertFalse(((String) attributes.get("request_id")).isEmpty());
        assertEquals("testPrefix", attributes.get("llm.testPrefix"));
        assertEquals("conversation-id-value", attributes.get("llm.conversation_id"));
        assertEquals(13, attributes.get("token_count"));
    }

    public static void assertErrorEvent(boolean isError, Collection<ErrorEvent> errorEvents) {
        if (isError) {
            assertEquals(1, errorEvents.size());
            Iterator<ErrorEvent> errorEventIterator = errorEvents.iterator();
            ErrorEvent errorEvent = errorEventIterator.next();

            assertEquals("LlmError: BAD_REQUEST", errorEvent.getErrorClass());
            assertEquals("LlmError: BAD_REQUEST", errorEvent.getErrorMessage());

            Map<String, Object> errorEventAttributes = errorEvent.getAttributes();
            assertFalse(errorEventAttributes.isEmpty());
            assertEquals(400, errorEventAttributes.get("error.code"));
            assertEquals(400, errorEventAttributes.get("http.statusCode"));
        } else {
            assertTrue(errorEvents.isEmpty());
        }
    }
}
