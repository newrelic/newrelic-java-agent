/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import com.newrelic.agent.Agent;
import com.newrelic.agent.config.internal.NoOpEnvironmentFacade;
import com.newrelic.agent.config.internal.SystemEnvironmentFacade;

import java.util.Map;
import java.util.logging.Level;

public abstract class EnvironmentFacade {
    static EnvironmentFacade getInstance() {
        try {
            System.getenv("test"); // this is a check to generate a security exception if misconfigured.
            return new SystemEnvironmentFacade();
        } catch (SecurityException e) {
            Agent.LOG.log(Level.SEVERE, e, "Unable to access environment variables for configuration");
            return new NoOpEnvironmentFacade();
        }
    }

    public abstract String getenv(String key);

    public abstract Map<String, String> getAllEnvProperties();
}
