/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracing;

import com.newrelic.agent.Agent;
import com.newrelic.agent.ConnectionListener;
import com.newrelic.agent.ExtendedTransactionListener;
import com.newrelic.agent.HarvestListener;
import com.newrelic.agent.IRPMService;
import com.newrelic.agent.MetricNames;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionData;
import com.newrelic.agent.bridge.NoOpDistributedTracePayload;
import com.newrelic.agent.tracing.samplers.AdaptiveSampler;
import com.newrelic.agent.tracing.samplers.Sampler;
import com.newrelic.api.agent.TransportType;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigListener;
import com.newrelic.agent.config.DistributedTracingConfig;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.StatsEngine;
import com.newrelic.agent.stats.StatsService;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.agent.tracers.AbstractTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.api.agent.DistributedTracePayload;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

public class DistributedTraceServiceImpl extends AbstractService implements DistributedTraceService, ConnectionListener,
        ExtendedTransactionListener, HarvestListener, AgentConfigListener {

    private static final int MAJOR_CAT_VERSION = 0;
    private static final int MINOR_CAT_VERSION = 1;

    private final AtomicReference<String> accountId = new AtomicReference<>();
    private final AtomicReference<String> applicationId = new AtomicReference<>();
    private final AtomicReference<String> trustKey = new AtomicReference<>();
    private final AtomicBoolean firstHarvest = new AtomicBoolean(true);

    private DistributedTracingConfig distributedTraceConfig;

    private Sampler sampler;
    private Sampler remoteParentSampledSampler;
    private Sampler remoteParentNotSampledSampler;


    // Instantiate a new DecimalFormat instance as it is not thread safe:
    // http://jonamiller.com/2015/12/21/decimalformat-is-not-thread-safe/
    private static final ThreadLocal<DecimalFormat> FORMATTER =
            new ThreadLocal<DecimalFormat>() {
                @Override
                protected DecimalFormat initialValue() {
                    DecimalFormat format = new DecimalFormat();
                    DecimalFormatSymbols symbols = format.getDecimalFormatSymbols();
                    if (symbols.getDecimalSeparator() == ',') {
                        format.applyLocalizedPattern("#,######");
                    } else {
                        format.applyPattern("#.######");
                    }
                    return format;
                }
            };

    public DistributedTraceServiceImpl() {
        super(DistributedTraceServiceImpl.class.getSimpleName());
        distributedTraceConfig = ServiceFactory.getConfigService().getDefaultAgentConfig().getDistributedTracingConfig();
        ServiceFactory.getConfigService().addIAgentConfigListener(this);
        //Initially, set up the samplers based on local config.
        //The adaptive sampler (SAMPLE_DEFAULT) will have its target overridden when we receive the connect response later.
        this.sampler = Sampler.getSamplerForType(DistributedTracingConfig.SAMPLE_DEFAULT);
        this.remoteParentSampledSampler = Sampler.getSamplerForType(distributedTraceConfig.getRemoteParentSampled());
        this.remoteParentNotSampledSampler = Sampler.getSamplerForType(distributedTraceConfig.getRemoteParentNotSampled());
    }

    @Override
    public boolean isEnabled() {
        return distributedTraceConfig.isEnabled();
    }

    @Override
    protected void doStart() throws Exception {
        ServiceFactory.getRPMServiceManager().addConnectionListener(this);
        ServiceFactory.getTransactionService().addTransactionListener(this);
        ServiceFactory.getHarvestService().addHarvestListener(this);

        // track feature for angler
        if (isEnabled()) {
            StatsService statsService = ServiceFactory.getServiceManager().getStatsService();
            statsService.getMetricAggregator().incrementCounter(MetricNames.SUPPORTABILITY_DISTRIBUTED_TRACING);
            statsService.getMetricAggregator().incrementCounter(MessageFormat.format(MetricNames.SUPPORTABILITY_DISTRIBUTED_TRACING_EXCLUDE_NEWRELIC_HEADER,
                    !distributedTraceConfig.isIncludeNewRelicHeader()));
        }
    }

    @Override
    protected void doStop() throws Exception {
        ServiceFactory.getRPMServiceManager().removeConnectionListener(this);
        ServiceFactory.getTransactionService().removeTransactionListener(this);
        ServiceFactory.getHarvestService().removeHarvestListener(this);
    }

    @Override
    public void connected(IRPMService rpmService, AgentConfig agentConfig) {
        DistributedTracingConfig dtConfig = agentConfig.getDistributedTracingConfig();

        String appId = String.valueOf(dtConfig.getPrimaryApplicationId());
        if (appId != null) {
            applicationId.set(appId);
        }

        String accId = String.valueOf(dtConfig.getAccountId());
        if (accId != null) {
            accountId.set(accId);
        }

        String trustedAccountKey = String.valueOf(dtConfig.getTrustedAccountKey());
        if (trustedAccountKey != null) {
            trustKey.set(trustedAccountKey);
        } else {
            trustKey.set(getAccountId());
        }

        // Fallback in case none of the previous attempts set the application ID
        applicationId.compareAndSet(null, "0");

        //The connect response includes a server-only config, sampling_target, that MUST be used to configure the adaptive sampler shared instance.
        AdaptiveSampler.setSharedTarget(agentConfig.getAdaptiveSamplingTarget());
    }

    @Override
    public void disconnected(IRPMService rpmService) {
        accountId.set(null); // reset accountId on disconnect
    }

    @Override
    public void beforeHarvest(String appName, StatsEngine statsEngine) {
        firstHarvest.set(false);
        // After the first harvest we no longer need this listener
        ServiceFactory.getHarvestService().removeHarvestListener(this);
    }

    @Override
    public void afterHarvest(String appName) {
    }

    @Override
    public int getMajorSupportedCatVersion() {
        return MAJOR_CAT_VERSION;
    }

    @Override
    public int getMinorSupportedCatVersion() {
        return MINOR_CAT_VERSION;
    }

    @Override
    public String getAccountId() {
        return accountId.get();
    }

    @Override
    public String getTrustKey() {
        if (trustKey.get() == null) {
            return getAccountId();
        }
        return trustKey.get();
    }

    @Override
    public String getApplicationId() {
        return applicationId.get();
    }

    @Override
    public float calculatePriorityRemoteParent(boolean remoteParentSampled, Float inboundPriority){
        Sampler parentSampler = remoteParentSampled ? remoteParentSampledSampler : remoteParentNotSampledSampler;
        if (parentSampler.getType().equals(Sampler.ADAPTIVE) && inboundPriority != null) {
            return inboundPriority;
        }
        return parentSampler.calculatePriority();
    }

    @Override
    public float calculatePriorityRoot(){
        return sampler.calculatePriority();
    }

    @Override
    public Map<String, Object> getIntrinsics(DistributedTracePayloadImpl inboundPayload, String guid, String traceId, TransportType transportType,
            long parentTransportDurationInMillis, long largestTransportDuration, String parentId, String parentSpanId, float priority) {
        Map<String, Object> intrinsicAttributes = new HashMap<>();

        try {
            if (transportType != null) {
                intrinsicAttributes.put("parent.transportType", transportType.name());
            }

            if (inboundPayload != null) {
                if (inboundPayload.parentType != null) {
                    intrinsicAttributes.put("parent.type", inboundPayload.parentType);
                }
                if (inboundPayload.applicationId != null) {
                    intrinsicAttributes.put("parent.app", inboundPayload.applicationId);
                }
                if (inboundPayload.accountId != null) {
                    intrinsicAttributes.put("parent.account", inboundPayload.accountId);
                }
                if (transportType != null) {
                    intrinsicAttributes.put("parent.transportType", transportType.name());
                }
                if (parentTransportDurationInMillis >= 0) {
                    float transportDurationSec = parentTransportDurationInMillis / 1000f;
                    intrinsicAttributes.put("parent.transportDuration", transportDurationSec);
                }
            }

            if (guid != null) {
                intrinsicAttributes.put("guid", guid);
            }
            if (traceId != null) {
                intrinsicAttributes.put("traceId", traceId);
            }

            intrinsicAttributes.put("priority", priority);
            intrinsicAttributes.put("sampled", DistributedTraceUtil.isSampledPriority(priority));
        } catch (Exception e) {
            Agent.LOG.log(Level.FINEST, e, "Failed to retrieve distributed trace intrinsics.");
        }

        return intrinsicAttributes;
    }

    @Override
    public void dispatcherTransactionStarted(Transaction transaction) {
    }

    @Override
    public void dispatcherTransactionCancelled(Transaction transaction) {
    }

    @Override
    public void dispatcherTransactionFinished(TransactionData transactionData, TransactionStats transactionStats) {
        if (distributedTraceConfig.isEnabled()) {
            Map<String, Object> distributedTracingIntrinsics = getIntrinsics(
                    transactionData.getInboundDistributedTracePayload(), transactionData.getGuid(),
                    transactionData.getTraceId(), transactionData.getTransportType(),
                    transactionData.getTransportDurationInMillis(), transactionData.getLargestTransportDurationInMillis(),
                    transactionData.getParentId(), transactionData.getParentSpanId(), transactionData.getPriority());
            transactionData.getIntrinsicAttributes().putAll(distributedTracingIntrinsics);
            recordMetrics(transactionData, transactionStats);
        }
    }

    public static float nextTruncatedFloat() {
        float next = 0.0f;
        try {
            next = Float.parseFloat(FORMATTER.get().format(ThreadLocalRandom.current().nextFloat()).replace(',', '.'));
        } catch (NumberFormatException e) {
            Agent.LOG.log(Level.WARNING, "Unable to create priority value", e);
        }
        return next;
    }

    private void recordMetrics(TransactionData transactionData, TransactionStats transactionStats) {
        DistributedTracePayloadImpl payload = transactionData.getInboundDistributedTracePayload();
        if (payload == null) {
            transactionStats.getUnscopedStats()
                    .getOrCreateResponseTimeStats(MessageFormat.format(MetricNames.DURATION_BY_PARENT_UNKNOWN_ALL, transactionData.getTransportType()))
                    .recordResponseTimeInNanos(transactionData.getTransactionTime().getResponseTimeInNanos());

            if (transactionData.isWebTransaction()) {
                transactionStats.getUnscopedStats()
                        .getOrCreateResponseTimeStats(MessageFormat.format(MetricNames.DURATION_BY_PARENT_UNKNOWN_ALL_WEB, transactionData.getTransportType()))
                        .recordResponseTimeInNanos(transactionData.getTransactionTime().getResponseTimeInNanos());
            }

            if (transactionData.hasReportableErrorThatIsNotIgnored()) {
                transactionStats.getUnscopedStats()
                        .getStats(MessageFormat.format(MetricNames.ERRORS_BY_PARENT_UNKNOWN, transactionData.getTransportType()))
                        .incrementCallCount();
            }
        } else {
            String txType = MetricNames.PARENT_DATA_ALL_OTHER;
            if (transactionData.isWebTransaction()) {
                txType = MetricNames.PARENT_DATA_ALL_WEB;
            }

            // Tx duration by parent (/all + /allWeb or /allOther)
            String parentDurationAll = MessageFormat.format(MetricNames.DURATION_BY_PARENT, payload.parentType,
                    payload.accountId, payload.applicationId, transactionData.getTransportType().name(),
                    MetricNames.PARENT_DATA_ALL);
            String parentDurationByType = MessageFormat.format(MetricNames.DURATION_BY_PARENT, payload.parentType,
                    payload.accountId, payload.applicationId, transactionData.getTransportType().name(), txType);
            long transactionResponseTime = transactionData.getTransactionTime().getResponseTimeInNanos();
            transactionStats.getUnscopedStats().getOrCreateResponseTimeStats(parentDurationAll)
                    .recordResponseTimeInNanos(transactionResponseTime);
            transactionStats.getUnscopedStats().getOrCreateResponseTimeStats(parentDurationByType)
                    .recordResponseTimeInNanos(transactionResponseTime);

            // Transport duration by parent (/all + /allWeb or /allOther)
            String transportDurationAll = MessageFormat.format(MetricNames.TRANSPORT_DURATION_BY_PARENT,
                    payload.parentType, payload.accountId, payload.applicationId,
                    transactionData.getTransportType().name(), MetricNames.PARENT_DATA_ALL);
            String transportDurationByType = MessageFormat.format(MetricNames.TRANSPORT_DURATION_BY_PARENT,
                    payload.parentType, payload.accountId, payload.applicationId,
                    transactionData.getTransportType().name(), txType);
            long transportDuration = transactionData.getTransportDurationInMillis();
            transactionStats.getUnscopedStats().getOrCreateResponseTimeStats(transportDurationAll)
                    .recordResponseTime(transportDuration, TimeUnit.MILLISECONDS);
            transactionStats.getUnscopedStats().getOrCreateResponseTimeStats(transportDurationByType)
                    .recordResponseTime(transportDuration, TimeUnit.MILLISECONDS);

            if (transactionData.hasReportableErrorThatIsNotIgnored()) {
                // Errors by parent (/all + /allWeb or /allOther)
                String errorsByParentAll = MessageFormat.format(MetricNames.ERRORS_BY_PARENT, payload.parentType,
                        payload.accountId, payload.applicationId, transactionData.getTransportType().name(),
                        MetricNames.PARENT_DATA_ALL);
                String errorsByParentByType = MessageFormat.format(MetricNames.ERRORS_BY_PARENT, payload.parentType,
                        payload.accountId, payload.applicationId, transactionData.getTransportType().name(), txType);
                transactionStats.getUnscopedStats().getStats(errorsByParentAll).incrementCallCount();
                transactionStats.getUnscopedStats().getStats(errorsByParentByType).incrementCallCount();
            }
        }
    }

    @Override
    public DistributedTracePayload createDistributedTracePayload(Tracer tracer) {
        if (tracer == null) {
            return NoOpDistributedTracePayload.INSTANCE;
        }

        Transaction tx = tracer.getTransactionActivity().getTransaction();
        String spanId = null;

        // Override guid if this trace is sampled and spans are enabled
        // guid will be the guid of the span that is creating this payload
        boolean spansEnabled = ServiceFactory.getConfigService().getDefaultAgentConfig().getSpanEventsConfig().isEnabled();
        tx.assignPriorityRootIfNotSet();
        boolean sampled = DistributedTraceUtil.isSampledPriority(tx.getPriority());
        if (sampled && spansEnabled) {
            // Need to do this in case the span that created this is a @Trace(excludeFromTransactionTrace=true)
            Tracer tracerWithSpan = AbstractTracer.getParentTracerWithSpan(tracer);
            spanId = tracerWithSpan != null ? tracerWithSpan.getGuid() : null;
        }

        DistributedTracePayload distributedTracePayload = tx.createDistributedTracePayload(spanId);
        if (distributedTracePayload == null) {
            return NoOpDistributedTracePayload.INSTANCE;
        }
        return distributedTracePayload;
    }

    @Override
    public void configChanged(String appName, AgentConfig agentConfig) {
        boolean wasEnabled = isEnabled();
        this.distributedTraceConfig = agentConfig.getDistributedTracingConfig();

        if (!wasEnabled && isEnabled()) {
            StatsService statsService = ServiceFactory.getServiceManager().getStatsService();
            statsService.getMetricAggregator().incrementCounter(MetricNames.SUPPORTABILITY_DISTRIBUTED_TRACING);
            statsService.getMetricAggregator().incrementCounter(MessageFormat.format(MetricNames.SUPPORTABILITY_DISTRIBUTED_TRACING_EXCLUDE_NEWRELIC_HEADER,
                    !distributedTraceConfig.isIncludeNewRelicHeader()));
        }
    }

    // These setters are NOT thread-safe.
    // They should NOT be used outside of testing.
    void setRemoteParentSampledSampler(Sampler sampler) {
        this.remoteParentSampledSampler = sampler;
    }

    void setRemoteParentNotSampledSampler(Sampler sampler) {
        this.remoteParentNotSampledSampler = sampler;
    }

    void setRootSampler(Sampler sampler) {
        this.sampler = sampler;
    }

    Sampler getRootSampler(){
        return sampler;
    }

    Sampler getRemoteParentSampledSampler(){
        return remoteParentSampledSampler;
    }

    Sampler getRemoteParentNotSampledSampler(){
        return remoteParentNotSampledSampler;
    }
}
