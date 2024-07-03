/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.superagent;

import com.newrelic.agent.superagent.protos.AgentToServer;

public interface SuperAgentIntegrationClient {
    void sendAgentToServerMessage(AgentToServer agentToServerMessage);
}
