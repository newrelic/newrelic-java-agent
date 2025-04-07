/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.newrelic.agent.bridge.CrossProcessState;
import com.newrelic.agent.bridge.NoOpCrossProcessState;
import com.newrelic.agent.bridge.NoOpDistributedTracePayload;
import com.newrelic.agent.bridge.NoOpSegment;
import com.newrelic.agent.bridge.NoOpToken;
import com.newrelic.agent.bridge.NoOpTracedMethod;
import com.newrelic.agent.bridge.NoOpTransaction;
import com.newrelic.agent.bridge.NoOpWebResponse;
import com.newrelic.agent.bridge.Token;
import com.newrelic.agent.bridge.TracedActivity;
import com.newrelic.agent.bridge.WebResponse;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.api.agent.ApplicationNamePriority;
import com.newrelic.api.agent.DistributedTracePayload;
import com.newrelic.api.agent.ExtendedRequest;
import com.newrelic.api.agent.Headers;
import com.newrelic.api.agent.InboundHeaders;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Request;
import com.newrelic.api.agent.Response;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.TracedMethod;
import com.newrelic.api.agent.TransactionNamePriority;
import com.newrelic.api.agent.TransportType;

import java.net.URI;
import java.util.Map;
import java.util.logging.Level;

/**
 * Public API implementation for Transaction class.<br>
 * <br>
 * Implementation note: this object normally does not hold a reference to the Agent's internal Transaction object.
 * Instead, this object re-fetches the inner Transaction from the current thread's local storage on demand. This is
 * important, because otherwise the calling would need to release references to this object after making a call to
 * Transaction.startAsyncActivity() because it changes the transaction on the current thread.<br>
 */
public class TransactionApiImpl implements com.newrelic.agent.bridge.Transaction {

    public static final TransactionApiImpl INSTANCE = new TransactionApiImpl();

    /**
     * Two ApiImpl classes are equal if their wrapped Transactions are the same object. They are also equal if neither
     * currently wraps a transaction.
     *
     * @return true if this object is equal to the argument.
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof TransactionApiImpl)) {
            return false;
        }
        TransactionApiImpl objTxi = (TransactionApiImpl) obj;
        return getTransactionIfExists() == objTxi.getTransactionIfExists();
    }

    /**
     * The hash code of an ApiImpl is equal to the hash code of its wrapped Transaction if it has one, else it is
     * necessarily equal to a constant.
     */
    @Override
    public int hashCode() {
        Transaction tx = getTransactionIfExists();
        return (tx == null) ? 42 : tx.hashCode();
    }

    /**
     * Get the thread's transaction. The return value may be null since we do not force creation of an inner Transaction
     * instance on the current thread.<br>
     *
     * Subclass should override this method. All methods of this class that need to get a hold of the inner Transaction
     * should use this method.
     *
     * @return the real transaction bound to this object, if any; else null.
     * @see com.newrelic.agent.instrumentation.InstrumentationImpl
     */
    protected com.newrelic.agent.Transaction getTransactionIfExists() {
        return com.newrelic.agent.Transaction.getTransaction(false);
    }

    @Override
    public boolean registerAsyncActivity(Object asyncContext) {
        return ServiceFactory.getAsyncTxService().registerAsyncActivity(asyncContext);
    }

    @Override
    public boolean setTransactionName(TransactionNamePriority namePriority, boolean override, String category,
            String... parts) {
        Transaction tx = getTransactionIfExists();
        if (NewRelic.getAgent().getLogger().isLoggable(Level.FINEST)) {
            Agent.LOG.log(Level.FINEST, "newrelic.agent.TransactionApiImpl::setTransactionName (1) - txn: {0}, override: {1}, category: {2}, parts: {3}",
                    (tx != null ? tx.toString() : "N/A"), override, category, String.join("/", parts));
        }
        return (tx != null) ? tx.setTransactionName(namePriority, override, category, parts) : false;
    }

    @Override
    public boolean isTransactionNameSet() {
        Transaction tx = getTransactionIfExists();
        return (tx != null) ? tx.isTransactionNameSet() : false;
    }

    @Override
    public TracedMethod getLastTracer() {
        return getTracedMethod();
    }

    @Override
    public TracedMethod getTracedMethod() {
        Transaction tx = getTransactionIfExists();
        Tracer tracedMethod = getTracedMethodTracer(tx);
        if (tracedMethod == null) {
            return NoOpTracedMethod.INSTANCE;
        }
        return tracedMethod;
    }

    Tracer getTracedMethodTracer(Transaction tx) {
        if (tx == null) {
            return null;
        }
        TransactionActivity txa = tx.getTransactionActivity();
        if (txa == null) {
            return null;
        }
        return txa.getLastTracer();
    }

    @Override
    public void ignore() {
        Transaction tx = getTransactionIfExists();
        if (tx != null) {
            tx.ignore();
        }
    }

    @Override
    public void ignoreApdex() {
        Transaction tx = getTransactionIfExists();
        if (tx != null) {
            tx.ignoreApdex();
        }
    }

    @Override
    public void ignoreErrors() {
        Transaction tx = getTransactionIfExists();
        if (tx != null) {
            tx.ignoreErrors();
        }
    }

    @Override
    public boolean setTransactionName(com.newrelic.agent.bridge.TransactionNamePriority namePriority, boolean override,
            String category, String... parts) {
        Transaction tx = getTransactionIfExists();
        if (NewRelic.getAgent().getLogger().isLoggable(Level.FINEST)) {
            Agent.LOG.log(Level.FINEST, "newrelic.agent.TransactionApiImpl::setTransactionName (2) - txn: {0}, override: {1}, category: {2}, parts: {3}",
                    (tx != null ? tx.toString() : "N/A"), override, category, String.join("/", parts));
        }
        return (tx != null) ? tx.setTransactionName(namePriority, override, category, parts) : false;
    }

    @Override
    public void beforeSendResponseHeaders() {
        addOutboundResponseHeaders();
    }

    @Override
    public void addOutboundResponseHeaders() {
        Transaction tx = getTransactionIfExists();
        if (tx != null) {
            tx.addOutboundResponseHeaders();
        }
    }

    @Override
    public boolean isStarted() {
        Transaction tx = getTransactionIfExists();
        return (tx != null) ? tx.isStarted() : false;
    }

    @Override
    public void setApplicationName(ApplicationNamePriority priority, String appName) {
        Transaction tx = getTransactionIfExists();
        if (tx != null) {
            tx.setApplicationName(priority, appName);
        }
    }

    @Override
    public boolean isAutoAppNamingEnabled() {
        Transaction tx = getTransactionIfExists();
        return (tx != null) ? tx.isAutoAppNamingEnabled() : false;
    }

    @Override
    public boolean isWebRequestSet() {
        Transaction tx = getTransactionIfExists();
        return (tx != null) ? tx.isWebRequestSet() : false;
    }

    @Override
    public boolean isWebResponseSet() {
        Transaction tx = getTransactionIfExists();
        return (tx != null) ? tx.isWebResponseSet() : false;
    }

    @Override
    public void setWebRequest(Request request) {
        Transaction tx = getTransactionIfExists();
        if (tx != null) {
            tx.setWebRequest(request);
        }
    }

    @Override
    public void setWebResponse(Response response) {
        Transaction tx = getTransactionIfExists();
        if (tx != null) {
            tx.setWebResponse(response);
        }
    }

    @Override
    public WebResponse getWebResponse() {
        Transaction tx = getTransactionIfExists();
        return (tx != null) ? tx.getWebResponse() : NoOpWebResponse.INSTANCE;
    }

    @Override
    public void convertToWebTransaction() {
        Transaction tx = getTransactionIfExists();
        if (tx != null) {
            tx.convertToWebTransaction();
        }
    }

    @Override
    public boolean isWebTransaction() {
        Transaction tx = getTransactionIfExists();
        if (tx != null) {
            return tx.isWebTransaction();
        } else {
            return false;
        }
    }

    @Override
    public void requestInitialized(Request request, Response response) {
        Transaction tx = getTransactionIfExists();
        if (tx != null) {
            tx.requestInitialized(request, response);
        }
    }

    @Override
    public void requestDestroyed() {
        Transaction tx = getTransactionIfExists();
        if (tx != null) {
            tx.requestDestroyed();
        }
    }

    @Override
    public void saveMessageParameters(Map<String, String> parameters) {
        Transaction tx = getTransactionIfExists();
        if (tx != null) {
            tx.saveMessageParameters(parameters);
        }
    }

    @Override
    public CrossProcessState getCrossProcessState() {
        Transaction tx = getTransactionIfExists();
        return (tx != null) ? tx.getCrossProcessState() : NoOpCrossProcessState.INSTANCE;
    }

    @Override
    public com.newrelic.agent.bridge.TracedMethod startFlyweightTracer() {
        Transaction tx = getTransactionIfExists();
        if (tx == null || !tx.isStarted()) {
            return NoOpTracedMethod.INSTANCE;
        }
        return tx.getTransactionActivity().startFlyweightTracer();
    }

    @Override
    public void finishFlyweightTracer(com.newrelic.agent.bridge.TracedMethod parent, long startInNanos,
            long finishInNanos, String className, String methodName, String methodDesc,
            String metricName,
            String[] rollupMetricNames) {
        Transaction tx = getTransactionIfExists();
        if (tx != null && tx.isStarted()) {
            tx.getTransactionActivity().finishFlyweightTracer(parent, startInNanos, finishInNanos, className,
                    methodName, methodDesc, metricName, rollupMetricNames);
        }
    }

    @Override
    public Map<String, Object> getAgentAttributes() {
        Transaction tx = getTransactionIfExists();
        return (tx != null) ? tx.getAgentAttributes() : NoOpTransaction.INSTANCE.getAgentAttributes();
    }

    @Override
    public Map<String, Object> getUserAttributes() {
        Transaction tx = getTransactionIfExists();
        return (tx != null) ? tx.getUserAttributes() : NoOpTransaction.INSTANCE.getUserAttributes();
    }

    @Override
    public void provideHeaders(InboundHeaders headers) {
        Transaction tx = getTransactionIfExists();
        if (tx != null) {
            tx.provideHeaders(headers);
        }
    }

    @Override
    public String getRequestMetadata() {
        Transaction tx = getTransactionIfExists();
        return (tx != null) ? tx.getCrossProcessState().getRequestMetadata()
                : NoOpCrossProcessState.INSTANCE.getRequestMetadata();
    }

    @Override
    public void processRequestMetadata(String metadata) {
        Transaction tx = getTransactionIfExists();
        if (tx != null) {
            tx.getCrossProcessState().processRequestMetadata(metadata);
        }
    }

    @Override
    public String getResponseMetadata() {
        Transaction tx = getTransactionIfExists();
        return (tx != null) ? tx.getCrossProcessState().getResponseMetadata()
                : NoOpCrossProcessState.INSTANCE.getResponseMetadata();
    }

    @Override
    public void processResponseMetadata(String metadata) {
        Transaction tx = getTransactionIfExists();
        if (tx != null) {
            tx.getCrossProcessState().processResponseMetadata(metadata, null);
        }
    }

    @Override
    public void processResponseMetadata(String metadata, URI uri) {
        Transaction tx = getTransactionIfExists();
        if (tx != null) {
            tx.getCrossProcessState().processResponseMetadata(metadata, uri);
        }
    }

    @Override
    public void setWebRequest(ExtendedRequest request) {
        Transaction tx = getTransactionIfExists();
        if (tx != null) {
            tx.setWebRequest(request);
        }
    }

    @Override
    public boolean markFirstByteOfResponse() {
        Transaction tx = getTransactionIfExists();
        return (tx == null) ? false : tx.markFirstByteOfResponse(System.nanoTime());
    }

    @Override
    public boolean markLastByteOfResponse() {
        Transaction tx = getTransactionIfExists();
        return (tx == null) ? false : tx.markLastByteOfResponse(System.nanoTime());
    }

    @Override
    public void markResponseAtTxaEnd() {
        Transaction tx = getTransactionIfExists();
        if (tx != null) {
            tx.getTransactionActivity().markAsResponseSender();
        }
    }

    @Override
    public boolean markResponseSent() {
        final Transaction tx = getTransactionIfExists();
        if (tx == null) {
            Agent.LOG.log(Level.FINER, "ignoring markResponseSent: no transaction");
            return false;
        } else {
            tx.getTransactionActivity().markAsResponseSender();
            if (tx.getTransactionTimer().markResponseTime(System.nanoTime())) {
                Agent.LOG.log(Level.FINER, "markResponseSent: successful transaction response timestamp");
                return true;
            } else {
                Agent.LOG.log(Level.FINER, "ignoring markResponseSent: response time is already set for {0}", tx);
                return false;
            }
        }
    }

    @Override
    public TracedActivity createAndStartTracedActivity() {
        Transaction tx = getTransactionIfExists();
        if (null == tx) {
            return NoOpSegment.INSTANCE;
        }

        com.newrelic.agent.Segment segment = tx.startSegment(MetricNames.CUSTOM,
                com.newrelic.agent.Segment.UNNAMED_SEGMENT);
        return segment == null ? NoOpSegment.INSTANCE : segment;
    }

    @Override
    public DistributedTracePayload createDistributedTracePayload() {
        Transaction tx = getTransactionIfExists();
        if (tx == null) {
            return NoOpDistributedTracePayload.INSTANCE;
        }
        TransactionActivity txa = tx.getTransactionActivity();
        if (txa == null) {
            return NoOpDistributedTracePayload.INSTANCE;

        }
        return ServiceFactory.getDistributedTraceService().createDistributedTracePayload(txa.getLastTracer());

    }

    @Override
    public Token getToken() {
        Transaction tx = getTransactionIfExists();
        if (null == tx) {
            return NoOpToken.INSTANCE;
        }
        return tx.getToken();
    }

    @Override
    public Segment startSegment(String segmentName) {
        Transaction tx = getTransactionIfExists();
        if (null == tx) {
            return NoOpSegment.INSTANCE;
        }
        return startSegment(MetricNames.CUSTOM, segmentName);
    }

    @Override
    public Segment startSegment(String category, String segmentName) {
        Transaction tx = getTransactionIfExists();
        if (null == tx) {
            return NoOpSegment.INSTANCE;
        }

        if (category == null || category.isEmpty()) {
            // maybe log?
            category = MetricNames.CUSTOM;
        }

        if (segmentName == null || segmentName.isEmpty()) {
            // maybe log?
            segmentName = com.newrelic.agent.Segment.UNNAMED_SEGMENT;
        }

        Segment segment = tx.startSegment(category, segmentName);

        return segment == null ? NoOpSegment.INSTANCE : segment;
    }

    @Override
    public void expireAllTokens() {
        Transaction tx = getTransactionIfExists();
        if (null != tx) {
            tx.expireAllTokensForCurrentTransaction();
        }
    }

    @Override
    public boolean clearTransaction() {
        Transaction tx = getTransactionIfExists();
        TransactionActivity txa = tx.getTransactionActivity();
        if(txa != null) {
          tx.checkFinishTransactionFromActivity(txa);
        }
        Transaction.clearTransaction();
        return true;
    }

  @Override
    public void acceptDistributedTracePayload(String payload) {
        Transaction tx = getTransactionIfExists();
        if (tx != null) {
            tx.acceptDistributedTracePayload(payload);
        }
    }

    @Override
    public void acceptDistributedTracePayload(DistributedTracePayload payload) {
        Transaction tx = getTransactionIfExists();
        if (tx != null) {
            tx.acceptDistributedTracePayload(payload, null);
        }
    }

    @Override
    public void insertDistributedTraceHeaders(Headers headers) {
        Transaction tx = getTransactionIfExists();
        if (tx == null) {
            return;
        }
        HeadersUtil.createAndSetDistributedTraceHeaders(tx, NewRelic.getAgent().getTracedMethod(), headers);
    }

    @Override
    public void acceptDistributedTraceHeaders(TransportType transportType, Headers headers) {
        Transaction tx = getTransactionIfExists();
        if (tx == null) {
            return;
        }
        if (TransportType.Unknown.equals(tx.getTransportType())) {
            tx.setTransportType(transportType);
        }
        HeadersUtil.parseAndAcceptDistributedTraceHeaders(tx, headers);
    }

    @Override
    public Object getSecurityMetaData() {
        Transaction tx = getTransactionIfExists();
        if (tx != null) {
            return tx.getSecurityMetaData();
        }
        return null;
    }

    @Override
    public void setTransportType(TransportType transportType) {
        Transaction tx = getTransactionIfExists();
        if (tx != null) {
            tx.setTransportType(transportType);
        }
    }
}
