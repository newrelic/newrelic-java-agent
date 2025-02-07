/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.core;

import com.newrelic.agent.Agent;
import com.newrelic.agent.IRPMService;
import com.newrelic.agent.InstrumentationProxy;
import com.newrelic.agent.MetricNames;
import com.newrelic.agent.PrivateApiImpl;
import com.newrelic.agent.TransactionService;
import com.newrelic.agent.cloud.CloudApiImpl;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.logging.AgentLogManager;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.StatsService;
import com.newrelic.agent.agentcontrol.HealthDataChangeListener;
import com.newrelic.agent.agentcontrol.HealthDataProducer;
import com.newrelic.api.agent.NewRelicApiImplementation;

import java.lang.instrument.Instrumentation;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.MessageFormat;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

public class CoreServiceImpl extends AbstractService implements CoreService, HealthDataProducer {
    private volatile boolean enabled = true;
    private final Instrumentation instrumentation;
    private volatile InstrumentationProxy instrumentationProxy;
    private final List<HealthDataChangeListener> healthDataChangeListeners = new CopyOnWriteArrayList<>();


    public CoreServiceImpl(Instrumentation instrumentation) {
        super(CoreService.class.getName());
        this.instrumentation = instrumentation;
    }

    @Override
    protected void doStart() {
        ConfigService configService = ServiceFactory.getConfigService();
        AgentConfig config = configService.getDefaultAgentConfig();
        AgentLogManager.configureLogger(config);

        logHostIp();
        Agent.LOG.info(MessageFormat.format("New Relic Agent v{0} is initializing...", Agent.getVersion()));

        enabled = config.isAgentEnabled();
        if (!enabled) {
            Agent.LOG.info("New Relic agent is disabled.");
        }

        if (config.liteMode()) {
            Agent.LOG.info("New Relic agent is running in lite mode. All instrumentation modules are disabled");
            StatsService statsService = ServiceFactory.getServiceManager().getStatsService();
            statsService.getMetricAggregator().incrementCounter(MetricNames.SUPPORTABILITY_LITE_MODE);
        }

        instrumentationProxy = InstrumentationProxy.getInstrumentationProxy(instrumentation);

        initializeBridgeApis();

        final long startTime = System.currentTimeMillis();
        Thread shutdownThread = new Thread(() -> jvmShutdown(startTime), "New Relic JVM Shutdown");
        Runtime.getRuntime().addShutdownHook(shutdownThread);
    }

    private void initializeBridgeApis() {
        NewRelicApiImplementation.initialize();
        PrivateApiImpl.initialize(Agent.LOG);
        CloudApiImpl.initialize();
    }

    /**
     * Logs the host and the IP address of the server currently running this agent.
     */
    private void logHostIp() {
        try {
            InetAddress address = InetAddress.getLocalHost();
            Agent.LOG.info("Agent Host: " + address.getHostName() + " IP: " + address.getHostAddress());
        } catch (UnknownHostException e) {
            Agent.LOG.info("New Relic could not identify host/ip.");
        }
    }

    @Override
    protected void doStop() {
    }

    @Override
    public void shutdownAsync() {
        Thread shutdownThread = new Thread(this::shutdown, "New Relic Shutdown");
        shutdownThread.start();
    }

    private void jvmShutdown(long startTime) {
        getLogger().fine("Agent JVM shutdown hook: enter.");

        AgentConfig config = ServiceFactory.getConfigService().getDefaultAgentConfig();

        // Only add the "transaction wait" shutdown hook if one has been configured
        if (config.waitForTransactionsInMillis() > 0) {
            getLogger().fine("Agent JVM shutdown hook: waiting for transactions to finish");

            // While there are still transactions in progress and we haven't hit the configured timeout keep
            // waiting and checking for the transactions to finish before allowing the shutdown to continue.
            long finishTime = System.currentTimeMillis() + config.waitForTransactionsInMillis();
            TransactionService txService = ServiceFactory.getTransactionService();
            while (txService.getTransactionsInProgress() > 0 && System.currentTimeMillis() < finishTime) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                }
            }

            getLogger().fine("Agent JVM shutdown hook: transactions finished");
        }

        if (config.isSendDataOnExit() && ((System.currentTimeMillis() - startTime) >= config.getSendDataOnExitThresholdInMillis())) {
            // Grab all RPMService instances (may be multiple with auto_app_naming enabled) and harvest them
            List<IRPMService> rpmServices = ServiceFactory.getRPMServiceManager().getRPMServices();
            for (IRPMService rpmService : rpmServices) {
                rpmService.harvestNow();
            }
        }

        getLogger().info("JVM is shutting down");
        shutdown();
        getLogger().fine("Agent JVM shutdown hook: done.");
    }

    private synchronized void shutdown() {
        try {
            ServiceFactory.getServiceManager().stop();
            getLogger().info("New Relic Agent has shutdown");
        } catch (Throwable t) {
            Agent.LOG.log(Level.SEVERE, t, "Error shutting down New Relic Agent");
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public InstrumentationProxy getInstrumentation() {
        return instrumentationProxy;
    }

    @Override
    public Instrumentation getRealInstrumentation() {
        return instrumentation;
    }

    @Override
    public void registerHealthDataChangeListener(HealthDataChangeListener listener) {
        healthDataChangeListeners.add(listener);
    }
}
