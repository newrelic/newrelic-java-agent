/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.model;

import junit.framework.TestCase;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class EventOnSpanTest {

    @Test
    public void testEquals() {
        long now = System.currentTimeMillis();

        EventOnSpan eventOnSpan1 = baseBuilder(now).build();
        EventOnSpan eventOnSpan2 = baseBuilder(now).build();
        assertEquals(eventOnSpan1, eventOnSpan1);
        assertNotEquals(null, eventOnSpan1);
        assertEquals(eventOnSpan2, eventOnSpan1);
        assertEquals(eventOnSpan1, eventOnSpan2);

        EventOnSpan eventOnSpan3 = baseBuilderExtraUser(now, "zizzy", "baluba").build();
        assertNotEquals(eventOnSpan1, eventOnSpan3);
        assertNotEquals(eventOnSpan2, eventOnSpan3);

        EventOnSpan eventOnSpan4 = baseBuilderExtraAgent(now, "clown", "town").build();
        assertNotEquals(eventOnSpan1, eventOnSpan4);
        assertNotEquals(eventOnSpan2, eventOnSpan4);

        EventOnSpan eventOnSpan5 = baseBuilderExtraIntrinsic(now, "munge", "factor").build();
        assertNotEquals(eventOnSpan1, eventOnSpan5);
        assertNotEquals(eventOnSpan2, eventOnSpan5);

        EventOnSpan eventOnSpan7 = baseBuilder(now).appName("somethingDifferent").build();
        assertNotEquals(eventOnSpan1, eventOnSpan7);
        assertNotEquals(eventOnSpan2, eventOnSpan7);

        EventOnSpan eventOnSpan8 = baseBuilder(now).priority(88.1210897f).build();
        assertNotEquals(eventOnSpan1, eventOnSpan8);
        assertNotEquals(eventOnSpan2, eventOnSpan8);

        EventOnSpan eventOnSpan9 = baseBuilder(now + 42).build();
        assertNotEquals(eventOnSpan1, eventOnSpan9);
        assertNotEquals(eventOnSpan2, eventOnSpan8);
    }

    @Test
    public void testBuilderMethods() {
        long now = System.currentTimeMillis();

        EventOnSpan.Builder builder = baseBuilder(now);
        EventOnSpan eventOnSpan1 = builder.build();
        Map<String, String> emptyAttributes = new HashMap<>();
        EventOnSpan eventOnSpan2 = builder.putAllUserAttributesIfAbsent(emptyAttributes).build();
        assertEquals(eventOnSpan1, eventOnSpan2);

        Map<String, String> newUserAttributes = new HashMap<>();
        newUserAttributes.put("a", "z");
        newUserAttributes.put("c", "d");
        EventOnSpan eventOnSpan3 = builder.putAllUserAttributesIfAbsent(newUserAttributes).build();
        assertEquals("b", eventOnSpan3.getUserAttributesCopy().get("a"));
        assertEquals("d", eventOnSpan3.getUserAttributesCopy().get("c"));

        EventOnSpan eventOnSpan4 = builder.putAgentAttribute("baz", "thud").build();
        assertEquals("thud", eventOnSpan4.getAgentAttributes().get("baz"));

        EventOnSpan eventOnSpan5 = builder.putIntrinsic("tiger", "mouse").build();
        assertEquals("mouse", eventOnSpan5.getIntrinsics().get("tiger"));
    }

    @Test
    public void writeJSONString_EventOnSpan_shouldFormatCorrectly() throws IOException {
        long now = System.currentTimeMillis();
        EventOnSpan eventOnSpan = baseBuilder(now).build();

        Writer stringWriter = new StringWriter();
        eventOnSpan.writeJSONString(stringWriter);

        assertEquals(
                "[{\"name\":\"name\",\"trace.id\":\"traceId\",\"span.id\":\"spanId\"},{\"a\":\"b\"},{\"foo\":\"bar\"}]",
                stringWriter.toString());
    }

    @Test
    public void eventOnSpan_getsAll_Attributes() {
        EventOnSpan.Builder builder = baseBuilder(System.currentTimeMillis());

        Map<String, Object> moreIntrinsics = new HashMap<>();
        moreIntrinsics.put("someBoolean", true);
        moreIntrinsics.put("someString", "abcdefg");
        moreIntrinsics.put("someNumber", 22.0f);
        builder.putAllIntrinsics(moreIntrinsics);

        Map<String, Object> moreAgentAttributes = new HashMap<>();
        moreAgentAttributes.put("userBoolean", false);
        moreAgentAttributes.put("userString", "qwerty");
        moreAgentAttributes.put("userNumber", 7.0f);
        builder.putAllAgentAttributes(moreAgentAttributes);

        EventOnSpan eventOnSpan = builder.build();
        assertEquals("spanId", eventOnSpan.getSpanId());
        assertEquals("traceId", eventOnSpan.getTraceId());
        assertEquals("name", eventOnSpan.getName());
        assertEquals(true, eventOnSpan.getIntrinsics().get("someBoolean"));
        assertEquals("abcdefg", eventOnSpan.getIntrinsics().get("someString"));
        assertEquals(22.0f, eventOnSpan.getIntrinsics().get("someNumber"));
        assertEquals("bar", eventOnSpan.getAgentAttributes().get("foo"));
        assertEquals(false, eventOnSpan.getAgentAttributes().get("userBoolean"));
        assertEquals("qwerty", eventOnSpan.getAgentAttributes().get("userString"));
        assertEquals(7.0f, eventOnSpan.getAgentAttributes().get("userNumber"));
    }

    private EventOnSpan.Builder baseBuilderExtraUser(long now, String extraUserAttr, String value) {
        return baseBuilder(now).putAllUserAttributes(singletonMap(extraUserAttr, value));
    }

    private EventOnSpan.Builder baseBuilderExtraAgent(long now, String extraAgentAttr, String value) {
        return baseBuilder(now).putAllAgentAttributes(singletonMap(extraAgentAttr, value));
    }

    private EventOnSpan.Builder baseBuilderExtraIntrinsic(long now, String extraIntrinsic, String value) {
        return baseBuilder(now).putAllAgentAttributes(singletonMap(extraIntrinsic, value));
    }

    private EventOnSpan.Builder baseBuilder(long now) {
        Map<String, String> userAttributes = new HashMap<>();
        userAttributes.put("a", "b");

        Map<String, String> agentAttributes = new HashMap<>();
        agentAttributes.put("foo", "bar");

        Map<String, String> intrinsics = new HashMap<>();
        intrinsics.put("span.id", "spanId");
        intrinsics.put("trace.id", "traceId");
        intrinsics.put("name", "name");

        return EventOnSpan.builder()
                .putAllUserAttributes(userAttributes)
                .putAllAgentAttributes(agentAttributes)
                .putAllIntrinsics(intrinsics)
                .appName("my_app")
                .priority(0)
                .timestamp(now);
    }

}