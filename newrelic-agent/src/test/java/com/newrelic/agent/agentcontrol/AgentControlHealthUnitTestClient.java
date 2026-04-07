/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.agentcontrol;

public class AgentControlHealthUnitTestClient implements AgentControlIntegrationHealthClient {

    private AgentHealth agentHealth;

    @Override
    public void sendHealthMessage(AgentHealth agentHealth) {
        this.agentHealth = agentHealth;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    public AgentHealth getAgentHealth() {
        return agentHealth;
    }
}
