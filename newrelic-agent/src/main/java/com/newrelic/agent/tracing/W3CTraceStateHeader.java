/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracing;

import com.google.common.base.Joiner;
import com.newrelic.agent.trace.TransactionGuidFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class W3CTraceStateHeader {
    private static final String NR_HEADER_VERSION = "0";
    static final String NR_TRACE_STATE_DELIMITER = "-";
    private static final Joiner NR_TRACE_STATE_JOINER = Joiner.on(NR_TRACE_STATE_DELIMITER);
    private static final String MULTI_TENANT_DELIMITER = "@";
    private static final String TENANT_IDENTIFIER = "[a-z0-9][_0-9a-z\\-*/]{0,240}";
    private static final String MULTI_TENANT_VENDOR_IDENTIFIER = "[a-z][_0-9a-z\\-*/]{0,13}";
    static final String MULTI_TENANT_VENDOR_STATE_KEY = TENANT_IDENTIFIER + MULTI_TENANT_DELIMITER + MULTI_TENANT_VENDOR_IDENTIFIER;
    static final String VENDOR_STATE_KEY_VALUE_DELIMITER = "=";
    static final String NR_VENDOR = MULTI_TENANT_DELIMITER + "nr" + VENDOR_STATE_KEY_VALUE_DELIMITER;

    private final boolean spanEventsEnabled;
    private final boolean transactionEventsEnabled;

    public W3CTraceStateHeader(boolean spanEventsEnabled, boolean transactionEventsEnabled) {
        this.spanEventsEnabled = spanEventsEnabled;
        this.transactionEventsEnabled = transactionEventsEnabled;
    }

    public String create(SpanProxy proxy) {
        DistributedTracePayloadImpl outboundPayload = proxy.getOutboundDistributedTracePayload();
        W3CTraceState traceState = proxy.getInitiatingW3CTraceState();
        if (traceState == null) {
            return createTraceStateHeader(outboundPayload, NR_HEADER_VERSION);
        }
        String traceStateHeader = createTraceStateHeader(outboundPayload);
        return Joiner.on(W3CTraceStateSupport.W3C_TRACE_STATE_VENDOR_DELIMITER).join(
                traceStateHeader,
                W3CTraceStateSupport.concatenateVendorStates(W3CTraceStateSupport.truncateVendorStates(traceState.getVendorStates()))
        );
    }

    String createTraceStateHeader(DistributedTracePayloadImpl payload) {
        return createTraceStateHeader(payload, NR_HEADER_VERSION);
    }

    String createTraceStateHeader(DistributedTracePayloadImpl payload, String version) {
        String spanId = getSpanId(payload);
        String transactionId = getTransactionId(payload);
        String priority = BigDecimal.valueOf(payload.priority)
                .setScale(6, RoundingMode.HALF_UP)
                .toString()
                .replaceFirst("(^.*\\.[1-9]+)0+", "$1");
        return NR_TRACE_STATE_JOINER.join(
                payload.trustKey + NR_VENDOR + version,
                ParentType.App.value,
                payload.accountId,
                payload.applicationId,
                spanId,
                transactionId,
                payload.sampled.booleanValue() ? 1 : 0,
                priority,
                payload.timestamp);
    }

    private String getTransactionId(DistributedTracePayloadImpl payload) {
        if (transactionEventsEnabled) {
            return payload.txnId;
        }
        return "";
    }

    private String getSpanId(DistributedTracePayloadImpl payload) {
        if (spanEventsEnabled) {
            return payload.guid == null ? TransactionGuidFactory.generate16CharGuid() : payload.guid;
        }
        return "";
    }
}
