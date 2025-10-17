/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.newrelic.agent.attributes.AttributesService;
import com.newrelic.api.agent.Logs;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.TransportType;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.TransactionTracerConfig;
import com.newrelic.agent.dispatchers.Dispatcher;
import com.newrelic.agent.model.ApdexPerfZone;
import com.newrelic.agent.model.TimeoutCause;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.sql.SlowQueryListener;
import com.newrelic.agent.stats.ApdexPerfZoneDetermination;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracing.DistributedTracePayloadImpl;
import com.newrelic.agent.tracing.SpanProxy;
import com.newrelic.agent.tracing.W3CTraceParent;
import com.newrelic.agent.transaction.PriorityTransactionName;
import com.newrelic.agent.transaction.TransactionThrowable;
import com.newrelic.agent.transaction.TransactionTimer;
import com.newrelic.api.agent.Insights;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

public class TransactionData {
    private final Transaction tx;
    private final int transactionSize;

    public TransactionData(Transaction transaction, int transactionSize) {
        this.tx = transaction;
        this.transactionSize = transactionSize;
    }

    public Transaction getTransaction() {
        return tx;
    }

    public Insights getInsightsData() {
        return tx.getInsightsData();
    }

    public Logs getLogEventData() {
        return tx.getLogEventData();
    }

    public Dispatcher getDispatcher() {
        return tx.getDispatcher();
    }

    public TransactionTimer getTransactionTime() {
        return tx.getTransactionTimer();
    }

    public Tracer getRootTracer() {
        return tx.getRootTracer();
    }

    public Collection<Tracer> getTracers() {
        return tx.getTracers();
    }

    public Set<TransactionActivity> getTransactionActivities() {
        return tx.getFinishedChildren();
    }

    /**
     * Get the start time of the transaction (wall clock time).
     */
    public long getWallClockStartTimeMs() {
        return tx.getWallClockStartTimeMs();
    }

    /**
     * Get the start time of the transaction in nanoseconds.
     */
    public long getStartTimeInNanos() {
        return tx.getTransactionTimer().getStartTimeInNanos();
    }

    /**
     * Get the end time of the transaction in nanoseconds.
     */
    public long getEndTimeInNanos() {
        return tx.getTransactionTimer().getEndTimeInNanos();
    }

    /**
     * @return filtered request URI.
     */
    public String getRequestUri(String destination) {
        AttributesService attributesService = ServiceFactory.getAttributesService();
        return attributesService.filterRequestUri(getApplicationName(), destination, getDispatcher().getUri());
    }

    public int getResponseStatus() {
        return tx.getStatus();
    }

    public String getStatusMessage() {
        return tx.getStatusMessage();
    }

    public String getApplicationName() {
        return tx.getRPMService().getApplicationName();
    }

    public AgentConfig getAgentConfig() {
        return tx.getAgentConfig();
    }

    public TransactionTracerConfig getTransactionTracerConfig() {
        return getAgentConfig() == null ? null : getAgentConfig().getTransactionTracerConfig();
    }

    public Map<String, Object> getInternalParameters() {
        return tx.getInternalParameters();
    }

    public Map<String, Map<String, String>> getPrefixedAttributes() {
        return tx.getPrefixedAgentAttributes();
    }

    public Map<String, Object> getUserAttributes() {
        return tx.getUserAttributes();
    }

    public Map<String, Object> getAgentAttributes() {
        return tx.getAgentAttributes();
    }

    public Map<String, ?> getErrorAttributes() {
        return tx.getErrorAttributes();
    }

    public Map<String, Object> getIntrinsicAttributes() {
        return tx.getIntrinsicAttributes();
    }

    public PriorityTransactionName getPriorityTransactionName() {
        return tx.getPriorityTransactionName();
    }

    public String getBlameMetricName() {
        return getPriorityTransactionName().getName();
    }

    public String getBlameOrRootMetricName() {
        return getBlameMetricName() == null ? getRootTracer().getMetricName() : getBlameMetricName();
    }

    public TransactionThrowable getThrowable() {
        return tx.getThrowable();
    }

    public boolean hasReportableErrorThatIsNotIgnored() {
        return tx.isErrorReportableAndNotIgnored();
    }

    public boolean hasErrorThatIsNotExpected() {
        return tx.isErrorNotExpected();
    }

    /**
     * A rough approximation of the transaction size (how much memory we are using with our tracers)
     */
    protected final int getTransactionSize() {
        return transactionSize;
    }

    /**
     * Get the duration of the transaction in milliseconds.
     */
    public long getDurationInMillis() {
        return tx.getTransactionTimer().getResponseTimeInMilliseconds();
    }

    /**
     * Returns the transaction duration in nanoseconds.
     */
    public long getLegacyDuration() {
        return tx.getTransactionTimer().getResponseTimeInNanos();
    }

    /**
     * Returns the wall clock time of the transaction. This is when all work in the transaction finished.
     */
    public long getTransactionDuration() {
        return tx.getTransactionTimer().getTransactionDurationInNanos();
    }

    public long getTimeToFirstByteDurationNs() {
        return tx.getTransactionTimer().getTimeToFirstByteInNanos();
    }

    public long getTimeToLastByteDurationNs() {
        return tx.getTransactionTimer().getTimetoLastByteInNanos();
    }

    public String getGuid() {
        return tx.getGuid();
    }

    public String getReferrerGuid() {
        if (!receivedInboundDistributedPayload()) {
            return tx.getInboundHeaderState().getReferrerGuid();
        }

        return getInboundDistributedTracePayload().guid;
    }

    public String getTripId() {
        String traceId = tx.getSpanProxy().getTraceId();
        if (traceId != null) {
            return traceId;
        }

        return tx.getCrossProcessTransactionState().getTripId();
    }

    public int generatePathHash() {
        return tx.getCrossProcessTransactionState().generatePathHash();
    }

    public Integer getReferringPathHash() {
        return tx.getInboundHeaderState().getReferringPathHash();
    }

    public String getAlternatePathHashes() {
        return tx.getCrossProcessTransactionState().getAlternatePathHashes();
    }

    public String getSyntheticsResourceId() {
        return tx.getInboundHeaderState().getSyntheticsResourceId();
    }

    public String getSyntheticsJobId() {
        return tx.getInboundHeaderState().getSyntheticsJobId();
    }

    public String getSyntheticsMonitorId() {
        return tx.getInboundHeaderState().getSyntheticsMonitorId();
    }

    public String getSyntheticsType() { return tx.getInboundHeaderState().getSyntheticsType(); }

    public String getSyntheticsInitiator() { return tx.getInboundHeaderState().getSyntheticsInitiator(); }

    public Map<String, String> getSyntheticsAttributes() { return tx.getInboundHeaderState().getSyntheticsAttrs(); }

    public ApdexPerfZone getApdexPerfZone() {
        if (!isWebTransaction() && !tx.getAgentConfig().isApdexTSet(getPriorityTransactionName().getName())) {
            return null;
        }

        if (isApdexFrustrating()) {
            return ApdexPerfZone.FRUSTRATING;
        }

        long responseTimeInMillis = tx.getTransactionTimer().getResponseTimeInMilliseconds() + tx.getExternalTime();
        long apdexTInMillis = tx.getAgentConfig().getApdexTInMillis(getPriorityTransactionName().getName());
        return ApdexPerfZoneDetermination.getZone(responseTimeInMillis, apdexTInMillis);
    }

    public boolean isApdexFrustrating() {
        return hasReportableErrorThatIsNotIgnored() && hasErrorThatIsNotExpected();
    }

    public String parentType() {
        if (!receivedInboundDistributedPayload()) {
            return null;
        }
        return getInboundDistributedTracePayload().parentType;
    }

    public String getParentApp() {
        if (!receivedInboundDistributedPayload()) {
            return null;
        }
        return getInboundDistributedTracePayload().applicationId;
    }

    public String getParentAccountId() {
        if (!receivedInboundDistributedPayload()) {
            return null;
        }
        return getInboundDistributedTracePayload().accountId;
    }

    public long getTransportDurationInMillis() {
        return tx.getTransportDurationInMillis();
    }

    public String getParentId() {
        if (!receivedInboundDistributedPayload()) {
            return null;
        }

        return getInboundDistributedTracePayload().txnId;
    }

    public String getParentSpanId() {
        if (!receivedInboundDistributedPayload()) {
            W3CTraceParent w3CTraceParent = getW3CTraceParent();
            if(w3CTraceParent != null) {
                return w3CTraceParent.getParentId();
            }

            return null;
        }

        // This will be the id of the span that caused this transaction to come into existence
        return getInboundDistributedTracePayload().guid;
    }

    public float getPriority() {
        return tx.getPriority();
    }

    public W3CTraceParent getW3CTraceParent() {
        return tx.getSpanProxy().getInitiatingW3CTraceParent();
    }

    public DistributedTracePayloadImpl getInboundDistributedTracePayload() {
        return tx.getSpanProxy().getInboundDistributedTracePayload();
    }

    private boolean receivedInboundDistributedPayload() {
        return getInboundDistributedTracePayload() != null;
    }

    /**
     * Returns true if this is a web transaction.
     *
     * @return true if this is a web transaction
     */
    public boolean isWebTransaction() {
        return getDispatcher().isWebTransaction();
    }

    /**
     * Returns true if this transaction was originated by New Relic Synthetics.
     *
     * @return true if this transaction was originated by New Relic Synthetics
     */
    public boolean isSyntheticTransaction() {
        return tx.isSynthetic();
    }

    /**
     * Returns the SlowQueryListener if it was used during the transaction, otherwise null.
     *
     * @return SlowQueryListener if used during the transaction, null otherwise
     */
    public SlowQueryListener getSlowQueryListener() {
        return tx.getSlowQueryListener(false);
    }

    @Override
    public String toString() {
        String name = getRequestUri(AgentConfigImpl.ATTRIBUTES) == null ? getRootTracer().getMetricName() : getRequestUri(AgentConfigImpl.ATTRIBUTES);
        StringBuilder builder = new StringBuilder(name == null ? "" : name).append(' ').append(getDurationInMillis()).append("ms");
        if (getThrowable() != null) {
            builder.append(' ').append(getThrowable().throwable.toString());
        }
        return builder.toString();
    }

    public TimeoutCause getTimeoutCause() {
        return tx.getTimeoutCause();
    }

    public TransportType getTransportType() {
        return tx.getTransportType();
    }

    public long getLargestTransportDurationInMillis() {
        return tx.getLargestTransportDurationInMillis();
    }

    public boolean sampled() {
        return tx.sampled();
    }

    public SpanProxy getSpanProxy() {
        return tx.getSpanProxy();
    }

    public String getTraceId() {
        return getSpanProxy().getOrCreateTraceId();
    }

    public Transaction.PartialSampleType getPartialSampleType() { return tx.getPartialSampleType(); }
}
