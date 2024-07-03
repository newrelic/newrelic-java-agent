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
import com.newrelic.agent.superagent.protos.AgentCapabilities;
import com.newrelic.agent.superagent.protos.AgentDisconnect;
import com.newrelic.agent.superagent.protos.AgentToServer;
import com.newrelic.agent.superagent.protos.ComponentHealth;
import com.newrelic.agent.util.DefaultThreadFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class SuperAgentIntegrationService extends AbstractService implements AgentConfigListener {
    private final String SA_INTEGRATION_THREAD_NAME = "New Relic Super Agent Integration Service";

    private AgentConfig agentConfig;
    private final long startTimeNano;
    private ByteString instanceId = ByteString.copyFromUtf8("test");
    private long sequenceNum = 1L;
    private final long capabilities = AgentCapabilities.AgentCapabilities_ReportsHealth_VALUE;
    private AgentHealth agentHealth;

    public SuperAgentIntegrationService(AgentConfig agentConfig) {
        super(SuperAgentIntegrationService.class.getSimpleName());
        this.agentConfig = agentConfig;
        this.startTimeNano = getPseudoCurrentTimeNanos();
        this.agentHealth = new AgentHealth();
    }

    @Override
    protected void doStart() throws Exception {
        // Check enabled, grab socket addr from config
        //SuperAgentIntegrationClient client = new SuperAgentDomainSocketIntegrationClient(agentConfig.getSuperAgentIntegrationConfig().getEndpoint());
        Agent.LOG.log(Level.INFO, "foooooooooooo");
        SuperAgentIntegrationClient client = new SuperAgentDomainSocketIntegrationClient("/Users/jduffy/foo.socket");
        ScheduledExecutorService s = Executors.newSingleThreadScheduledExecutor(new DefaultThreadFactory(SA_INTEGRATION_THREAD_NAME, true));
        AgentToServer a = generateAgentToServerOpAmpMessage(false);
        Agent.LOG.log(Level.INFO, "2222222222");

//        s.scheduleWithFixedDelay(() -> client.sendAgentToServerMessage(generateAgentToServerOpAmpMessage(false)),
//                5,
//                5,
//                TimeUnit.SECONDS);
        s.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                System.out.println("4444444444444444444");
                Agent.LOG.log(Level.INFO, "zzzzzzzzzzzzzzzzzzzzzzzzzz  " + AgentToServer.class.getName());
                AgentToServer a = generateAgentToServerOpAmpMessage(false);
                Agent.LOG.log(Level.INFO, "aaaaaaaaaaaaaaaaaaaaaaaaa\n" + a.toString());
                client.sendAgentToServerMessage(generateAgentToServerOpAmpMessage(false));

            }
        }, 5, 5, TimeUnit.SECONDS);
    }

    @Override
    protected void doStop() throws Exception {

    }

    @Override
    public boolean isEnabled() {
        return false;
        //return agentConfig.isEnabled();
    }

    private AgentToServer generateAgentToServerOpAmpMessage(boolean sendShutdownProperty) {
        AgentToServer.Builder builder;
        try {

            builder = AgentToServer.newBuilder()
                    .setInstanceUid(instanceId)
                    .setSequenceNum(sequenceNum++)
                    .setCapabilities(capabilities)
                    .setHealth(generateComponentHealthOpAmpMessage());

            if (sendShutdownProperty) {
                builder.setAgentDisconnect(AgentDisconnect.newBuilder().build());
            }

            return builder.build();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return null;
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

    private static class AgentToServerMessageSender implements Runnable {
        public AgentToServerMessageSender() {

        }

        @Override
        public void run() {

        }
    }
}
