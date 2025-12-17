/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.service.analytics;

import com.newrelic.agent.model.AttributeFilter;
import com.newrelic.agent.model.LinkOnSpan;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.newrelic.agent.service.analytics.SpanEventFactory.DEFAULT_SYSTEM_TIMESTAMP_SUPPLIER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class LinkOnSpanFactoryTest {
    LinkOnSpanFactory linkOnSpanFactory = new LinkOnSpanFactory("AppName", new AttributeFilter.PassEverythingAttributeFilter(),
            DEFAULT_SYSTEM_TIMESTAMP_SUPPLIER);

    @Test
    public void typeShouldBeSet() {
        LinkOnSpan target = linkOnSpanFactory.build();
        assertEquals("SpanLink", target.getType());
        assertTrue(target.getIntrinsics().containsKey("type"));
    }

    @Test
    public void appNameShouldBeSet() {
        LinkOnSpan target = linkOnSpanFactory.build();
        assertEquals("AppName", target.getAppName());
    }

    @Test
    public void timestampShouldBeSet() {
        LinkOnSpan target = linkOnSpanFactory.build();
        assertTrue(target.getTimestamp() > 0);
    }

    @Test
    public void idShouldBeSet() {
        LinkOnSpan target = linkOnSpanFactory.setId("Id").build();
        assertEquals("Id", target.getId());
        assertTrue(target.getIntrinsics().containsKey("id"));
    }

    @Test
    public void traceIdShouldBeSet() {
        LinkOnSpan target = linkOnSpanFactory.setTraceId("TraceId").build();
        assertEquals("TraceId", target.getTraceId());
        assertTrue(target.getIntrinsics().containsKey("trace.id"));
    }

    @Test
    public void linkedSpanIdShouldBeSet() {
        LinkOnSpan target = linkOnSpanFactory.setLinkedSpanId("LinkedSpanId").build();
        assertEquals("LinkedSpanId", target.getLinkedSpanId());
        assertTrue(target.getIntrinsics().containsKey("linkedSpanId"));
    }

    @Test
    public void linkedTraceIdShouldBeSet() {
        LinkOnSpan target = linkOnSpanFactory.setLinkedTraceId("LinkedTraceId").build();
        assertEquals("LinkedTraceId", target.getLinkedTraceId());
        assertTrue(target.getIntrinsics().containsKey("linkedTraceId"));
    }

    @Test
    public void userAttributesShouldBeSet() {
        HashMap<String, Object> userAtts = new HashMap<String, Object>() {
        };
        userAtts.put("a", "b");
        userAtts.put("c", "d");
        LinkOnSpan target = linkOnSpanFactory.putAllUserAttributes(userAtts).build();
        assertEquals(userAtts, target.getUserAttributesCopy());
    }

    @Test
    public void agentAttributesShouldBeEmpty() {
        LinkOnSpan target = linkOnSpanFactory.build();
        assertTrue(target.getAgentAttributes().isEmpty());
    }

    @Test
    public void writeJsonStringHasCorrectOutput() throws IOException {
        LinkOnSpan target = linkOnSpanFactory
                .setId("id")
                .setTraceId("traceId")
                .setLinkedSpanId("linkedSpanId")
                .setLinkedTraceId("linkedTraceId")
                .putAllUserAttributes(Collections.singletonMap("foo", "bar"))
                .build();

        Writer stringWriter = new StringWriter();
        target.writeJSONString(stringWriter);

        assertEquals(
                "[{\"trace.id\":\"traceId\",\"linkedSpanId\":\"linkedSpanId\",\"id\":\"id\",\"type\":\"SpanLink\",\"linkedTraceId\":\"linkedTraceId\"},{\"foo\":\"bar\"},{}]",
                stringWriter.toString());
    }

    @Test
    public void builderShouldNotSetNullValues() throws IOException {
        LinkOnSpan target = linkOnSpanFactory
                .setId(null)
                .setTraceId(null)
                .setLinkedSpanId(null)
                .setLinkedTraceId(null)
                .putAllUserAttributes(null)
                .build();

        Writer stringWriter = new StringWriter();
        target.writeJSONString(stringWriter);

        assertEquals(
                "[{\"type\":\"SpanLink\"},{},{}]",
                stringWriter.toString());
    }

    @Test
    public void shouldFilterUserAttributes() {
        LinkOnSpanFactory target = new LinkOnSpanFactory("blerb", new AttributeFilter.PassEverythingAttributeFilter() {
            @Override
            public Map<String, ?> filterUserAttributes(String appName, Map<String, ?> userAttributes) {
                return Collections.<String, Object>singletonMap("filtered", "yes");
            }
        }, DEFAULT_SYSTEM_TIMESTAMP_SUPPLIER);

        LinkOnSpan linkOnSpan = target.setUserAttributes(Collections.<String, Object>singletonMap("original", "sad")).build();

        assertEquals("yes", linkOnSpan.getUserAttributesCopy().get("filtered"));
        assertNull(linkOnSpan.getUserAttributesCopy().get("original"));
    }
}
