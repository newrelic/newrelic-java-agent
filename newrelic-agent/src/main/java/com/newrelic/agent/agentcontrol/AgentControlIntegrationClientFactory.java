/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.agentcontrol;

import com.newrelic.agent.Agent;
import com.newrelic.agent.config.AgentControlIntegrationConfig;

import java.util.logging.Level;

public class AgentControlIntegrationClientFactory {
    private static final AgentControlIntegrationHealthClient NO_OP_INSTANCE = new AgentControlHealthNoOpClientControl();

    public enum HealthClientType {
        noop,
        file,
    }
    public static AgentControlIntegrationHealthClient createHealthClient(AgentControlIntegrationConfig config) {
        AgentControlIntegrationHealthClient client;

        try {
            HealthClientType healthClientType = HealthClientType.valueOf(config.getHealthClientType());
            Agent.LOG.log(Level.INFO, "Generating Agent Control Health Client type: {0}", healthClientType);

            switch (healthClientType) {
                case file:
                    client = new AgentControlControlIntegrationHealthFileBasedClient(config);
                    break;

                default:
                    client = NO_OP_INSTANCE;
                    break;
            }
        } catch (Exception e) {
            Agent.LOG.log(Level.WARNING, "Invalid health client type: {0}; returning NoOp implementation", config.getHealthClientType());
            client = NO_OP_INSTANCE;
        }

        return client;
    }
}
