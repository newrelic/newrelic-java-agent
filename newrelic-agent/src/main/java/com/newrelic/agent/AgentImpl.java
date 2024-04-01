/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.newrelic.agent.aimonitoring.AiMonitoringImpl;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.NoOpMetricAggregator;
import com.newrelic.agent.bridge.NoOpTracedMethod;
import com.newrelic.agent.bridge.NoOpTransaction;
import com.newrelic.agent.bridge.TracedMethod;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.api.agent.AiMonitoring;
import com.newrelic.api.agent.ErrorApi;
import com.newrelic.api.agent.Insights;
import com.newrelic.api.agent.Logger;
import com.newrelic.api.agent.Logs;
import com.newrelic.api.agent.MetricAggregator;
import com.newrelic.api.agent.TraceMetadata;

import java.util.Map;
import java.util.logging.Level;

public class AgentImpl implements com.newrelic.agent.bridge.Agent {

    private final Logger logger;

    public AgentImpl(Logger logger) {
        this.logger = logger;
    }

    /**
     * If in a transaction, then getTransaction().getTracedMethod() returns the same thing as this method. If outside a
     * transaction then this method returns a noop. Note: The getTransaction().getTracedMethod() will return null.
     */
    @Override
    public TracedMethod getTracedMethod() {
        com.newrelic.agent.bridge.Transaction transaction = getTransaction(false);
        if (NoOpTransaction.INSTANCE.equals(transaction)) {
            return NoOpTracedMethod.INSTANCE;
        }
        com.newrelic.agent.Transaction txn = com.newrelic.agent.Transaction.getTransaction(false);
        // These have already been checked in getTransaction but I am just copying the getTracedMethod code on
        // TransactionApiImpl to ensure the same behavior.
        if (txn == null) {
            return NoOpTracedMethod.INSTANCE;
        }
        TransactionActivity txa = txn.getTransactionActivity();
        if (txa == null) {
            return NoOpTracedMethod.INSTANCE;
        }
        Tracer tracer = txa.getLastTracer();
        return (tracer == null) ? NoOpTracedMethod.INSTANCE : tracer;
    }

    /**
     * This method will be invoked through the public client api.
     */
    @Override
    public Transaction getTransaction() {
        TransactionActivity txa = com.newrelic.agent.TransactionActivity.get();
        if (txa != null) {
            Tracer tracer = txa.getRootTracer();

            // async = true optimization. Don't create a txn if we don't need one.
            com.newrelic.agent.Transaction txn = com.newrelic.agent.Transaction.getTransaction(false);
            if (txn == null && tracer != null) {
                if (tracer.isAsync()) {
                    return NoOpTransaction.INSTANCE;
                }
            }
        }

        // If this succeeds, return the standard unbound wrapper to the Transaction instance on this thread. If not,
        // (e.g. on an Agent thread), return something so callers don't have to code around a null value.
        com.newrelic.agent.Transaction innerTx = com.newrelic.agent.Transaction.getTransaction(false);
        if (innerTx != null) {
            return TransactionApiImpl.INSTANCE;
        }
        return NoOpTransaction.INSTANCE;
    }

    @Override
    public Transaction getTransaction(boolean createIfNotExists) {
        if (null == com.newrelic.agent.Transaction.getTransaction(createIfNotExists)) {
            return null;
        }
        return getTransaction();
    }

    @Override
    public Transaction getWeakRefTransaction(boolean createIfNotExists) {
        com.newrelic.agent.Transaction transaction = com.newrelic.agent.Transaction.getTransaction(createIfNotExists);
        if (transaction == null) {
            return null;
        }
        return new WeakRefTransaction(transaction);
    }

    @Override
    public com.newrelic.api.agent.Logger getLogger() {
        return logger;
    }

    @Override
    public ErrorApi getErrorApi() {
        return AgentBridge.publicApi;
    }

    @Override
    public com.newrelic.api.agent.Config getConfig() {
        return ServiceFactory.getConfigService().getDefaultAgentConfig();
    }

    @Override
    public MetricAggregator getMetricAggregator() {
        try {
            com.newrelic.agent.Transaction tx = com.newrelic.agent.Transaction.getTransaction(false);
            if (null != tx && tx.isInProgress()) {
                return tx.getMetricAggregator();
            }
            return ServiceFactory.getStatsService().getMetricAggregator();
        } catch (Throwable t) {
            Agent.LOG.log(Level.FINE, "getMetricAggregator() call failed : {0}", t.getMessage());
            Agent.LOG.log(Level.FINEST, t, "getMetricAggregator() call failed");
            return NoOpMetricAggregator.INSTANCE;
        }
    }

    @Override
    public Insights getInsights() {
        return ServiceFactory.getServiceManager().getInsights();
    }

    @Override
    public AiMonitoring getAiMonitoring() {
        return new AiMonitoringImpl();
    }

    @Override
    public Logs getLogSender() {
        return ServiceFactory.getServiceManager().getLogSenderService();
    }

    @Override
    public boolean startAsyncActivity(Object activityContext) {
        return ServiceFactory.getAsyncTxService().startAsyncActivity(activityContext);
    }

    @Override
    public boolean ignoreIfUnstartedAsyncContext(Object activityContext) {
        return ServiceFactory.getAsyncTxService().ignoreIfUnstartedAsyncContext(activityContext);
    }

    @Override
    public TraceMetadata getTraceMetadata() {
        return TraceMetadataImpl.INSTANCE;
    }

    @Override
    public Map<String, String> getLinkingMetadata() {
        return AgentLinkingMetadata.getLinkingMetadata(
                getTraceMetadata(),
                ServiceFactory.getConfigService(),
                ServiceFactory.getRPMServiceManager() != null ? ServiceFactory.getRPMServiceManager().getRPMService() : null
        );
    }

}
