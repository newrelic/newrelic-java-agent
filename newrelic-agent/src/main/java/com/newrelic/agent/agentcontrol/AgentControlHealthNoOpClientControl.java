/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.agentcontrol;

public class AgentControlHealthNoOpClientControl implements AgentControlIntegrationHealthClient {
    @Override
    public void sendHealthMessage(AgentHealth agentHealth) {

    }

    @Override
    public boolean isValid() {
        return true;
    }
}
