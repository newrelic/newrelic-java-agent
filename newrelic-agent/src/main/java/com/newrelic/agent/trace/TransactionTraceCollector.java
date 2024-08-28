/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.trace;

import com.newrelic.agent.Agent;
import com.newrelic.agent.IRPMService;
import com.newrelic.agent.IgnoreSilentlyException;
import com.newrelic.agent.TransactionData;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.TransactionTracerConfigImpl;
import com.newrelic.agent.profile.v2.TransactionProfileService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.StatsEngine;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

import static com.newrelic.agent.logging.AgentLogManager.getLogger;

public class TransactionTraceCollector {

    /**
     * Number of samples the random sampler should capture before it stops.
     */
    private static final int INITIAL_TRACE_LIMIT = 5;
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

    public TransactionTraceCollector() {
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

    public void addTransactionTraceSampler(ITransactionSampler transactionSampler) {
        transactionSamplers.add(transactionSampler);
    }

    public void removeTransactionTraceSampler(ITransactionSampler transactionSampler) {
        transactionSamplers.remove(transactionSampler);
    }

    public boolean isThreadCpuTimeEnabled() {
        return threadCpuTimeEnabled || isProfileSessionActive();
    }

    public boolean isProfileSessionActive() {
        if (transactionProfileService == null) {
            return false;
        }
        return transactionProfileService.isTransactionProfileSessionActive() && threadMXBean.isCurrentThreadCpuTimeSupported();
    }

    public boolean initThreadCPUEnabled(AgentConfig config) {
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
     *
     * @param transactionData TransactionData representing a given transaction that has just completed
     * @return TransactionSampler
     */
    public ITransactionSampler getOrCreateNamedSampler(TransactionData transactionData) {
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

    public boolean noticeTransaction(TransactionData transactionData) {
        if (syntheticsTransactionSampler.noticeTransaction(transactionData)) {
            return true;
        }
        for (ITransactionSampler transactionSampler : transactionSamplers) {
            if (transactionSampler.noticeTransaction(transactionData)) {
                return true;
            }
        }
        ITransactionSampler sampler = getOrCreateNamedSampler(transactionData);
        if (sampler != null) {
            return sampler.noticeTransaction(transactionData);
        }
        return false;
    }

    public void beforeHarvest(String appName, StatsEngine statsEngine) {
    }

    /**
     * This is the exit point where all captured transaction traces are harvested.
     *
     * @param appName Name of the current application
     */
    public void afterHarvest(String appName) {
        List<TransactionTrace> traces = harvestTransactionTraces(appName);
        if (!traces.isEmpty()) {
            IRPMService rpmService = ServiceFactory.getRPMServiceManager().getOrCreateRPMService(appName);
            sendTraces(rpmService, traces);
        }
    }

    public List<TransactionTrace> harvestTransactionTraces(String appName) {
        List<TransactionTrace> traces = new ArrayList<>();
        if (autoAppNameEnabled) {
            traces.addAll(getNamedSamplerTraces(appName));
        } else {
            traces.addAll(getNamedSamplerTraces(TransactionTracerConfigImpl.REQUEST_CATEGORY_NAME));
            traces.addAll(getNamedSamplerTraces(TransactionTracerConfigImpl.BACKGROUND_CATEGORY_NAME));
        }
        traces.addAll(this.syntheticsTransactionSampler.harvest(appName));
        for (ITransactionSampler transactionSampler : transactionSamplers) {
            traces.addAll(transactionSampler.harvest(appName));
        }

        return traces;
    }

    public List<TransactionTrace> getNamedSamplerTraces(String appName) {
        ITransactionSampler sampler = namedSamplers.get(appName);
        if (sampler != null) {
            return sampler.harvest(appName);
        }
        return Collections.emptyList();
    }

    public void sendTraces(IRPMService rpmService, List<TransactionTrace> traces) {
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

    public void doStart() {
        transactionProfileService = ServiceFactory.getProfilerService().getTransactionProfileService();
        RandomTransactionSampler randomTransactionSampler = new RandomTransactionSampler(INITIAL_TRACE_LIMIT, this);
        addTransactionTraceSampler(randomTransactionSampler);

        if (Agent.LOG.isLoggable(Level.FINER)) {
            String msg = MessageFormat.format("Started random transaction tracing: max traces={0}", INITIAL_TRACE_LIMIT);
            Agent.LOG.finer(msg);
        }
    }

    public void doStop() {
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

    public ITransactionSampler createSampler() {
        return new TransactionTraceSampler();
    }

    /**
     * This is the entry point for evaluating whether a completed transaction
     * should be considered to generate a transaction trace from.
     *
     * @param transactionData TransactionData representing a given transaction that has just completed
     */
    public boolean evaluateAsPotentialTransactionTrace(TransactionData transactionData) {
        if (!transactionData.getTransactionTracerConfig().isEnabled()) {
            return false;
        }
        return noticeTransaction(transactionData);
    }

}
