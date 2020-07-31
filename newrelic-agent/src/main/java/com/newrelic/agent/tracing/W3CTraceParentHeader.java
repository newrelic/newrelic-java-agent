/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracing;

import com.google.common.base.Strings;
import com.newrelic.agent.trace.TransactionGuidFactory;

import java.util.Locale;

public class W3CTraceParentHeader {

    static final String W3C_VERSION = "00";
    static final String W3C_TRACE_PARENT_DELIMITER = "-";

    public static String create(SpanProxy proxy, String traceId, String guid, boolean sampled) {
        W3CTraceParent existingW3cTraceParent = proxy.getInitiatingW3CTraceParent();
        if (existingW3cTraceParent == null) {
            // if existingW3cTraceParent is null, that means we are the root of the distributed trace, otherwise we are the middle of the distributed trace
            return createNewHeader(proxy, traceId, guid, sampled);
        }
        return forwardHeader(existingW3cTraceParent, guid, sampled);
    }

    //WARNING: SIDE EFFECT :: Mutates proxy parameter!!
    private static String createNewHeader(SpanProxy proxy, String traceId, String guid, boolean sampled) {
        String parentId = guid == null ? TransactionGuidFactory.generate16CharGuid() : guid;
        proxy.setInitiatingW3CTraceParent(new W3CTraceParent(W3C_VERSION, traceId, null, (sampled ? 1 : 0)));
        String w3cTraceId = maybePad(maybeLower(traceId));
        return createHeader(sampled, parentId, w3cTraceId);
    }

    private static String createHeader(boolean sampled, String parentId, String w3cTraceId) {
        return W3C_VERSION + W3C_TRACE_PARENT_DELIMITER + w3cTraceId + W3C_TRACE_PARENT_DELIMITER + parentId + W3C_TRACE_PARENT_DELIMITER
                + sampledToFlags(sampled);
    }

    private static String maybeLower(String traceId) {
        return traceId.toLowerCase(Locale.ROOT);
    }

    private static String maybePad(String txnId) {
        return Strings.padStart(txnId, 32, '0');
    }

    private static String forwardHeader(W3CTraceParent existingW3cTraceParent, String guid, boolean sampled) {
        String parentId = guid == null ? TransactionGuidFactory.generate16CharGuid() : guid;
        return createHeader(sampled, parentId, existingW3cTraceParent.getTraceId());
    }

    private static String sampledToFlags(boolean sampled) {
        return sampled ? "01" : "00";
    }
}
