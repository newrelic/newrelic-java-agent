/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.tomcat;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;

public class TomcatUtils {

    private static final String JMX_PREFIX = "Catalina";
    private static final AtomicBoolean addedJmx = new AtomicBoolean(false);

    public static void addJmx() {
        if (System.getProperty("com.sun.aas.installRoot") == null) {
            if (!addedJmx.getAndSet(true)) {
                AgentBridge.jmxApi.addJmxMBeanGroup(JMX_PREFIX);
                NewRelic.getAgent().getLogger().log(Level.FINER, "Added JMX for Tomcat");
            }
        }
    }

}
