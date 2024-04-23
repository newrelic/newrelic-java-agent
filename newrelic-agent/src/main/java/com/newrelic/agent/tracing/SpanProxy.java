/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracing;

import com.newrelic.agent.Agent;
import com.newrelic.agent.MetricNames;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.trace.TransactionGuidFactory;
import com.newrelic.api.agent.DistributedTracePayload;
import com.newrelic.api.agent.NewRelic;

import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

public class SpanProxy {

    private volatile long timestamp;
    private volatile long transportDurationInMillis;

    private final AtomicReference<String> traceId = new AtomicReference<>(null);
    private final AtomicReference<DistributedTracePayloadImpl> inboundPayloadData = new AtomicReference<>(null);
    private final AtomicReference<DistributedTracePayloadImpl> outboundPayloadData = new AtomicReference<>(null);

    private final AtomicReference<W3CTraceParent> initiatingW3CTraceParent = new AtomicReference<>(null);
    private final AtomicReference<W3CTraceState> initiatingW3CTraceState = new AtomicReference<>(null);

    public String getOrCreateTraceId() {
        String id = traceId.get();
        if (id == null) {
            String newGuid = TransactionGuidFactory.ID_GENERATOR.generateTraceId();
            traceId.compareAndSet(null, newGuid);
        }
        return traceId.get();
    }

    public String getTraceId() {
        return traceId.get();
    }


    public DistributedTracePayload createDistributedTracePayload(Float priority, String guid, String txnId) {
        try {
            String traceId = getOrCreateTraceId();
            DistributedTracePayloadImpl payload = DistributedTracePayloadImpl.createDistributedTracePayload(traceId, guid, txnId, priority);
            if (payload == null) {
                return null;
            }

            if (Agent.LOG.isFinestEnabled()) {
                Agent.LOG.log(Level.FINEST, "Created distributed trace payload: {0} for transactionId: {1}", payload, txnId);
            }
            NewRelic.incrementCounter(MetricNames.SUPPORTABILITY_CREATE_PAYLOAD_SUCCESS);

            // This only records the data the first time that we generate "outbound" data
            this.outboundPayloadData.compareAndSet(null, payload);

            return payload;
        } catch (Exception e) {
            Agent.LOG.log(Level.FINEST, e, "Unable to create distributed trace payload");
            NewRelic.incrementCounter(MetricNames.SUPPORTABILITY_CREATE_PAYLOAD_EXCEPTION);
            return null;
        }
    }

    public boolean acceptDistributedTracePayload(String payload) {
        try {
            DistributedTracePayloadParser parser = new DistributedTracePayloadParser(NewRelic.getAgent().getMetricAggregator(),  ServiceFactory.getDistributedTraceService(),
                Agent.LOG);
            DistributedTracePayloadImpl parsedPayload = parser.parse(outboundPayloadData.get(), payload);
            return parsedPayload != null && acceptDistributedTracePayloadImpl(parsedPayload);
        } catch (Exception e) {
            Agent.LOG.log(Level.FINEST, e, "Unable to accept distributed trace payload: {0}", payload);
            NewRelic.incrementCounter(MetricNames.SUPPORTABILITY_ACCEPT_PAYLOAD_EXCEPTION);
        }
        return false;
    }

    public boolean acceptDistributedTracePayload(DistributedTracePayload payload) {
        try {
            // record supportability error if someone called createDistributedTracePayload already
            if (outboundPayloadData.get() != null) {
                Agent.LOG.log(Level.WARNING, "Error: createDistributedTracePayload was called before acceptDistributedTracePayload. Ignoring Call");
                NewRelic.incrementCounter(MetricNames.SUPPORTABILITY_ACCEPT_PAYLOAD_CREATE_BEFORE_ACCEPT);
                return false;
            }

            if (payload instanceof DistributedTracePayloadImpl) {
                return acceptDistributedTracePayloadImpl((DistributedTracePayloadImpl) payload);
            } else {
                Agent.LOG.log(Level.FINEST, "Unable to accept distributed trace payload. Incorrect type: {0}", payload.getClass().getName());
            }
        } catch (Exception e) {
            Agent.LOG.log(Level.FINEST, e, "Unable to accept distributed trace payload: {0}", payload);
            NewRelic.incrementCounter(MetricNames.SUPPORTABILITY_ACCEPT_PAYLOAD_EXCEPTION);
        }

        return false;
    }

    private boolean acceptDistributedTracePayloadImpl(DistributedTracePayloadImpl payload) {
        // First payload "accepted" wins
        if (this.inboundPayloadData.compareAndSet(null, payload)) {
            traceId.set(payload.traceId);

            // We'd like to get the transaction start time in milliseconds since the epoch.
            this.transportDurationInMillis = timestamp - payload.timestamp; // check positive

            NewRelic.incrementCounter(MetricNames.SUPPORTABILITY_ACCEPT_PAYLOAD_SUCCESS);
            return true;
        }
        NewRelic.incrementCounter(MetricNames.SUPPORTABILITY_ACCEPT_PAYLOAD_IGNORED_MULTIPLE_ACCEPT);
        return false;
    }

    public DistributedTracePayloadImpl getInboundDistributedTracePayload() {
        return inboundPayloadData.get();
    }

    public DistributedTracePayloadImpl getOutboundDistributedTracePayload() {
        return outboundPayloadData.get();
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getTransportDurationInMillis() {
        return transportDurationInMillis;
    }

    public W3CTraceParent getInitiatingW3CTraceParent() {
        return initiatingW3CTraceParent.get();
    }

    public W3CTraceState getInitiatingW3CTraceState() {
        return initiatingW3CTraceState.get();
    }

    public void setInitiatingW3CTraceParent(W3CTraceParent w3CTraceParent) {
        if (this.initiatingW3CTraceParent.compareAndSet(null, w3CTraceParent)) {
            // The inbound trace id has higher priority in case the default or DT id had already been set
            traceId.set(w3CTraceParent.getTraceId());
        }
    }

    public void setInitiatingW3CTraceState(W3CTraceState w3CTraceState) {
        this.initiatingW3CTraceState.compareAndSet(null, w3CTraceState);
    }
}
