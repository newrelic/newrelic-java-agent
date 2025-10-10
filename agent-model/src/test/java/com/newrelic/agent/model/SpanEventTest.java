/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
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
import static org.junit.Assert.assertNotNull;

public class SpanEventTest {

    @Test
    public void shouldNotBuildWithEmptyAttributeMaps() {
        SpanEvent target = SpanEvent.builder().build();
        assertNotNull(target.getMutableUserAttributes());
        assertNotNull(target.getIntrinsics());
        assertNotNull(target.getAgentAttributes());
    }

    @Test
    public void testEquals() {
        long now = System.currentTimeMillis();

        SpanEvent span1 = baseBuilder(now).build();
        SpanEvent span2 = baseBuilder(now).build();
        assertEquals(span1, span1);
        assertNotEquals(span1, null);
        assertEquals(span2, span1);
        assertEquals(span1, span2);

        SpanEvent span3 = baseBuilderExtraUser(now, "zizzy", "baluba").build();
        assertNotEquals(span1, span3);
        assertNotEquals(span2, span3);

        SpanEvent span4 = baseBuilderExtraAgent(now, "clown", "town").build();
        assertNotEquals(span1, span4);
        assertNotEquals(span2, span4);

        SpanEvent span5 = baseBuilderExtraIntrinsic(now, "munge", "factor").build();
        assertNotEquals(span1, span5);
        assertNotEquals(span2, span5);

        SpanEvent span7 = baseBuilder(now).appName("somethingDifferent").build();
        assertNotEquals(span1, span7);
        assertNotEquals(span2, span7);

        SpanEvent span8 = baseBuilder(now).priority(88.1210897f).build();
        assertNotEquals(span1, span8);
        assertNotEquals(span2, span8);

        SpanEvent span9 = baseBuilder(now + 42).build();
        assertNotEquals(span1, span9);
        assertNotEquals(span2, span8);
    }

    @Test
    public void testBuilderMethods(){
        long now = System.currentTimeMillis();

        SpanEvent.Builder builder = baseBuilder(now);
        assertEquals(builder.getSpanKindFromUserAttributes(), "client");

        SpanEvent span1 = builder.build();
        Map<String, String> emptyAttributes = new HashMap<>();
        SpanEvent span2 = builder.putAllUserAttributesIfAbsent(emptyAttributes).build();
        assertEquals(span1, span2);

        Map<String, String> newUserAttributes = new HashMap<>();
        newUserAttributes.put("a", "z");
        newUserAttributes.put("c", "d");
        SpanEvent span3 = builder.putAllUserAttributesIfAbsent(newUserAttributes).build();
        assertEquals("b", span3.getUserAttributesCopy().get("a"));
        assertEquals("d", span3.getUserAttributesCopy().get("c"));

        SpanEvent span4 = builder.putAgentAttribute("baz", "thud").build();
        assertEquals("thud", span4.getAgentAttributes().get("baz"));

        SpanEvent span5 = builder.putIntrinsic("tiger", "mouse").build();
        assertEquals("mouse", span5.getIntrinsics().get("tiger"));
    }

    @Test
    public void writeJSONString_SpanEvent_shouldFormatCorrectly() throws IOException {
        long now = System.currentTimeMillis();
        SpanEvent spanEvent = baseBuilder(now).build();

        Writer stringWriter = new StringWriter();
        spanEvent.writeJSONString(stringWriter);

        assertEquals("[{\"cat\":\"dog\"},{\"a\":\"b\"},{\"foo\":\"bar\"}]", stringWriter.toString());
    }

    @Test
    public void spanEvent_getsAll_Attributes() {
        SpanEvent.Builder builder = baseBuilder(System.currentTimeMillis());

        Map<String, Object> moreIntrinsics = new HashMap<>();
        moreIntrinsics.put("guid", "123456");
        moreIntrinsics.put("traceId", "abcdefg");
        moreIntrinsics.put("duration", 22.0f);
        moreIntrinsics.put("parentId", "buzzbuzz");
        moreIntrinsics.put("name", "thud");
        moreIntrinsics.put("transactionId", "8675zzz");
        builder.putAllIntrinsics(moreIntrinsics);

        SpanEvent spanEvent = builder.build();
        assertEquals(22.0f, spanEvent.getDuration(), 0);
        assertEquals("123456", spanEvent.getGuid());
        assertEquals("abcdefg", spanEvent.getTraceId());
        assertEquals("buzzbuzz", spanEvent.getParentId());
        assertEquals("thud", spanEvent.getName());
        assertEquals("8675zzz", spanEvent.getTransactionId());
        assertEquals("wally", spanEvent.getAppName());
    }

    private SpanEvent.Builder baseBuilderExtraUser(long now, String extraUserAttr, String value) {
        return baseBuilder(now).putAllUserAttributes(singletonMap(extraUserAttr, value));
    }

    private SpanEvent.Builder baseBuilderExtraAgent(long now, String extraAgentAttr, String value) {
        return baseBuilder(now).putAllAgentAttributes(singletonMap(extraAgentAttr, value));
    }

    private SpanEvent.Builder baseBuilderExtraIntrinsic(long now, String extraIntrinsic, String value) {
        return baseBuilder(now).putAllAgentAttributes(singletonMap(extraIntrinsic, value));
    }

    private SpanEvent.Builder baseBuilder(long now) {
        Map<String, String> userAttributes = new HashMap<>();
        userAttributes.put("a", "b");
        Map<String, String> agentAttributes = new HashMap<>();
        agentAttributes.put("foo", "bar");
        Map<String, String> intrinsics = new HashMap<>();
        intrinsics.put("cat", "dog");

        return SpanEvent.builder()
                .putAllUserAttributes(userAttributes)
                .putAllAgentAttributes(agentAttributes)
                .putAllIntrinsics(intrinsics)
                .appName("wally")
                .priority(21.7f)
                .timestamp(now);
    }
}