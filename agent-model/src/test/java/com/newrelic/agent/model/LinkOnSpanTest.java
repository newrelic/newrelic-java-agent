/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.model;

import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class LinkOnSpanTest {

    @Test
    public void testEquals() {
        long now = System.currentTimeMillis();

        LinkOnSpan linkOnSpan1 = baseBuilder(now).build();
        LinkOnSpan linkOnSpan2 = baseBuilder(now).build();
        assertEquals(linkOnSpan1, linkOnSpan1);
        assertNotEquals(null, linkOnSpan1);
        assertEquals(linkOnSpan2, linkOnSpan1);
        assertEquals(linkOnSpan1, linkOnSpan2);

        LinkOnSpan linkOnSpan3 = baseBuilderExtraUser(now, "zizzy", "baluba").build();
        assertNotEquals(linkOnSpan1, linkOnSpan3);
        assertNotEquals(linkOnSpan2, linkOnSpan3);

        LinkOnSpan linkOnSpan4 = baseBuilderExtraAgent(now, "clown", "town").build();
        assertNotEquals(linkOnSpan1, linkOnSpan4);
        assertNotEquals(linkOnSpan2, linkOnSpan4);

        LinkOnSpan linkOnSpan5 = baseBuilderExtraIntrinsic(now, "munge", "factor").build();
        assertNotEquals(linkOnSpan1, linkOnSpan5);
        assertNotEquals(linkOnSpan2, linkOnSpan5);

        LinkOnSpan linkOnSpan7 = baseBuilder(now).appName("somethingDifferent").build();
        assertNotEquals(linkOnSpan1, linkOnSpan7);
        assertNotEquals(linkOnSpan2, linkOnSpan7);

        LinkOnSpan linkOnSpan8 = baseBuilder(now).priority(88.1210897f).build();
        assertNotEquals(linkOnSpan1, linkOnSpan8);
        assertNotEquals(linkOnSpan2, linkOnSpan8);

        LinkOnSpan linkOnSpan9 = baseBuilder(now + 42).build();
        assertNotEquals(linkOnSpan1, linkOnSpan9);
        assertNotEquals(linkOnSpan2, linkOnSpan8);
    }

    @Test
    public void testBuilderMethods() {
        long now = System.currentTimeMillis();

        LinkOnSpan.Builder builder = baseBuilder(now);
        LinkOnSpan linkOnSpan1 = builder.build();
        Map<String, String> emptyAttributes = new HashMap<>();
        LinkOnSpan linkOnSpan2 = builder.putAllUserAttributesIfAbsent(emptyAttributes).build();
        assertEquals(linkOnSpan1, linkOnSpan2);

        Map<String, String> newUserAttributes = new HashMap<>();
        newUserAttributes.put("a", "z");
        newUserAttributes.put("c", "d");
        LinkOnSpan linkOnSpan3 = builder.putAllUserAttributesIfAbsent(newUserAttributes).build();
        assertEquals("b", linkOnSpan3.getUserAttributesCopy().get("a"));
        assertEquals("d", linkOnSpan3.getUserAttributesCopy().get("c"));

        LinkOnSpan linkOnSpan4 = builder.putAgentAttribute("baz", "thud").build();
        assertEquals("thud", linkOnSpan4.getAgentAttributes().get("baz"));

        LinkOnSpan linkOnSpan5 = builder.putIntrinsic("tiger", "mouse").build();
        assertEquals("mouse", linkOnSpan5.getIntrinsics().get("tiger"));
    }

    @Test
    public void writeJSONString_LinkOnSpan_shouldFormatCorrectly() throws IOException {
        long now = System.currentTimeMillis();
        LinkOnSpan linkOnSpan = baseBuilder(now).build();

        Writer stringWriter = new StringWriter();
        linkOnSpan.writeJSONString(stringWriter);

        assertEquals(
                "[{\"trace.id\":\"traceId\",\"id\":\"id\",\"linkedSpanId\":\"linkedSpanId\",\"linkedTraceId\":\"linkedTraceId\"},{\"a\":\"b\"},{\"foo\":\"bar\"}]",
                stringWriter.toString());
    }

    @Test
    public void linkOnSpan_getsAll_Attributes() {
        LinkOnSpan.Builder builder = baseBuilder(System.currentTimeMillis());

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

        LinkOnSpan linkOnSpan = builder.build();
        assertEquals("id", linkOnSpan.getId());
        assertEquals("traceId", linkOnSpan.getTraceId());
        assertEquals("linkedSpanId", linkOnSpan.getLinkedSpanId());
        assertEquals("linkedTraceId", linkOnSpan.getLinkedTraceId());
        assertEquals(true, linkOnSpan.getIntrinsics().get("someBoolean"));
        assertEquals("abcdefg", linkOnSpan.getIntrinsics().get("someString"));
        assertEquals(22.0f, linkOnSpan.getIntrinsics().get("someNumber"));
        assertEquals("bar", linkOnSpan.getAgentAttributes().get("foo"));
        assertEquals(false, linkOnSpan.getAgentAttributes().get("userBoolean"));
        assertEquals("qwerty", linkOnSpan.getAgentAttributes().get("userString"));
        assertEquals(7.0f, linkOnSpan.getAgentAttributes().get("userNumber"));
    }

    private LinkOnSpan.Builder baseBuilderExtraUser(long now, String extraUserAttr, String value) {
        return baseBuilder(now).putAllUserAttributes(singletonMap(extraUserAttr, value));
    }

    private LinkOnSpan.Builder baseBuilderExtraAgent(long now, String extraAgentAttr, String value) {
        return baseBuilder(now).putAllAgentAttributes(singletonMap(extraAgentAttr, value));
    }

    private LinkOnSpan.Builder baseBuilderExtraIntrinsic(long now, String extraIntrinsic, String value) {
        return baseBuilder(now).putAllAgentAttributes(singletonMap(extraIntrinsic, value));
    }

    private LinkOnSpan.Builder baseBuilder(long now) {
        Map<String, String> userAttributes = new HashMap<>();
        userAttributes.put("a", "b");

        Map<String, String> agentAttributes = new HashMap<>();
        agentAttributes.put("foo", "bar");

        Map<String, String> intrinsics = new HashMap<>();
        intrinsics.put("id", "id");
        intrinsics.put("trace.id", "traceId");
        intrinsics.put("linkedSpanId", "linkedSpanId");
        intrinsics.put("linkedTraceId", "linkedTraceId");

        return LinkOnSpan.builder()
                .putAllUserAttributes(userAttributes)
                .putAllAgentAttributes(agentAttributes)
                .putAllIntrinsics(intrinsics)
                .appName("my_app")
                .priority(0)
                .timestamp(now);
    }

}