/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.superagent;

public class
SuperAgentHealthUnitTestClient implements SuperAgentIntegrationHealthClient {

    private AgentHealth agentHealth;

    @Override
    public void sendHealthMessage(AgentHealth agentHealth) {
        this.agentHealth = agentHealth;
    }

    public AgentHealth getAgentHealth() {
        return agentHealth;
    }
}
