/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import com.newrelic.agent.Agent;
import com.newrelic.agent.config.internal.DefaultSystemProps;
import com.newrelic.agent.config.internal.NoOpSystemProps;

import java.util.Properties;
import java.util.logging.Level;

public abstract class SystemProps {

    static SystemProps getSystemProps() {
        try {
            System.getProperties().get("test");
            return new DefaultSystemProps();
        } catch (SecurityException e) {
            Agent.LOG.log(Level.SEVERE, e, "Unable to access system properties because of a security exception.");
            return new NoOpSystemProps();
        }
    }

    public abstract String getSystemProperty(String prop);

    public abstract Properties getAllSystemProperties();
}
