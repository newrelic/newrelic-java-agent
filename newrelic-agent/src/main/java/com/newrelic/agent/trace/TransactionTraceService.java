/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.trace;

import com.newrelic.agent.HarvestListener;
import com.newrelic.agent.IRPMService;
import com.newrelic.agent.TransactionData;
import com.newrelic.agent.TransactionListener;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.StatsEngine;
import com.newrelic.agent.stats.TransactionStats;

import java.lang.management.ThreadMXBean;
import java.util.List;

/**
 * This service collects transaction traces according to a set of rules and transmits them to RPM.
 * <p>
 * This class is thread-safe.
 */
public class TransactionTraceService extends AbstractService implements HarvestListener, TransactionListener {

    /*-
     * This service collects transaction traces based on a complex set of interacting rules that have evolved over time.
     * It maintains a collection (really several collections) of TransactionSamplers, each of which can preserve a trace
     * for the next harvest.
     *
     * Each time a transaction completes in the instrumented JVM, a notification arrives at this service. This service
     * offers the transaction to each of the samplers in an order that is partially hardwired in the code and partially
     * determined at runtime. The first sampler that "takes" the transaction cuts off the search. The search order is:
     *
     * 1) the sole SyntheticsTransactionSampler
     * 2) the sampler for each active X-Ray session, in the order that the sessions started, most recent first.
     * 3) each of the samplers add via the public addTransactionTraceSampler API, which as of this writing is:
     * 3a) a random sampler for one expensive transaction
     * 4) one of the samplers, either:
     * 4a) a sampler associated with the app name, if auto app naming is in use, or
     * 4b) one of two hardwired samplers, one for web transactions and the other for non-web transactions.
     */

    private final TransactionTraceCollector transactionTraceCollector;

    public TransactionTraceService() {
        super(TransactionTraceService.class.getSimpleName());
        this.transactionTraceCollector = new TransactionTraceCollector();
    }

    public ThreadMXBean getThreadMXBean() {
        return transactionTraceCollector.getThreadMXBean();
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public void addTransactionTraceSampler(ITransactionSampler transactionSampler) {
        transactionTraceCollector.addTransactionTraceSampler(transactionSampler);
    }

    public void removeTransactionTraceSampler(ITransactionSampler transactionSampler) {
        transactionTraceCollector.removeTransactionTraceSampler(transactionSampler);
    }

    public boolean isThreadCpuTimeEnabled() {
        return transactionTraceCollector.isThreadCpuTimeEnabled();
    }

    private boolean isProfileSessionActive() {
        return transactionTraceCollector.isProfileSessionActive();
    }

    private boolean initThreadCPUEnabled(AgentConfig config) {
        return transactionTraceCollector.initThreadCPUEnabled(config);
    }

    /**
     * Get or create the named sampler for the given transactionData.
     * If auto-app-naming is configured, the sampler name will be the app name.
     * If auto-app-naming is not configured, the sampler will be named after the dispatcher (web or background).
     */
    private ITransactionSampler getOrCreateNamedSampler(TransactionData transactionData) {
        return transactionTraceCollector.getOrCreateNamedSampler(transactionData);
    }

    private void noticeTransaction(TransactionData transactionData) {
        transactionTraceCollector.noticeTransaction(transactionData);
    }

    @Override
    public void beforeHarvest(String appName, StatsEngine statsEngine) {
        transactionTraceCollector.beforeHarvest(appName, statsEngine);
    }

    @Override
    public void afterHarvest(String appName) {
        transactionTraceCollector.afterHarvest(appName);
    }

    private List<TransactionTrace> getNamedSamplerTraces(String appName) {
        return transactionTraceCollector.getNamedSamplerTraces(appName);
    }

    private void sendTraces(IRPMService rpmService, List<TransactionTrace> traces) {
        transactionTraceCollector.sendTraces(rpmService, traces);
    }

    @Override
    protected void doStart() {
        ServiceFactory.getTransactionService().addTransactionListener(this);
        ServiceFactory.getHarvestService().addHarvestListener(this);
        transactionTraceCollector.doStart();
    }

    @Override
    protected void doStop() {
        transactionTraceCollector.doStop();
    }

    private ITransactionSampler createSampler() {
        return transactionTraceCollector.createSampler();
    }

    @Override
    public void dispatcherTransactionFinished(TransactionData transactionData, TransactionStats transactionStats) {
        // If transaction_traces_as_spans is false, then sample transaction traces here in TransactionTraceService
        // otherwise transaction traces will be sampled in SpanEventsServiceImpl
        if (!ServiceFactory.getConfigService().getDefaultAgentConfig().getTransactionTracerConfig().getTransactionTracesAsSpans()) {
            transactionTraceCollector.considerSamplingTransactionTrace(transactionData);
        }
    }
}
