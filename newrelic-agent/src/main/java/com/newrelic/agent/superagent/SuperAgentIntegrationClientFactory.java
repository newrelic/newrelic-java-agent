/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.superagent;

import com.newrelic.agent.Agent;
import com.newrelic.agent.config.SuperAgentIntegrationConfig;

import java.util.logging.Level;

public class SuperAgentIntegrationClientFactory {
    private static final SuperAgentIntegrationHealthClient NO_OP_INSTANCE = new SuperAgentHealthNoOpClient();

    public enum HealthClientType {
        noop,
        file,
    }
    public static SuperAgentIntegrationHealthClient createHealthClient(String type, SuperAgentIntegrationConfig config) {
        SuperAgentIntegrationHealthClient client;

        try {
            HealthClientType healthClientType = HealthClientType.valueOf(type);
            Agent.LOG.log(Level.INFO, "Generating SuperAgent Health Client type: {0}", type);

            switch (healthClientType) {
                case file:
                    client = new SuperAgentIntegrationHealthFileBasedClient(config);
                    break;

                default:
                    client = NO_OP_INSTANCE;
                    break;
            }
        } catch (Exception e) {
            Agent.LOG.log(Level.WARNING, "Invalid health client type: {0}; returning NoOp implementation", type);
            client = NO_OP_INSTANCE;
        }

        return client;
    }
}
