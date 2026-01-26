/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.service.analytics;

import com.newrelic.agent.model.AttributeFilter;
import com.newrelic.agent.model.EventOnSpan;
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

public class EventOnSpanFactoryTest {
    EventOnSpanFactory eventOnSpanFactory = new EventOnSpanFactory("AppName", new AttributeFilter.PassEverythingAttributeFilter(),
            DEFAULT_SYSTEM_TIMESTAMP_SUPPLIER);

    @Test
    public void typeShouldBeSet() {
        EventOnSpan target = eventOnSpanFactory.build();
        assertEquals("SpanEvent", target.getType());
        assertTrue(target.getIntrinsics().containsKey("type"));
    }

    @Test
    public void appNameShouldBeSet() {
        EventOnSpan target = eventOnSpanFactory.build();
        assertEquals("AppName", target.getAppName());
    }

    @Test
    public void timestampShouldBeSet() {
        EventOnSpan target = eventOnSpanFactory.build();
        assertTrue(target.getTimestamp() > 0);
    }

    @Test
    public void idShouldBeSet() {
        EventOnSpan target = eventOnSpanFactory.setSpanId("SpanId").build();
        assertEquals("SpanId", target.getSpanId());
        assertTrue(target.getIntrinsics().containsKey("span.id"));
    }

    @Test
    public void traceIdShouldBeSet() {
        EventOnSpan target = eventOnSpanFactory.setTraceId("TraceId").build();
        assertEquals("TraceId", target.getTraceId());
        assertTrue(target.getIntrinsics().containsKey("trace.id"));
    }

    @Test
    public void nameShouldBeSet() {
        EventOnSpan target = eventOnSpanFactory.setName("Name").build();
        assertEquals("Name", target.getName());
        assertTrue(target.getIntrinsics().containsKey("name"));
    }

    @Test
    public void userAttributesShouldBeSet() {
        HashMap<String, Object> userAtts = new HashMap<String, Object>() {
        };
        userAtts.put("a", "b");
        userAtts.put("c", "d");
        EventOnSpan target = eventOnSpanFactory.putAllUserAttributes(userAtts).build();
        assertEquals(userAtts, target.getUserAttributesCopy());
    }

    @Test
    public void agentAttributesShouldBeEmpty() {
        EventOnSpan target = eventOnSpanFactory.build();
        assertTrue(target.getAgentAttributes().isEmpty());
    }

    @Test
    public void writeJsonStringHasCorrectOutput() throws IOException {
        EventOnSpan target = eventOnSpanFactory
                .setSpanId("spanId")
                .setTraceId("traceId")
                .setName("name")
                .putAllUserAttributes(Collections.singletonMap("foo", "bar"))
                .build();

        Writer stringWriter = new StringWriter();
        target.writeJSONString(stringWriter);

        assertEquals(
                "[{\"trace.id\":\"traceId\",\"name\":\"name\",\"span.id\":\"spanId\",\"type\":\"SpanEvent\"},{\"foo\":\"bar\"},{}]",
                stringWriter.toString());
    }

    @Test
    public void builderShouldNotSetNullValues() throws IOException {
        EventOnSpan target = eventOnSpanFactory
                .setSpanId(null)
                .setTraceId(null)
                .setName(null)
                .putAllUserAttributes(null)
                .build();

        Writer stringWriter = new StringWriter();
        target.writeJSONString(stringWriter);

        assertEquals(
                "[{\"type\":\"SpanEvent\"},{},{}]",
                stringWriter.toString());
    }

    @Test
    public void shouldFilterUserAttributes() {
        EventOnSpanFactory target = new EventOnSpanFactory("blerb", new AttributeFilter.PassEverythingAttributeFilter() {
            @Override
            public Map<String, ?> filterUserAttributes(String appName, Map<String, ?> userAttributes) {
                return Collections.<String, Object>singletonMap("filtered", "yes");
            }
        }, DEFAULT_SYSTEM_TIMESTAMP_SUPPLIER);

        EventOnSpan eventOnSpan = target.setUserAttributes(Collections.<String, Object>singletonMap("original", "sad")).build();

        assertEquals("yes", eventOnSpan.getUserAttributesCopy().get("filtered"));
        assertNull(eventOnSpan.getUserAttributesCopy().get("original"));
    }
}
