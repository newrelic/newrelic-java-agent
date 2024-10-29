/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.superagent;

import com.newrelic.agent.Agent;
import com.newrelic.agent.MetricNames;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigListener;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.util.DefaultThreadFactory;
import com.newrelic.api.agent.NewRelic;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class SuperAgentIntegrationService extends AbstractService implements HealthDataChangeListener {
    private final String SA_INTEGRATION_THREAD_NAME = "New Relic Super Agent Integration Service";

    private final AgentConfig agentConfig;
    private final SuperAgentIntegrationHealthClient client;
    private final AgentHealth agentHealth;

    private ScheduledExecutorService scheduler;

    public SuperAgentIntegrationService(SuperAgentIntegrationHealthClient client, AgentConfig agentConfig,
            HealthDataProducer... healthProducers) {
        super(SuperAgentIntegrationService.class.getSimpleName());

        this.agentConfig = agentConfig;
        this.client = client;
        this.agentHealth = new AgentHealth(SuperAgentIntegrationUtils.getPseudoCurrentTimeNanos());

        for (HealthDataProducer healthProducer : healthProducers) {
            healthProducer.registerHealthDataChangeListener(this);
        }
    }

    @Override
    protected void doStart() throws Exception {
        if (isEnabled()) {
            Agent.LOG.log(Level.INFO, "SuperAgentIntegrationService starting: Health file location: {0}  Frequency: {1}  Scheme: {2}",
                    agentConfig.getSuperAgentIntegrationConfig().getHealthDeliveryLocation(),
                    agentConfig.getSuperAgentIntegrationConfig().getHealthReportingFrequency(),
                    agentConfig.getSuperAgentIntegrationConfig().getHealthClientType());
            NewRelic.getAgent().getMetricAggregator().incrementCounter(MetricNames.SUPPORTABILITY_SUPERAGENT_HEALTH_REPORTING_ENABLED);

            int messageSendFrequency = agentConfig.getSuperAgentIntegrationConfig().getHealthReportingFrequency(); //Used for both repeat frequency and initial delay

            this.scheduler = Executors.newSingleThreadScheduledExecutor(new DefaultThreadFactory(SA_INTEGRATION_THREAD_NAME, true));
            this.scheduler.scheduleWithFixedDelay(() -> client.sendHealthMessage(agentHealth), messageSendFrequency, messageSendFrequency, TimeUnit.SECONDS);
        }
    }

    @Override
    protected void doStop() throws Exception {
        if (isEnabled()) {
            scheduler.shutdown();
            agentHealth.setUnhealthyStatus(AgentHealth.Status.SHUTDOWN);
            client.sendHealthMessage(agentHealth);
        }
    }

    @Override
    public boolean isEnabled() {
        return agentConfig.getSuperAgentIntegrationConfig().isEnabled();
    }

    @Override
    public void onUnhealthyStatus(AgentHealth.Status newStatus, String... additionalInfo) {
        if (isEnabled()) {
            agentHealth.setUnhealthyStatus(newStatus, additionalInfo);
        }
    }

    @Override
    public void onHealthyStatus(AgentHealth.Category... categories) {
        if (isEnabled()) {
            for (AgentHealth.Category category : categories) {
                agentHealth.setHealthyStatus(category);
            }
        }
    }
}
