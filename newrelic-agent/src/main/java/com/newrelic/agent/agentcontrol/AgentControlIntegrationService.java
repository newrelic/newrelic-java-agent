/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.agentcontrol;

import com.newrelic.agent.Agent;
import com.newrelic.agent.MetricNames;
import com.newrelic.agent.agentcontrol.effectiveconfig.AgentControlIntegrationEffectiveConfigClient;
import com.newrelic.agent.agentcontrol.health.AgentControlIntegrationHealthClient;
import com.newrelic.agent.agentcontrol.health.AgentHealth;
import com.newrelic.agent.agentcontrol.health.HealthDataChangeListener;
import com.newrelic.agent.agentcontrol.health.HealthDataProducer;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.util.DefaultThreadFactory;
import com.newrelic.api.agent.NewRelic;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class AgentControlIntegrationService extends AbstractService implements HealthDataChangeListener {
    private final AgentConfig agentConfig;
    private final AgentControlIntegrationHealthClient healthClient;
    private final AgentControlIntegrationEffectiveConfigClient effectiveConfigClient;
    private final AgentHealth agentHealth;

    private ScheduledExecutorService scheduler;

    public AgentControlIntegrationService(AgentControlIntegrationHealthClient healthClient,
            AgentControlIntegrationEffectiveConfigClient effectiveConfigClient,  AgentConfig agentConfig,
            HealthDataProducer... healthProducers) {
        super(AgentControlIntegrationService.class.getSimpleName());

        this.agentConfig = agentConfig;
        this.healthClient = healthClient;
        this.effectiveConfigClient = effectiveConfigClient;
        this.agentHealth = new AgentHealth(AgentControlIntegrationUtils.getPseudoCurrentTimeNanos());

        for (HealthDataProducer healthProducer : healthProducers) {
            healthProducer.registerHealthDataChangeListener(this);
        }
    }

    @Override
    protected void doStart() throws Exception {
        if (isEnabled()) {
            Agent.LOG.log(Level.INFO, "AgentControlIntegrationService starting: Health delivery location: {0}  Frequency: {1}  Scheme: {2}",
                    agentConfig.getAgentControlIntegrationConfig().getHealthDeliveryLocation(),
                    agentConfig.getAgentControlIntegrationConfig().getHealthReportingFrequency(),
                    agentConfig.getAgentControlIntegrationConfig().getHealthClientType());
            Agent.LOG.log(Level.INFO, "AgentControlIntegrationService starting: Effective config delivery location: {0}  Scheme: {2}",
                    agentConfig.getAgentControlIntegrationConfig().getEffectiveConfigDeliveryLocation(),
                    agentConfig.getAgentControlIntegrationConfig().getEffectiveConfigClientType());
            NewRelic.getAgent().getMetricAggregator().incrementCounter(MetricNames.SUPPORTABILITY_AGENT_CONTROL_ENABLED);

            int messageSendFrequency = agentConfig.getAgentControlIntegrationConfig().getHealthReportingFrequency(); //Used for both repeat frequency and initial delay

            this.scheduler = Executors.newSingleThreadScheduledExecutor(new DefaultThreadFactory("New Relic Agent Control Integration Service", true));
            this.scheduler.scheduleWithFixedDelay(() -> healthClient.sendHealthMessage(agentHealth), messageSendFrequency, messageSendFrequency, TimeUnit.SECONDS);

            effectiveConfigClient.sendEffectiveConfigMessage(ServiceFactory.getConfigService().getExplicitlySetConfig());
        }
    }

    @Override
    protected void doStop() throws Exception {
        if (isEnabled()) {
            scheduler.shutdown();
            agentHealth.setUnhealthyStatus(AgentHealth.Status.SHUTDOWN);
            healthClient.sendHealthMessage(agentHealth);
        }
    }

    @Override
    public boolean isEnabled() {
        return agentConfig.getAgentControlIntegrationConfig().isEnabled() &&
                healthClient != null && healthClient.isValid() &&
                effectiveConfigClient != null && effectiveConfigClient.isValid();
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

    @Override
    public void assignEntityGuid(String entityGuid) {
        if (isEnabled()) {
            agentHealth.setEntityGuid(entityGuid);
        }
    }
}
