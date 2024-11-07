/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracing;

import com.newrelic.agent.MetricNames;
import com.newrelic.agent.Transaction;
import com.newrelic.api.agent.DistributedTracePayload;
import com.newrelic.api.agent.NewRelic;

import java.util.List;
import java.util.logging.Level;

public class W3CTracePayload {

    private final DistributedTracePayload payload;
    private final W3CTraceParent traceParent;
    private final W3CTraceState traceState;

    private W3CTracePayload(W3CTraceParent parentHeader, W3CTraceState stateHeader, DistributedTracePayload payload) {
        this.payload = payload;
        this.traceParent = parentHeader;
        this.traceState = stateHeader;
    }

    public DistributedTracePayload getPayload() {
        return payload;
    }

    public W3CTraceParent getTraceParent() {
        return traceParent;
    }

    public W3CTraceState getTraceState() {
        return traceState;
    }

    public static W3CTracePayload parseHeaders(Transaction tx, List<String> parentHeaders, List<String> stateHeaders) {
        try {
            if (parentHeaders == null) {
                tx.getMetricAggregator().incrementCounter(MetricNames.SUPPORTABILITY_TRACE_CONTEXT_NULL_PARENT);
                return null;
            }
            W3CTraceParent w3CTraceParent = W3CTraceParentParser.parseHeaders(parentHeaders);
            if (w3CTraceParent == null) {
                tx.getMetricAggregator().incrementCounter(MetricNames.SUPPORTABILITY_TRACE_CONTEXT_PARENT_PARSE_EXCEPTION);
                return null;
            }

            W3CTraceState w3CTraceState = W3CTraceStateSupport.parseHeaders(stateHeaders);
            if (w3CTraceState == null && stateHeaders != null && !stateHeaders.isEmpty()) {
                tx.getMetricAggregator().incrementCounter(MetricNames.SUPPORTABILITY_TRACE_CONTEXT_STATE_PARSE_EXCEPTION);
            }

            tx.getMetricAggregator().incrementCounter(MetricNames.SUPPORTABILITY_TRACE_CONTEXT_ACCEPT_SUCCESS);

            if (w3CTraceState == null || !w3CTraceState.containsNrData()) {
                // return with empty DT payload
                NewRelic.getAgent().getLogger().log(Level.INFO, "NR-335227 debug: traceState was null. Parent traceId:" + w3CTraceParent.getTraceId() + " parentId:" + w3CTraceParent.getParentId());
                return new W3CTracePayload(w3CTraceParent, w3CTraceState, null);
            }

            DistributedTracePayload dtPayload =  new DistributedTracePayloadImpl(w3CTraceState.getTimestamp(), w3CTraceState.getParentType().name(),
                    w3CTraceState.getAccountId(), w3CTraceState.getTrustKey(), w3CTraceState.getApplicationId(),
                    w3CTraceParent.getParentId(), w3CTraceParent.getTraceId(), w3CTraceState.getTxnId(), w3CTraceState.getPriority(),
                    w3CTraceState.getSampled());
            NewRelic.getAgent().getLogger().log(Level.INFO, "NR-335227 debug: distributed trace payload=" + dtPayload);
            return new W3CTracePayload(w3CTraceParent, w3CTraceState, dtPayload);
        } catch (Exception e) {
            tx.getMetricAggregator().incrementCounter(MetricNames.SUPPORTABILITY_TRACE_CONTEXT_ACCEPT_EXCEPTION);
            return null;
        }
    }
}
