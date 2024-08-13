/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.trace;

import com.newrelic.agent.HarvestListener;
import com.newrelic.agent.IRPMService;
import com.newrelic.agent.IgnoreSilentlyException;
import com.newrelic.agent.PriorityTransactionListener;
import com.newrelic.agent.TransactionData;
import com.newrelic.agent.TransactionListener;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.TransactionTracerConfigImpl;
import com.newrelic.agent.profile.v2.TransactionProfileService;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.StatsEngine;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.api.agent.NewRelic;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

/**
 * This service collects transaction traces according to a set of rules and transmits them to RPM.
 *
 * This class is thread-safe.
 */
public class TransactionTraceService extends AbstractService implements HarvestListener, PriorityTransactionListener {

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

    /**
     * Number of samples the random sampler should capture before it stops.
     */
    private static final int INITIAL_TRACE_LIMIT = 5; // TODO look into this
    // config
    private final ThreadMXBean threadMXBean;
    private final boolean autoAppNameEnabled;
    private final boolean threadCpuTimeEnabled;
    private final ConfigService configService;

    /**
     * named samplers.
     * If auto app naming this maps appName -> sampler
     * Otherwise this contains two sampler names for transactions (web and background)
     */
    private final ConcurrentMap<String, ITransactionSampler> namedSamplers;
    /**
     * ordered list of transaction samplers
     * package-private for tests only
     */
    final List<ITransactionSampler> transactionSamplers = new CopyOnWriteArrayList<>();

    // special samplers which are hardcoded to run in specific notice/harvest order
    // synthetics
    private final SyntheticsTransactionSampler syntheticsTransactionSampler;
    private volatile TransactionProfileService transactionProfileService;

    public TransactionTraceService() {
        super(TransactionTraceService.class.getSimpleName());
        namedSamplers = new ConcurrentHashMap<>();
        namedSamplers.put(TransactionTracerConfigImpl.REQUEST_CATEGORY_NAME, createSampler());
        namedSamplers.put(TransactionTracerConfigImpl.BACKGROUND_CATEGORY_NAME, createSampler());

        addTransactionTraceSampler(new KeyTransactionTraceSampler());

        threadMXBean = ManagementFactory.getThreadMXBean();
        configService = ServiceFactory.getConfigService();
        AgentConfig config = configService.getDefaultAgentConfig();
        autoAppNameEnabled = config.isAutoAppNamingEnabled();
        threadCpuTimeEnabled = initThreadCPUEnabled(config);

        syntheticsTransactionSampler = new SyntheticsTransactionSampler();
    }

    public ThreadMXBean getThreadMXBean() {
        return threadMXBean;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public void addTransactionTraceSampler(ITransactionSampler transactionSampler) {
        transactionSamplers.add(transactionSampler);
    }

    public void removeTransactionTraceSampler(ITransactionSampler transactionSampler) {
        transactionSamplers.remove(transactionSampler);
    }

    public boolean isThreadCpuTimeEnabled() {
        return threadCpuTimeEnabled || isProfileSessionActive();
    }

    private boolean isProfileSessionActive() {
        if (transactionProfileService == null) {
            return false;
        }
        return transactionProfileService.isTransactionProfileSessionActive() && threadMXBean.isCurrentThreadCpuTimeSupported();
    }

    private boolean initThreadCPUEnabled(AgentConfig config) {
        Boolean prop = config.getProperty(AgentConfigImpl.THREAD_CPU_TIME_ENABLED, false);
        if (prop == null || !prop) {
            return false;
        }

        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

        return threadMXBean.isThreadCpuTimeSupported() && threadMXBean.isThreadCpuTimeEnabled();
    }

    /**
     * Get or create the named sampler for the given transactionData.
     * If auto-app-naming is configured, the sampler name will be the app name.
     * If auto-app-naming is not configured, the sampler will be named after the dispatcher (web or background).
     */
    private ITransactionSampler getOrCreateNamedSampler(TransactionData transactionData) {
        final String samplerName;
        if (autoAppNameEnabled) {
            samplerName = transactionData.getApplicationName();
        } else {
            samplerName = transactionData.getDispatcher().isWebTransaction() ?
                    TransactionTracerConfigImpl.REQUEST_CATEGORY_NAME
                    : TransactionTracerConfigImpl.BACKGROUND_CATEGORY_NAME;
        }
        ITransactionSampler sampler = namedSamplers.get(samplerName);
        if (sampler == null) {
            sampler = createSampler();
            ITransactionSampler existingSampler = namedSamplers.putIfAbsent(transactionData.getApplicationName(), sampler);
            if (existingSampler != null) {
                return existingSampler;
            }
        }
        return sampler;
    }

    private void noticeTransaction(TransactionData transactionData) {
        // Synthetics transactions
        if (syntheticsTransactionSampler.noticeTransaction(transactionData)) {
            return;
        }
        // Key transactions, random transaction
        for (ITransactionSampler transactionSampler : transactionSamplers) {
            if (transactionSampler.noticeTransaction(transactionData)) {
                return;
            }
        }
        //
        ITransactionSampler sampler = getOrCreateNamedSampler(transactionData);
        if (sampler != null) {
            sampler.noticeTransaction(transactionData);
        }
    }

    @Override
    public void beforeHarvest(String appName, StatsEngine statsEngine) {
    }

    @Override
    public void afterHarvest(String appName) {
        List<TransactionTrace> traces = new ArrayList<>();
        if (autoAppNameEnabled) {
            List<TransactionTrace> transactionTraces = getNamedSamplerTraces(appName);
            if (!transactionTraces.isEmpty()) {
                NewRelic.getAgent().getLogger().log(Level.INFO, "Captured " + transactionTraces.size() + " transaction traces for appName " + appName);
            }
            traces.addAll(transactionTraces); // sampler for app name
        } else {
            List<TransactionTrace> webTransactionTraces = getNamedSamplerTraces(TransactionTracerConfigImpl.REQUEST_CATEGORY_NAME);
            if (!webTransactionTraces.isEmpty()) {
                NewRelic.getAgent().getLogger().log(Level.INFO, "Captured " + webTransactionTraces.size() + " web transaction traces");
            }
            traces.addAll(webTransactionTraces); // sampler for web txns

            List<TransactionTrace> backgroundTransactionTraces = getNamedSamplerTraces(TransactionTracerConfigImpl.BACKGROUND_CATEGORY_NAME);
            if (!backgroundTransactionTraces.isEmpty()) {
                NewRelic.getAgent().getLogger().log(Level.INFO, "Captured " + backgroundTransactionTraces.size() + " background transaction traces");
            }
            traces.addAll(backgroundTransactionTraces); // sampler for background txns
        }
        List<TransactionTrace> syntheticTransactionTraces = this.syntheticsTransactionSampler.harvest(appName);
        if (!syntheticTransactionTraces.isEmpty()) {
            NewRelic.getAgent().getLogger().log(Level.INFO, "Captured " + syntheticTransactionTraces.size() + " synthetic transaction traces");
        }
        traces.addAll(syntheticTransactionTraces); // sampler for synthetics
        for (ITransactionSampler transactionSampler : transactionSamplers) {
            List<TransactionTrace> transactionTraces = transactionSampler.harvest(appName);
            if (!transactionTraces.isEmpty()) {
                NewRelic.getAgent().getLogger().log(Level.INFO, "Captured " + transactionTraces.size() + " " + transactionSampler.getClass().getName() + " transaction traces");
            }
            traces.addAll(transactionTraces);  // sampler for Key txns, random txns,
        }

        if (!traces.isEmpty()) {
            IRPMService rpmService = ServiceFactory.getRPMServiceManager().getOrCreateRPMService(appName);
            sendTraces(rpmService, traces);
            NewRelic.getAgent().getLogger().log(Level.INFO, "afterHarvest#sendTraces: Captured # of traces: " + traces.size());
        }
    }

    private List<TransactionTrace> getNamedSamplerTraces(String appName) {
        ITransactionSampler sampler = namedSamplers.get(appName);
        if (sampler != null) {
            return sampler.harvest(appName);
        }
        return Collections.emptyList();
    }

    private void sendTraces(IRPMService rpmService, List<TransactionTrace> traces) {
        if (!rpmService.isConnected()) {
            return;
        }
        try {
            rpmService.sendTransactionTraceData(traces);
        } catch (IgnoreSilentlyException e) {
            // ignore
        } catch (Exception e) {
            // HttpError/LicenseException handled here
            if (getLogger().isLoggable(Level.FINEST)) {
                getLogger().log(Level.FINEST, e, "Error sending transaction trace data to {0} for {1}: {2}",
                        rpmService.getHostString(), rpmService.getApplicationName(), e.getMessage());
            } else {
                getLogger().log(Level.FINE, "Error sending transaction trace data to {0} for {1}: {2}",
                        rpmService.getHostString(), rpmService.getApplicationName(), e.getMessage());
            }
        }
    }

    @Override
    protected void doStart() {
        ServiceFactory.getTransactionService().addTransactionListener(this);
        ServiceFactory.getHarvestService().addHarvestListener(this);
        transactionProfileService = ServiceFactory.getProfilerService().getTransactionProfileService();

        RandomTransactionSampler.startSampler(INITIAL_TRACE_LIMIT);
    }

    @Override
    protected void doStop() {
        syntheticsTransactionSampler.stop();
        for (ITransactionSampler sampler : transactionSamplers) {
            sampler.stop();
        }
        transactionSamplers.clear();
        for (ITransactionSampler sampler : namedSamplers.values()) {
            sampler.stop();
        }
        namedSamplers.clear();
    }

    private ITransactionSampler createSampler() {
        return new TransactionTraceSampler();
    }

    @Override
    public void dispatcherTransactionFinished(TransactionData transactionData, TransactionStats transactionStats) {
        if (!transactionData.getTransactionTracerConfig().isEnabled()) {
            return;
        }
        noticeTransaction(transactionData);
    }
}
