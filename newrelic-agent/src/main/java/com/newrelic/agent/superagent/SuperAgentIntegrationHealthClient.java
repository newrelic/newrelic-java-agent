/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.superagent;

public interface SuperAgentIntegrationHealthClient {
    void sendHealthMessage(AgentHealth agentHealth);

    boolean isValid();
}
