/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.logging;

import com.newrelic.agent.config.AgentConfig;

public interface IAgentLogManager {

    IAgentLogger getRootLogger();

    String getLogFilePath();

    void configureLogger(AgentConfig agentConfig);

    void addConsoleHandler();

    void setLogLevel(String level);

    String getLogLevel();

}
