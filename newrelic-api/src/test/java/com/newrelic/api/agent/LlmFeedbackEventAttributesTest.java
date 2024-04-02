package com.newrelic.api.agent;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class LlmFeedbackEventAttributesTest {

    LlmFeedbackEventAttributes.Builder llmFeedbackEventBuilder;
    Map<String, Object> llmFeedbackEventAttributes;

    @Before
    public void setup() {
        String traceId = "123456";
        Object rating = 3;
        llmFeedbackEventBuilder = new LlmFeedbackEventAttributes.Builder(traceId, rating);
    }

    @Test
    public void testBuilderWithRequiredParamsOnly() {
        llmFeedbackEventAttributes = llmFeedbackEventBuilder.build();

        assertNotNull(llmFeedbackEventAttributes);
        assertEquals("123456", llmFeedbackEventAttributes.get("trace_id"));
        assertEquals(3, llmFeedbackEventAttributes.get("rating"));
        assertNotNull(llmFeedbackEventAttributes.get("id"));
        assertEquals("Java", llmFeedbackEventAttributes.get("ingest_source"));
        assertFalse(llmFeedbackEventAttributes.containsKey("category"));
        assertFalse(llmFeedbackEventAttributes.containsKey("message"));
        assertFalse(llmFeedbackEventAttributes.containsKey("metadata"));
    }

    @Test
    public void testBuilderWithRequiredAndOptionalParams() {
        llmFeedbackEventAttributes = llmFeedbackEventBuilder
                .category("exampleCategory")
                .message("exampleMessage")
                .metadata(createMetadataMap())
                .build();

        assertNotNull(llmFeedbackEventAttributes);
        assertEquals("123456", llmFeedbackEventAttributes.get("trace_id"));
        assertEquals(3, llmFeedbackEventAttributes.get("rating"));
        assertEquals("exampleCategory", llmFeedbackEventAttributes.get("category"));
        assertEquals("exampleMessage", llmFeedbackEventAttributes.get("message"));
    }

    @Test
    public void testBuilderWithOptionalParamsSetToNull() {
        llmFeedbackEventAttributes = llmFeedbackEventBuilder
                .category(null)
                .message(null)
                .metadata(null)
                .build();

        assertNotNull(llmFeedbackEventAttributes);
        assertEquals("123456", llmFeedbackEventAttributes.get("trace_id"));
        assertEquals(3, llmFeedbackEventAttributes.get("rating"));
        assertNull(llmFeedbackEventAttributes.get("category"));
        assertNull(llmFeedbackEventAttributes.get("message"));
        assertNull(llmFeedbackEventAttributes.get("metadata"));
        assertNotNull(llmFeedbackEventAttributes.get("id"));
        assertEquals("Java", llmFeedbackEventAttributes.get("ingest_source"));
    }

    @Test
    public void testBuilderWithRatingParamAsStringType() {
        String traceId2 = "123456";
        Object rating2 = "3";
        llmFeedbackEventBuilder = new LlmFeedbackEventAttributes.Builder(traceId2, rating2);
        llmFeedbackEventAttributes = llmFeedbackEventBuilder.build();

        assertNotNull(llmFeedbackEventAttributes);
        assertEquals("123456", llmFeedbackEventAttributes.get("trace_id"));
        assertEquals("3", llmFeedbackEventAttributes.get("rating"));
    }

    public Map<String, String> createMetadataMap() {
        Map<String, String> map = new HashMap<>();
        map.put("key1", "val1");
        map.put("key2", "val2");
        return map;
    }
}