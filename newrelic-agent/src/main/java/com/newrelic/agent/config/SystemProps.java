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
import com.newrelic.agent.discovery.Discovery;
import com.newrelic.bootstrap.BootstrapAgent;

import java.util.Collections;
import java.util.Map;
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
                    @SuppressWarnings("unchecked")
                    final Map<String, Object> map = (Map<String, Object>) new JSONParser().parse(agentArgs);
                    @SuppressWarnings("unchecked")
                    final Map<String, String> props = (Map<String, String>) map.get(
                            Discovery.SYSTEM_PROPERTIES_AGENT_ARGS_KEY);
                    if (props == null) {
                        throw new IllegalArgumentException("No system properties were provided for attach");
                    }
                    return new MapSystemProps(props);
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
