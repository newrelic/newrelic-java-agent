/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.newrelic.agent.config.AgentConfig;

public interface ConnectionListener {

    void connected(IRPMService rpmService, AgentConfig agentConfig);

    void disconnected(IRPMService rpmService);
}
