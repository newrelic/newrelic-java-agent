/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.agentcontrol;

import com.newrelic.agent.Agent;
import com.newrelic.agent.agentcontrol.health.AgentControlIntegrationHealthClient;
import com.newrelic.agent.agentcontrol.health.AgentControlIntegrationHealthFileBasedClient;
import com.newrelic.agent.agentcontrol.health.AgentControlIntegrationHealthNoOpClient;
import com.newrelic.agent.config.agentcontrol.AgentControlIntegrationConfig;

import java.util.logging.Level;

public class AgentControlIntegrationClientFactory {
    private static final AgentControlIntegrationHealthClient HEALTH_NO_OP_INSTANCE = new AgentControlIntegrationHealthNoOpClient();

    public enum ClientType {
        noop,
        file,
        collector
    }
    public static AgentControlIntegrationHealthClient  createHealthClient(AgentControlIntegrationConfig config) {
        AgentControlIntegrationHealthClient client;

        try {
            ClientType healthClientType = ClientType.valueOf(config.getHealthClientType());
            Agent.LOG.log(Level.INFO, "Generating Agent Control Health Client type: {0}", healthClientType);

            switch (healthClientType) {
                case file:
                    client = new AgentControlIntegrationHealthFileBasedClient(config);
                    break;

                default:
                    client = HEALTH_NO_OP_INSTANCE;
                    break;
            }
        } catch (Exception e) {
            Agent.LOG.log(Level.WARNING, "Invalid health client type: {0}; returning NoOp implementation", config.getHealthClientType());
            client = HEALTH_NO_OP_INSTANCE;
        }

        return client;
    }
}
