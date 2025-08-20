/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.nr.agent.instrumentation.utils.span;

import io.opentelemetry.api.trace.SpanKind;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class AttributeMapperTest {
    @Test
    public void getInstance_withValidJson_createsValidMapper() {
        AttributeMapper attributeMapper = AttributeMapper.getInstance();

        // 5 SpanKind types
        Map<SpanKind, Map<AttributeType, List<AttributeKey>>> mappings = attributeMapper.getMappings();
        assertEquals(5, mappings.size());

        for (SpanKind spanKind : SpanKind.values()) {
            Map<AttributeType, List<AttributeKey>> attributeTypesBySpan = mappings.get(spanKind);
            assertEquals(2, attributeTypesBySpan.size());

            // The actual attribute key mappings...
            for (AttributeType attributeType : AttributeType.values()) {
                assertEquals(2, attributeTypesBySpan.get(attributeType).size());
            }
        }
    }

    @Test
    public void findProperOtelKey_returnsProperKey() {
        AttributeMapper attributeMapper = AttributeMapper.getInstance();
        Set<String> otelKeys = new HashSet<>();
        otelKeys.add("key1");
        otelKeys.add("key2");
        otelKeys.add("key3");
        otelKeys.add("server.port");    //Should find this in the mapper

        assertEquals("server.port", attributeMapper.findProperOtelKey(SpanKind.SERVER, AttributeType.Port, otelKeys));
    }

    @Test
    public void findProperOtelKey_returnsEmptyString_whenRequestedKeyNotFound() {
        AttributeMapper attributeMapper = AttributeMapper.getInstance();
        Set<String> otelKeys = new HashSet<>();
        otelKeys.add("key1");
        otelKeys.add("key2");
        otelKeys.add("key3");

        assertEquals("", attributeMapper.findProperOtelKey(SpanKind.SERVER, AttributeType.Port, otelKeys));
    }
}
