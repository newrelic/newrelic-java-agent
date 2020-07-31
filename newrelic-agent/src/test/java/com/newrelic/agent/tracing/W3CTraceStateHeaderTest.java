/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracing;

import com.newrelic.agent.Transaction;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class W3CTraceStateHeaderTest extends BaseDistributedTraceTest {

    @Test
    public void testCreateTraceStateHeader() {
        createDistributedTraceService("accountId", "trustKey", "appID", 0, 2);
        Transaction transaction = Transaction.getTransaction(true);
        W3CTracePayload.parseHeaders(transaction,
                Collections.singletonList("02-12341234123412341234123412341234-4321432143214321-01"),
                Collections.singletonList("190@nr=0-0-709288-8599547-f85f42fd82a4cf1d-164d3b4b0d09cb05164d3b4b0d09cb05-1-0.789-1563574856827"));

        String traceStateHeader = new W3CTraceStateHeader(true, true).createTraceStateHeader(
                new DistributedTracePayloadImpl(1234L, "parentType", "accountId", "trustKey", "appId", "guid", "traceId",
                        "txnId", 0.789f, Sampled.SAMPLED_NO), "0");
        assertEquals("trustKey@nr=0-0-accountId-appId-guid-txnId-0-0.789-1234", traceStateHeader);
    }

    @Test
    public void testSpanIdOmittedWhenSpansAreDisabled() {
        createDistributedTraceService("accountId", "trustKey", "appID", 0, 2);
        W3CTraceStateHeader testClass = new W3CTraceStateHeader(false, true);
        String traceStateHeader = testClass.createTraceStateHeader(
                new DistributedTracePayloadImpl(1234L, "parentType", "accountId", "trustKey", "appId",
                        "I_SHOULD_NOT_BE_OUTPUT", "traceId", "txnId", 0.789f, Sampled.SAMPLED_NO), "0");
        assertEquals("trustKey@nr=0-0-accountId-appId--txnId-0-0.789-1234", traceStateHeader);
    }

    @Test
    public void testTransactionIdOmittedWhenSpansAreDisabled() {
        createDistributedTraceService("accountId", "trustKey", "appID", 0, 2);
        W3CTraceStateHeader testClass = new W3CTraceStateHeader(true, false);
        String traceStateHeader = testClass.createTraceStateHeader(
                new DistributedTracePayloadImpl(1234L, "parentType", "accountId", "trustKey", "appId",
                        "667", "traceId", "NO_SOUP", 0.789f, Sampled.SAMPLED_NO ), "0");
        assertEquals("trustKey@nr=0-0-accountId-appId-667--0-0.789-1234", traceStateHeader);
    }

    @Test
    public void testBothSpanAndTxDisabled() {
        createDistributedTraceService("accountId", "trustKey", "appID", 0, 2);
        W3CTraceStateHeader testClass = new W3CTraceStateHeader(false, false);
        String traceStateHeader = testClass.createTraceStateHeader(
                new DistributedTracePayloadImpl(1234L, "parentType", "accountId", "trustKey", "appId",
                        "ZOOMY_ZOOM_ZOOM_ZOOM", "traceId", "NO_SOUP", 0.789f, Sampled.SAMPLED_NO), "0");
        assertEquals("trustKey@nr=0-0-accountId-appId---0-0.789-1234", traceStateHeader);
    }

    @Test
    public void testSmallPrecisionPriority() throws Exception {
        W3CTraceStateHeader testClass = new W3CTraceStateHeader(true, true);
        String traceStateHeader = testClass.createTraceStateHeader(
                new DistributedTracePayloadImpl(1234L, "parentType", "accountId", "trustKey", "appId",
                        "broop", "traceId", "txnid", 0.000001f, Sampled.SAMPLED_NO), "0");
        assertEquals("trustKey@nr=0-0-accountId-appId-broop-txnid-0-0.000001-1234", traceStateHeader);
    }

    @Test
    public void testVerySmallPrecisionPriority() throws Exception {
        W3CTraceStateHeader testClass = new W3CTraceStateHeader(true, true);
        String traceStateHeader = testClass.createTraceStateHeader(
                new DistributedTracePayloadImpl(1234L, "parentType", "accountId", "trustKey", "appId",
                        "broop", "traceId", "txnid", 0.0000000000000000001f, Sampled.SAMPLED_NO), "0");
        assertEquals("trustKey@nr=0-0-accountId-appId-broop-txnid-0-0.000000-1234", traceStateHeader);
    }
}