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

import static org.junit.Assert.*;

public class W3CTracePayloadTest extends BaseDistributedTraceTest {

    @Test
    public void testParsePayload() {
        createDistributedTraceService("accountId", "190", "appID", 0, 2);
        Transaction transaction = Transaction.getTransaction(true);
        W3CTracePayload payload = W3CTracePayload.parseHeaders(transaction,
                Collections.singletonList("02-12341234123412341234123412341234-4321432143214321-01"),
                Collections.singletonList("190@nr=0-0-709288-8599547-f85f42fd82a4cf1d-164d3b4b0d09cb05164d3b4b0d09cb05-1-0.789-1563574856827"));
        assertNotNull(payload);
        DistributedTracePayloadImpl dtPayload = (DistributedTracePayloadImpl) payload.getPayload();
        assertNotNull(dtPayload);

        assertEquals("190", dtPayload.trustKey);
        assertEquals("App", dtPayload.parentType);
        assertEquals("709288", dtPayload.accountId);
        assertEquals("8599547", dtPayload.applicationId);
        assertEquals("4321432143214321", dtPayload.guid);
        assertEquals("164d3b4b0d09cb05164d3b4b0d09cb05", dtPayload.txnId);
        assertEquals(Sampled.SAMPLED_YES, dtPayload.sampled);
        assertEquals(0.789F, dtPayload.priority, 0.0001);
        assertEquals(1563574856827L, dtPayload.timestamp);

        W3CTraceParent parentHeader = payload.getTraceParent();
        assertEquals("02", parentHeader.getVersion());
        assertEquals("12341234123412341234123412341234", parentHeader.getTraceId());
        assertEquals("4321432143214321", parentHeader.getParentId());
        assertEquals(1, parentHeader.getFlags());

        W3CTraceState stateHeader = payload.getTraceState();
        assertEquals(0, stateHeader.getVersion());
        assertEquals(ParentType.App, stateHeader.getParentType());
        assertEquals("709288", stateHeader.getAccountId());
        assertEquals("8599547", stateHeader.getApplicationId());
        assertEquals("164d3b4b0d09cb05164d3b4b0d09cb05", stateHeader.getTxnId());
        assertEquals(0.789f, stateHeader.getPriority(), 0.0001);
        assertEquals(1563574856827L, stateHeader.getTimestamp());
        assertEquals("190", stateHeader.getTrustKey());
    }

    @Test
    public void testParsePayloadNullState() {
        createDistributedTraceService("accountId", "trustKey", "appID", 0, 2);
        Transaction transaction = Transaction.getTransaction(true);
        W3CTracePayload payload = W3CTracePayload.parseHeaders(transaction,
                Collections.singletonList("02-12341234123412341234123412341234-4321432143214321-01"), null);
        assertNotNull(payload);
        DistributedTracePayloadImpl dtPayload = (DistributedTracePayloadImpl) payload.getPayload();
        assertNull(dtPayload);

        W3CTraceParent parentHeader = payload.getTraceParent();
        assertEquals("02", parentHeader.getVersion());
        assertEquals("12341234123412341234123412341234", parentHeader.getTraceId());
        assertEquals("4321432143214321", parentHeader.getParentId());
        assertEquals(1, parentHeader.getFlags());

        assertNull(transaction.getSpanProxy().getInitiatingW3CTraceState());
    }

    @Test
    public void testParseNonLeadingZeroPriority() throws Exception {
        createDistributedTraceService("accountId", "190", "appID", 0, 2);
        Transaction transaction = Transaction.getTransaction(true);
        W3CTracePayload payload = W3CTracePayload.parseHeaders(transaction,
                Collections.singletonList("02-12341234123412341234123412341234-4321432143214321-01"),
                Collections.singletonList("190@nr=0-0-709288-8599547-f85f42fd82a4cf1d-164d3b4b0d09cb05164d3b4b0d09cb05-1-.303-1563574856827"));
        assertNotNull(payload);
        DistributedTracePayloadImpl dtPayload = (DistributedTracePayloadImpl) payload.getPayload();
        assertEquals(0.303f, dtPayload.priority, 0.0001);
    }
}
