/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package weblogic.server;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.weaver.Weave;

import com.newrelic.api.agent.weaver.WeaveAllConstructors;
import weblogic.management.configuration.ServerTemplateMBean;
import weblogic.version;

import java.util.logging.Level;

@Weave(originalName = "weblogic.server.ServerLifeCycleRuntime")
public final class ServerLifeCycleRuntime_Instrumentation {
    @WeaveAllConstructors
    ServerLifeCycleRuntime_Instrumentation() {
        AgentBridge.getAgent().getLogger().log(Level.FINEST, "ServerLifeCycleRuntime_Instrumentation - constructor called");
        AgentBridge.getAgent().getLogger().log(Level.FINEST, "ServerLifeCycleRuntime_Instrumentation - weblogic.Name sysprop value: {0}",
                System.getProperty("weblogic.Name"));
        AgentBridge.publicApi.setServerInfo("WebLogic", version.getVersions().substring(16,26));
        AgentBridge.publicApi.setInstanceName(System.getProperty("weblogic.Name"));

    }
}
