/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracing;

import com.newrelic.agent.Transaction;
import org.junit.Test;

import java.util.Arrays;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class W3CTraceParentHeaderTest extends BaseDistributedTraceTest {

    @Test
    public void testCreateTraceParentPayload() {
        Transaction transaction = Transaction.getTransaction(true);
        SpanProxy spanProxy = transaction.getSpanProxy();
        W3CTracePayload payload = W3CTracePayload.parseHeaders(transaction,
                singletonList("02-12341234123412341234123412341234-4321432143214321-01"),
                singletonList("190@nr=0-App-709288-8599547-f85f42fd82a4cf1d-164d3b4b0d09cb05164d3b4b0d09cb05-1-0.789-1563574856827"));
        assertNotNull(payload);
        spanProxy.setInitiatingW3CTraceParent(payload.getTraceParent());
        spanProxy.setInitiatingW3CTraceState(payload.getTraceState());

        String traceParentHeader = W3CTraceParentHeader.create(spanProxy, spanProxy.getInitiatingW3CTraceParent().getTraceId(), spanProxy.getInitiatingW3CTraceParent().getParentId(), spanProxy.getInitiatingW3CTraceParent().sampled());
        assertEquals("00-12341234123412341234123412341234-4321432143214321-01", traceParentHeader);
    }

    @Test
    public void testCreateTraceParentPayloadPadsTraceId() {
        Transaction transaction = Transaction.getTransaction(true);
        SpanProxy spanProxy = transaction.getSpanProxy();

        String traceId = "1234123412341234";
        String parentId = "4321432143214321";
        boolean sampled = false;

        String traceParentHeader = W3CTraceParentHeader.create(spanProxy, traceId, parentId, sampled);
        String resultingParentTraceId = traceParentHeader.split("-")[1];

        assertEquals(16, traceId.length());
        assertEquals(32, resultingParentTraceId.length());
        assertTrue(resultingParentTraceId.endsWith(traceId));
    }

    @Test
    public void testCreateTraceParentPayloadPadsAndLowerCasesTraceId() {
        Transaction transaction = Transaction.getTransaction(true);
        SpanProxy spanProxy = transaction.getSpanProxy();

        String traceId = "ABCDEFABCDEFABCD";
        String parentId = "4321432143214321";
        boolean sampled = false;

        String traceParentHeader = W3CTraceParentHeader.create(spanProxy, traceId, parentId, sampled);
        String resultingParentTraceId = traceParentHeader.split("-")[1];

        assertEquals(16, traceId.length());
        assertEquals("0000000000000000abcdefabcdefabcd", resultingParentTraceId);
    }

    @Test
    public void testParseMultiple_differentValues_shouldFail() {
        W3CTraceParent result = W3CTraceParentParser.parseHeaders(
                Arrays.asList("00-12345678123456781234567812345678-1234123412341234-01", "00-87654321876543218765432187654321-1234123412341234-01"));
        assertNull(result);
    }

    @Test
    public void testParseMultiple_sameValues_shouldPass() {
        W3CTraceParent result = W3CTraceParentParser.parseHeaders(
                Arrays.asList("00-12345678123456781234567812345678-1234123412341234-01", "00-12345678123456781234567812345678-1234123412341234-01"));
        W3CTraceParent expected = new W3CTraceParent("00", "12345678123456781234567812345678", "1234123412341234", 1);
        assertEquals(expected, result);
    }

    @Test
    public void testParseMultiple_butReallyJustOne() {
        W3CTraceParent result = W3CTraceParentParser.parseHeaders(
                singletonList("00-12345678123456781234567812345678-1234123412341234-01"));
        W3CTraceParent expected = new W3CTraceParent("00", "12345678123456781234567812345678", "1234123412341234", 1);
        assertEquals(expected, result);
    }

    @Test
    public void testParseCorrectHeader() {
        W3CTraceParent expected = new W3CTraceParent("00", "12345678123456781234567812345678", "1234123412341234", 1);
        W3CTraceParent result = W3CTraceParentParser.parseHeader("00-12345678123456781234567812345678-1234123412341234-01");
        assertEquals(expected, result);
    }

    @Test
    public void testParse64BitTraceId() {
        W3CTraceParent result = W3CTraceParentParser.parseHeader("00-f2f4f6f8f2f4f6f8-1234123412341234-01");
        assertNull(result);
    }

    @Test
    public void testParseBadVersion() {
        W3CTraceParent header = W3CTraceParentParser.parseHeader("zz-12345678123456781234567812345678-1234123412341234-01");
        assertNull(header);
    }

    @Test
    public void testParseLargerVersion() {
        W3CTraceParent expected = new W3CTraceParent("03", "12345678123456781234567812345678", "1234123412341234", 1);
        W3CTraceParent result = W3CTraceParentParser.parseHeader("03-12345678123456781234567812345678-1234123412341234-01");
        assertEquals(expected, result);
    }

    @Test
    public void testParseLargerVersionFailure() {
        W3CTraceParent header = W3CTraceParentParser.parseHeader("03-123456781234567812345647812345678-23453-123423412341234-01");
        assertNull(header);
    }

    @Test
    public void testBadTraceId() {
        W3CTraceParent header = W3CTraceParentParser.parseHeader("00-1234567812J456781234564812345678-123423412341234-01");
        assertNull(header);
    }

    @Test
    public void testBadParentId() {
        W3CTraceParent header = W3CTraceParentParser.parseHeader("03-12345678123456781234564781245678-12342BAD3441234-01");
        assertNull(header);
    }
}
