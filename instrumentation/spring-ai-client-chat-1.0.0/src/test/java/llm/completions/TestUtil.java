/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package llm.completions;

import com.newrelic.agent.introspec.Event;
import llm.completions.vendor.Vendor;

import java.util.Map;

import static llm.completions.events.LlmEvent.LLM_CHAT_COMPLETION_MESSAGE;
import static llm.completions.events.LlmEvent.LLM_CHAT_COMPLETION_SUMMARY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestUtil {
    public static void assertLlmChatCompletionMessageAttributes(Event event, String modelId, String content, boolean isResponse) {
        assertEquals(LLM_CHAT_COMPLETION_MESSAGE, event.getType());

        Map<String, Object> attributes = event.getAttributes();
        assertEquals(Vendor.INGEST_SOURCE, attributes.get("ingest_source"));
        assertFalse(((String) attributes.get("completion_id")).isEmpty());
        assertFalse(((String) attributes.get("id")).isEmpty());
        assertFalse(((String) attributes.get("request_id")).isEmpty());
        assertEquals(Vendor.VENDOR, attributes.get("vendor"));
        assertEquals(modelId, attributes.get("response.model"));
        assertEquals("testPrefix", attributes.get("llm.testPrefix"));
        assertEquals("conversation-id-value", attributes.get("llm.conversation_id"));
        assertEquals(13, attributes.get("token_count"));

        assertEquals(content, attributes.get("content"));
        if (isResponse) {
            assertEquals("assistant", attributes.get("role"));
            assertEquals(true, attributes.get("is_response"));
            assertEquals(1, attributes.get("sequence"));
        } else {
            assertEquals("user", attributes.get("role"));
            assertEquals(false, attributes.get("is_response"));
            assertEquals(0, attributes.get("sequence"));
        }
    }

    public static void assertLlmChatCompletionSummaryAttributes(Event event, String requestModelId, String responseModelId, String finishReason,
            Double expectedTemp) {
        assertEquals(LLM_CHAT_COMPLETION_SUMMARY, event.getType());

        Map<String, Object> attributes = event.getAttributes();
        assertEquals(Vendor.INGEST_SOURCE, attributes.get("ingest_source"));
        assertEquals(expectedTemp.floatValue(), attributes.get("request.temperature"));
        assertTrue(((Float) attributes.get("duration")) >= 0);
        assertEquals(finishReason, attributes.get("response.choices.finish_reason"));
        assertEquals(requestModelId, attributes.get("request.model"));
        assertEquals(Vendor.VENDOR, attributes.get("vendor"));
        assertEquals(responseModelId, attributes.get("response.model"));
        assertFalse(((String) attributes.get("id")).isEmpty());
        assertFalse(((String) attributes.get("request_id")).isEmpty());
        assertEquals(2, attributes.get("response.number_of_messages"));
        assertEquals(1000, attributes.get("request.max_tokens"));
        assertEquals("testPrefix", attributes.get("llm.testPrefix"));
        assertEquals("conversation-id-value", attributes.get("llm.conversation_id"));
    }
}
