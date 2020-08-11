/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import com.newrelic.agent.Agent;
import com.newrelic.agent.config.internal.DefaultSystemProps;
import com.newrelic.agent.config.internal.MapSystemProps;
import com.newrelic.agent.discovery.AgentArguments;
import com.newrelic.bootstrap.BootstrapAgent;

import java.util.Collections;
import java.util.Properties;
import java.util.logging.Level;

import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public abstract class SystemProps {

    static SystemProps getSystemProps() {
        try {
            final String agentArgs = System.getProperty(BootstrapAgent.NR_AGENT_ARGS_SYSTEM_PROPERTY);
            if (agentArgs != null) {
                try {
                    final AgentArguments args = AgentArguments.fromJsonObject(new JSONParser().parse(agentArgs));
                    return new MapSystemProps(args.getSystemProperties());
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
            }
            return new DefaultSystemProps();
        } catch (SecurityException e) {
            Agent.LOG.log(Level.SEVERE, e, "Unable to access system properties because of a security exception.");
            return new MapSystemProps(Collections.<String, String>emptyMap());
        }
    }

    public abstract String getSystemProperty(String prop);

    public abstract Properties getAllSystemProperties();
}
