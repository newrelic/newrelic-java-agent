/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.superagent;

import com.google.protobuf.ByteString;
import com.newrelic.agent.Agent;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigListener;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.superagent.protos.AgentCapabilities;
import com.newrelic.agent.superagent.protos.AgentDisconnect;
import com.newrelic.agent.superagent.protos.AgentToServer;
import com.newrelic.agent.superagent.protos.ComponentHealth;
import com.newrelic.agent.util.DefaultThreadFactory;
import org.apache.commons.lang3.StringUtils;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class SuperAgentIntegrationService extends AbstractService implements AgentConfigListener {
    private final String SA_INTEGRATION_THREAD_NAME = "New Relic Super Agent Integration Service";

    private AgentConfig agentConfig;
    private final SuperAgentIntegrationClient client;
    private final long startTimeNano;
    private ByteString instanceId = ByteString.copyFromUtf8("test");
    private long sequenceNum = 1L;
    private final long capabilities = AgentCapabilities.AgentCapabilities_ReportsHealth_VALUE;
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
            int messageSendFrequency = agentConfig.getSuperAgentIntegrationConfig().getFrequency(); //Used for both repeat frequency and initial delay

            this.scheduler = Executors.newSingleThreadScheduledExecutor(new DefaultThreadFactory(SA_INTEGRATION_THREAD_NAME, true));
            this.scheduler.scheduleWithFixedDelay(() -> {
                if (!instanceId.isEmpty()) {
                    client.sendAgentToServerMessage(generateAgentToServerOpAmpMessage(false));
                }
            }, messageSendFrequency, messageSendFrequency, TimeUnit.SECONDS);
        }
    }

    @Override
    protected void doStop() throws Exception {
        if (isEnabled()) {
            scheduler.shutdown();

            //Submit final msg with shutdown object
        }
    }

    @Override
    public boolean isEnabled() {
        return agentConfig.getSuperAgentIntegrationConfig().isEnabled();
    }

    private AgentToServer generateAgentToServerOpAmpMessage(boolean sendShutdownProperty) {
        AgentToServer.Builder builder = AgentToServer.newBuilder()
                    .setInstanceUid(instanceId)
                    .setSequenceNum(sequenceNum++)
                    .setCapabilities(capabilities)
                    .setHealth(generateComponentHealthOpAmpMessage());

        if (sendShutdownProperty) {
            builder.setAgentDisconnect(AgentDisconnect.newBuilder().build());
        }

        return builder.build();
    }

    private ComponentHealth generateComponentHealthOpAmpMessage() {
        return ComponentHealth.newBuilder()
                .setHealthy(agentHealth.isHealthy())
                .setStartTimeUnixNano(startTimeNano)
                .setLastError(agentHealth.getLastError())
                .setStatus(agentHealth.getCurrentStatus())
                .setStatusTimeUnixNano(getPseudoCurrentTimeNanos())
                .build();
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
