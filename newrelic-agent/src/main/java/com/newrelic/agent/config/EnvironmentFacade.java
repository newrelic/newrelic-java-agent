/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import com.newrelic.agent.Agent;
import com.newrelic.agent.config.internal.MapEnvironmentFacade;
import com.newrelic.agent.config.internal.SystemEnvironmentFacade;
import com.newrelic.agent.discovery.AgentArguments;
import com.newrelic.bootstrap.BootstrapAgent;

import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;

import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public abstract class EnvironmentFacade {
    static EnvironmentFacade getInstance() {
        final String agentArgs = System.getProperty(BootstrapAgent.NR_AGENT_ARGS_SYSTEM_PROPERTY);
        if (agentArgs != null) {
            try {
                final AgentArguments args = AgentArguments.fromJsonObject(new JSONParser().parse(agentArgs));
                return new MapEnvironmentFacade(args.getEnvironment());
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }
        try {
            System.getenv("test"); // this is a check to generate a security exception if misconfigured.
            return new SystemEnvironmentFacade();
        } catch (SecurityException e) {
            Agent.LOG.log(Level.SEVERE, e, "Unable to access environment variables for configuration");
            return new MapEnvironmentFacade(Collections.<String, String>emptyMap());
        }
    }

    public abstract String getenv(String key);

    public abstract Map<String, String> getAllEnvProperties();
}
