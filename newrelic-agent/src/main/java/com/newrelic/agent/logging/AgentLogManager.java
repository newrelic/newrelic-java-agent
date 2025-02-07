/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.logging;

import com.newrelic.agent.config.AgentConfig;

public class AgentLogManager {

    static final String ROOT_LOGGER_NAME = "com.newrelic";

    // Ensure/Configure commons logging to use our own logger to prevent collisions (will be prepended by JarJar):
    // System.setProperty("org.apache.commons.logging.LogFactory", ApacheCommonsAdaptingLogFactory.class.getName());
    private static final IAgentLogManager INSTANCE = Log4jLogManager.create(ROOT_LOGGER_NAME);
    private static final IAgentLogger ROOT_LOGGER = INSTANCE.getRootLogger();

    private AgentLogManager() {
    }


    public static IAgentLogger getLogger() {
        return ROOT_LOGGER;
    }

    public static String getLogFilePath() {
        return INSTANCE.getLogFilePath();
    }

    public static void configureLogger(AgentConfig agentConfig) {
        INSTANCE.configureLogger(agentConfig);
    }

    public static void addConsoleHandler() {
        INSTANCE.addConsoleHandler();
    }

    public static void setLogLevel(String level) {
        INSTANCE.setLogLevel(level);
    }

    public static String getLogLevel() {
        return INSTANCE.getLogLevel();
    }

}
