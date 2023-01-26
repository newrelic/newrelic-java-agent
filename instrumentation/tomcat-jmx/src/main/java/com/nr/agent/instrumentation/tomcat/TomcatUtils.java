/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.tomcat;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

public class TomcatUtils {

    private static final String JMX_PREFIX = "Catalina";
    private static final String JMX_EMBEDDED_PREFIX = "Tomcat";
    private static final String JMX_EMBEDDED_DATASOURCE_PREFIX = "org.apache.tomcat.jdbc.pool.jmx";

    private static final AtomicBoolean addedJmx = new AtomicBoolean(false);

    public static void addJmx() {
        if (System.getProperty("com.sun.aas.installRoot") == null) {
            if (!addedJmx.getAndSet(true)) {
                MBeanServer server = ManagementFactory.getPlatformMBeanServer();
                //The ObjectName is different if embedded tomcat is used. Checking that this object has been registered
                //will let us know that we are in an embedded tomcat. The Set returned by queryNames will return size 0 if the ObjectName is not
                //found
                try {
                    if (server.queryNames(new ObjectName("Tomcat:type=Server"), null).size() == 1) {
                        AgentBridge.jmxApi.addJmxMBeanGroup(JMX_EMBEDDED_PREFIX);
                        NewRelic.getAgent().getLogger().log(Level.FINER, "Added JMX for Tomcat");

                        if (server.queryNames(new ObjectName("org.apache.tomcat.jdbc.pool.jmx:name=dataSourceMBean,type=ConnectionPool"), null)
                                .size() == 1) {
                            AgentBridge.jmxApi.addJmxMBeanGroup(JMX_EMBEDDED_DATASOURCE_PREFIX);
                            NewRelic.getAgent().getLogger().log(Level.FINER, "Added JMX for Tomcat dataSourceMbean ConnectionPool");
                        }

                    } else {
                        // It is safe to assume we are in a non embedded Tomcat (Catalina) which uses Catalina for the ObjectName, no need to query.
                        AgentBridge.jmxApi.addJmxMBeanGroup(JMX_PREFIX);
                        NewRelic.getAgent().getLogger().log(Level.FINER, "Added JMX for Catalina");
                    }
                } catch (MalformedObjectNameException e) {
                    NewRelic.getAgent().getLogger().log(Level.FINEST, e, e.getMessage());
                }
            }
        }
    }

}
