/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.superagent;

import com.newrelic.agent.Agent;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigListener;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.util.DefaultThreadFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class SuperAgentIntegrationService extends AbstractService implements AgentConfigListener {
    private final String SA_INTEGRATION_THREAD_NAME = "New Relic Super Agent Integration Service";

    private AgentConfig agentConfig;
    private final SuperAgentIntegrationClient client;
    private final long startTimeNano;
    private final AgentHealth agentHealth;

    private ScheduledExecutorService scheduler;

    public SuperAgentIntegrationService(SuperAgentIntegrationClient client, AgentConfig agentConfig) {
        super(SuperAgentIntegrationService.class.getSimpleName());
        this.agentConfig = agentConfig;
        this.client = client;
        this.startTimeNano = getPseudoCurrentTimeNanos();
        this.agentHealth = new AgentHealth();

        ServiceFactory.getConfigService().addIAgentConfigListener(this);
    }

    @Override
    protected void doStart() throws Exception {
        if (isEnabled()) {
            Agent.LOG.log(Level.INFO, "SuperAgentIntegrationService starting");
            int messageSendFrequency = agentConfig.getSuperAgentIntegrationConfig().getHealthReportingFrequency(); //Used for both repeat frequency and initial delay

            this.scheduler = Executors.newSingleThreadScheduledExecutor(new DefaultThreadFactory(SA_INTEGRATION_THREAD_NAME, true));
            this.scheduler.scheduleWithFixedDelay(() -> client.sendHealthMessage(generateHealthInfoMessage()), messageSendFrequency, messageSendFrequency, TimeUnit.SECONDS);
        }
    }

    @Override
    protected void doStop() throws Exception {
        if (isEnabled()) {
            scheduler.shutdown();
        }
    }

    @Override
    public boolean isEnabled() {
        return agentConfig.getSuperAgentIntegrationConfig().isEnabled();
    }

    private String generateHealthInfoMessage() {
        return "";
    }

    private long getPseudoCurrentTimeNanos() {
        // The message expects the time in nanoseconds. Since this is a practical impossibility on most hardware,
        // simply get the current ms and multiply.
        return System.currentTimeMillis() * 1000000;
    }

    @Override
    public void configChanged(String appName, AgentConfig agentConfig) {
        this.agentConfig = agentConfig;
    }
}
