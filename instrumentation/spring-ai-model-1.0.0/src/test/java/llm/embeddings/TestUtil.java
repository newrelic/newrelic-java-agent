/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package llm.embeddings;
import com.newrelic.agent.introspec.Event;
import llm.embeddings.vendor.Vendor;

import java.util.Map;

import static llm.embeddings.events.LlmEvent.LLM_EMBEDDING;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.springframework.ai.embedding.EmbeddingUtil.totalTokens;

public class TestUtil {
    public static void assertLlmEmbeddingAttributes(Event event, String modelId, String requestInput) {
        assertEquals(LLM_EMBEDDING, event.getType());

        Map<String, Object> attributes = event.getAttributes();
        assertFalse(((String) attributes.get("id")).isEmpty());
        assertFalse(((String) attributes.get("request_id")).isEmpty());
        assertEquals(requestInput, attributes.get("input"));
        assertEquals(modelId, attributes.get("request.model"));
        assertEquals(modelId, attributes.get("response.model"));
        assertEquals(totalTokens, attributes.get("response.usage.total_tokens"));
        assertEquals(Vendor.VENDOR, attributes.get("vendor"));
        assertEquals(Vendor.INGEST_SOURCE, attributes.get("ingest_source"));
        assertTrue(((Float) attributes.get("duration")) >= 0);
        assertEquals("testPrefix", attributes.get("llm.testPrefix"));
        assertEquals("conversation-id-value", attributes.get("llm.conversation_id"));
    }
}
