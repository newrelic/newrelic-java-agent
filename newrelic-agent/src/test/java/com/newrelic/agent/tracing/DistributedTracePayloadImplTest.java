/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracing;

import com.newrelic.agent.Transaction;
import org.junit.Test;

import static org.junit.Assert.*;

public class DistributedTracePayloadImplTest extends BaseDistributedTraceTest {

    @Test
    public void testCreatePayloadTxn() {
        createDistributedTraceService("accountId", "trustKey", "appID", 0, 2);

        Transaction transaction = Transaction.getTransaction(true);
        DistributedTracePayloadImpl payload = DistributedTracePayloadImpl.createDistributedTracePayload(transaction.getGuid(), transaction.getGuid(),
                transaction.getGuid(), 1.0f);
        assertNotNull(payload);
        assertEquals("App", payload.parentType);
        assertEquals(transaction.getGuid(), payload.traceId);
        assertEquals(transaction.getGuid(), payload.txnId);
        // May be true only if created via API transaction.createDistributedTracePayload()
        assertEquals(1.0f, payload.priority, 0.0001);
    }
    @Test
    public void testMissingAccountId() {
        createDistributedTraceService(null, "67890", "67890", 0, 2);

        Transaction transaction = Transaction.getTransaction(true);
        DistributedTracePayloadImpl payload = DistributedTracePayloadImpl.createDistributedTracePayload("12345678", "12345678", transaction.getGuid(), 1.0f);
        assertNull(payload); // payload should be null due to missing accountId but no exceptions thrown
    }

    @Test
    public void testMissingApplicationId() {
        createDistributedTraceService("12345", "67890", null, 0, 2);

        Transaction transaction = Transaction.getTransaction(true);
        DistributedTracePayloadImpl payload = DistributedTracePayloadImpl.createDistributedTracePayload(transaction.getGuid(), transaction.getGuid(),
                transaction.getGuid(), 0.5f);
        assertNotNull(payload); // payload should be created but should be missing "accountId" field
        assertEquals("12345", payload.accountId);
        assertNull(payload.applicationId);
        assertEquals("App", payload.parentType);
        assertEquals(transaction.getGuid(), payload.traceId);
        // May be true only if created via API transaction.createDistributedTracePayload()
        assertEquals(0.5f, payload.priority,0.0001);
    }

}
