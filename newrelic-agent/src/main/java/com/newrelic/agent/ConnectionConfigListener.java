/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.newrelic.agent.config.AgentConfig;

import java.util.Map;

public interface ConnectionConfigListener {

    AgentConfig connected(IRPMService rpmService, Map<String, Object> connectionInfo);

}
