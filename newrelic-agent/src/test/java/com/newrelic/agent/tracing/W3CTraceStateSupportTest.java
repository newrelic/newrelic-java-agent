/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracing;

import com.google.common.base.Strings;
import com.newrelic.agent.MockDispatcherTracer;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.trace.TransactionGuidFactory;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class W3CTraceStateSupportTest extends BaseDistributedTraceTest {

    @Test
    public void testDuplicateVendorKeys() throws Exception {
        createDistributedTraceService("accountId", "190", "appID", 0, 2);
        List<String> headers = Collections.singletonList("foo=1,foo=1");
        W3CTraceState expected = new W3CTraceState(headers, Collections.<String>emptyList());

        W3CTraceState result = W3CTraceStateSupport.parseHeaders(headers);
        assertEquals(expected, result);
    }

    @Test
    public void testParseHeaders() {
        createDistributedTraceService("accountId", "190", "appID", 0, 2);
        List<String> headers = new ArrayList<>();
        headers.add("190@nr=0-0-709288-jimmy8599547-f85f42fd82a4cf1d-164d3b4b0d09cb05164d3b4b0d09cb05-1-0.789-1563574856827");
        W3CTraceState payload = W3CTraceStateSupport.parseHeaders(headers);

        assertEquals("190", payload.getTrustKey());
        assertEquals(ParentType.App, payload.getParentType());
        assertEquals("709288", payload.getAccountId());
        assertEquals("jimmy8599547", payload.getApplicationId());
        assertEquals("f85f42fd82a4cf1d", payload.getGuid());
        assertEquals("164d3b4b0d09cb05164d3b4b0d09cb05", payload.getTxnId());
        assertEquals(payload.getSampled(), Sampled.SAMPLED_YES);
        assertEquals(0.789f, payload.getPriority(), 0.0001);
        assertEquals(1563574856827L, payload.getTimestamp());
        assertTrue(payload.getVendorStates().isEmpty());
    }

    @Test
    public void testParseHeadersMultiple() {
        createDistributedTraceService("accountId", "190", "appID", 0, 2);
        List<String> headers = new ArrayList<>();
        headers.add("190@nr=0-0-709288-8599547-f85f42fd82a4cf1d-164d3b4b0d09cb05164d3b4b0d09cb05-1-0.789-1563574856827");
        headers.add("bad@congo=qzx932");
        W3CTraceState payload = W3CTraceStateSupport.parseHeaders(headers);

        assertEquals(1, payload.getVendorStates().size());
    }

    @Test
    public void testParseHeadersTooMany() {
        createDistributedTraceService("accountId", "190", "appID", 0, 2);
        List<String> headers = new ArrayList<>();
        headers.add("190@nr=0-0-709288-8599547-f85f42fd82a4cf1d-164d3b4b0d09cb05164d3b4b0d09cb05-1-0.789-1563574856827");
        for (int i = 0; i < 50; i++) {
            headers.add("ba" + i + "d@congo=qzx932");
        }

        W3CTraceState result = W3CTraceStateSupport.parseHeaders(headers);
        assertEquals(51, result.getTraceStateHeaders().size());    //all are kept!
    }

    @Test
    public void testParseHeadersNullSampled() {
        createDistributedTraceService("accountId", "190", "appID", 0, 2);
        List<String> headers = new ArrayList<>();
        headers.add("190@nr=0-0-709288-8599547-f85f42fd82a4cf1d-164d3b4b0d09cb05164d3b4b0d09cb05--0.789-1563574856827");

        W3CTraceState payload = W3CTraceStateSupport.parseHeaders(headers);
        assertEquals(Sampled.UNKNOWN, payload.getSampled());
    }

    @Test
    public void testParseHeadersInvalidSampled() {
        createDistributedTraceService("accountId", "190", "appID", 0, 2);
        List<String> headers = new ArrayList<>();
        headers.add("190@nr=0-0-709288-8599547-f85f42fd82a4cf1d-164d3b4b0d09cb05164d3b4b0d09cb05-grr-0.789-1563574856827");

        W3CTraceState payload = W3CTraceStateSupport.parseHeaders(headers);
        assertEquals(Sampled.UNKNOWN, payload.getSampled());
    }

    @Test
    public void testParseHeadersNullPriority() {
        createDistributedTraceService("accountId", "190", "appID", 0, 2);
        List<String> headers = new ArrayList<>();
        headers.add("190@nr=0-0-709288-8599547-f85f42fd82a4cf1d-164d3b4b0d09cb05164d3b4b0d09cb05-1--1563574856827");

        W3CTraceState payload = W3CTraceStateSupport.parseHeaders(headers);
        assertNull(payload.getPriority());
    }

    @Test
    public void testParseHeadersNoPriorityOrSampledTriggersResamplingDecision() {
        createDistributedTraceService("accountId", "190", "appID", 0, 2);
        Transaction transaction = Transaction.getTransaction(true);
        transaction.startTransactionIfBeginning(new MockDispatcherTracer(transaction));

        DistributedTracePayloadImpl inboundPayload = (DistributedTracePayloadImpl) W3CTracePayload.parseHeaders(transaction,
                Collections.singletonList("02-12341234123412341234123412341234-4321432143214321-01"),
                Arrays.asList("190@nr=0-0-709288-8599547-f85f42fd82a4cf1d-164d3b4b0d09cb05164d3b4b0d09cb05---1563574856827")).getPayload();
        assertNotNull(inboundPayload);
        transaction.acceptDistributedTracePayload(inboundPayload, null);

        transaction.createDistributedTracePayload("meatball!");
        String[] outboundPayload = new W3CTraceStateHeader(true, true).create(transaction.getSpanProxy()).split("-");
        assertFalse(outboundPayload[6].isEmpty()); // sampled
        assertFalse(outboundPayload[7].isEmpty()); // priority
    }

    @Test
    public void testParseHeadersNoLeadingZeroPriority() {
        createDistributedTraceService("accountId", "190", "appID", 0, 2);
        List<String> headers = new ArrayList<>();
        headers.add("190@nr=0-0-709288-8599547-f85f42fd82a4cf1d-164d3b4b0d09cb05164d3b4b0d09cb05-1-.123-1563574856827");

        W3CTraceState payload = W3CTraceStateSupport.parseHeaders(headers);
        assertEquals(0.123, payload.getPriority(), 0.0001);
    }

    @Test
    public void testParseHeadersInvalidPriority() {
        createDistributedTraceService("accountId", "190", "appID", 0, 2);
        List<String> headers = new ArrayList<>();
        headers.add("190@nr=0-0-709288-8599547-f85f42fd82a4cf1d-164d3b4b0d09cb05164d3b4b0d09cb05-1-grr-1563574856827");

        W3CTraceState payload = W3CTraceStateSupport.parseHeaders(headers);
        assertNull(payload.getPriority());
    }

    @Test
    public void testParseHeadersDup() {
        createDistributedTraceService("accountId", "190", "appID", 0, 2);
        Transaction transaction = Transaction.getTransaction(true);
        transaction.startTransactionIfBeginning(new MockDispatcherTracer(transaction));

        DistributedTracePayloadImpl inboundPayload = (DistributedTracePayloadImpl) W3CTracePayload.parseHeaders(transaction,
                Collections.singletonList("02-12341234123412341234123412341234-4321432143214321-01"),
                Arrays.asList("190@nr=0-0-709288-8599547-f85f42fd82a4cf1d-164d3b4b0d09cb05164d3b4b0d09cb05-1-0.789-1563574856827", "congo@=0-qzx932-abc123",
                        "congo@=0-very-qzx932-abc123")).getPayload();
        assertNotNull(inboundPayload);
        transaction.acceptDistributedTracePayload(inboundPayload, null);

        String newSpanId = TransactionGuidFactory.generate16CharGuid();
        transaction.createDistributedTracePayload(newSpanId);
        String outboundPayload = new W3CTraceStateHeader(true, true).create(transaction.getSpanProxy());
        //timestamp is generated
        assertTrue(outboundPayload.startsWith("190@nr=0-0-accountId-appID-" + newSpanId + "-" + transaction.getGuid() + "-0-0.789-"));
    }

    @Test
    public void testParseHeadersNoNR() {
        createDistributedTraceService("accountId", "190", "appID", 0, 2);
        List<String> headers = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            headers.add("ba" + i + "d@congo=qzx932");
        }

        W3CTraceState payload = W3CTraceStateSupport.parseHeaders(headers);
        assertEquals(20, payload.getVendorStates().size());
        assertNull(payload.getAccountId());
        assertNull(payload.getApplicationId());
        assertNull(payload.getGuid());
        assertEquals(ParentType.Invalid, payload.getParentType());
        assertNull(payload.getTrustKey());
        assertNull(payload.getTxnId());
        assertEquals(0, payload.getTimestamp());
        assertNull(payload.getPriority());
    }

    @Test
    public void testTruncateWith32() throws Exception {
        List<String> states = new ArrayList<>(32);
        for (int i = 0; i < 32; i++) {
            states.add("bar:" + i + "=" + i);
        }
        List<String> result = W3CTraceStateSupport.truncateVendorStates(states);
        assertEquals(31, result.size());
    }

    @Test
    public void testTruncateFewer32() throws Exception {
        List<String> states = new ArrayList<>(32);
        for (int i = 0; i < 13; i++) {
            states.add("jeb:" + i + "=" + i);
        }
        List<String> result = W3CTraceStateSupport.truncateVendorStates(states);
        assertSame(states, result);
    }

    @Test
    public void testInterleavedWithLongOnes() throws Exception {
        List<String> states = new ArrayList<>(32);
        for (int i = 0; i < 16; i++) {
            String state = "jeb:" + i + "=" + i;
            String longState = "jeb:" + i + "=" + Strings.repeat("1", 128);
            states.add(longState);
            states.add(state);
        }
        List<String> result = W3CTraceStateSupport.truncateVendorStates(states);
        List<String> expected = states.subList(0, 30);
        expected.add(states.get(31));
        assertEquals(expected, result);
    }

    @Test
    public void testInterleavedOnlyShortOnesRemain() throws Exception {
        List<String> states = new ArrayList<>(32);
        List<String> expected = new ArrayList<>(32);
        for (int i = 0; i < 31; i++) {
            String state = "jeb:" + i + "=" + i;
            String longState = "jeb:" + i + "=" + Strings.repeat("1", 128);
            states.add(state);
            states.add(longState);
            expected.add(state);
        }
        List<String> result = W3CTraceStateSupport.truncateVendorStates(states);
        assertEquals(expected, result);
    }
}
