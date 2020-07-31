/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.service.analytics;

import com.newrelic.agent.TransactionData;
import com.newrelic.agent.tracing.DistributedTracePayloadImpl;
import com.newrelic.agent.tracing.DistributedTraceService;

import java.util.HashMap;
import java.util.Map;

public class TransactionDataToDistributedTraceIntrinsics {
    private final DistributedTraceService distributedTraceService;

    public TransactionDataToDistributedTraceIntrinsics(DistributedTraceService distributedTraceService) {
        this.distributedTraceService = distributedTraceService;
    }

    public Map<String, Object> buildDistributedTracingIntrinsics(TransactionData transactionData, boolean addTransactionParentingAttributes) {
        DistributedTracePayloadImpl inboundDistributedTracePayload = transactionData.getInboundDistributedTracePayload();

        String parentId = null;

        // only set parentId if a payload is received with tx guid
        if (inboundDistributedTracePayload != null && inboundDistributedTracePayload.getTransactionId() != null) {
            parentId = transactionData.getParentId();
        }

        String parentSpanId = null;

        // only set parentSpanId if a payload is received with id guid
        if ((inboundDistributedTracePayload != null && inboundDistributedTracePayload.getGuid() != null) || transactionData.getW3CTraceParent() != null) {
            parentSpanId = transactionData.getParentSpanId();
        }

        Map<String, Object> intrinsics = distributedTraceService.getIntrinsics(
                inboundDistributedTracePayload,
                transactionData.getGuid(),
                transactionData.getTripId(),
                transactionData.getTransportType(),
                transactionData.getTransportDurationInMillis(),
                transactionData.getLargestTransportDurationInMillis(),
                parentId,
                parentSpanId,
                transactionData.getPriority());

        if (!addTransactionParentingAttributes) {
            return intrinsics;
        }

        intrinsics = new HashMap<>(intrinsics);
        putIntrinsicIfNotNull(intrinsics, "parentId", parentId);
        putIntrinsicIfNotNull(intrinsics, "parentSpanId", parentSpanId);
        return intrinsics;
    }

    private void putIntrinsicIfNotNull(Map<String, Object> intrinsics, String key, String value) {
        if (value != null) {
            intrinsics.put(key, value);
        }
    }
}
