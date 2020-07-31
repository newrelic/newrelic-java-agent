/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.eclipse.jetty.server;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

@Weave(originalName = "org.eclipse.jetty.server.Server")
public class Server_Instrumentation {

    @NewField
    private static final AtomicBoolean addedJmx = new AtomicBoolean(false);

    protected void doStart() throws Exception {
        if (!addedJmx.getAndSet(true)) {
            AgentBridge.jmxApi.addJmxMBeanGroup("org.eclipse.jetty");
            NewRelic.getAgent().getLogger().log(Level.FINER, "Added JMX for Jetty");
        }
        Weaver.callOriginal();
    }

}