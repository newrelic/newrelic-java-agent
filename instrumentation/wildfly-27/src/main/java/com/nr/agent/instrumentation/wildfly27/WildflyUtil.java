/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.wildfly27;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import java.lang.management.ManagementFactory;
import java.util.logging.Level;

public class WildflyUtil {
    public static void setServerInfo() {
        String version = "27+";
        String dispatcherName = "WildFly";

      	MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        ObjectName name = null;

        try {
            name = new ObjectName("jboss.as:management-root=server");
        } catch (MalformedObjectNameException e) {
            NewRelic.getAgent().getLogger().log(Level.FINEST, e, "Error getting WildFly version");
        }

        String productVersion = getServerAttribute(server, name, "productVersion");
        String releaseVersion = getServerAttribute(server, name, "releaseVersion");
        String productName = getServerAttribute(server, name, "productName");

        // Wildfly 9/10 changed the meaning of releaseVersion and added productVersion.
        // So we check product first since release will give the wrong value in 9/10
        if (productVersion != null) {
            version = productVersion;
        } else if (releaseVersion != null) {
            version = releaseVersion;
        }

        // This instrumentation applies to JBoss EAP as well and we don't want
        // to change the Dispatcher name for existing Wildfly users.
        if (productName != null && !productName.startsWith("WildFly")) {
            dispatcherName = productName;
        }

        AgentBridge.publicApi.setServerInfo(dispatcherName, version);
    }

    private static String getServerAttribute(MBeanServer server, ObjectName objectName, String attributeName) {
        String attribute = null;
        try {
            attribute = (String) server.getAttribute(objectName, attributeName);
        } catch (MBeanException | AttributeNotFoundException | InstanceNotFoundException | ReflectionException e) {
            NewRelic.getAgent().getLogger().log(Level.FINEST, e, "Error getting " + attributeName);
        }
        return attribute;
    }
}
