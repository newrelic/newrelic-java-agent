/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package weblogic.server;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.weaver.Weave;

import weblogic.management.configuration.ServerTemplateMBean;
import weblogic.version;

@Weave(originalName = "weblogic.server.ServerLifeCycleRuntime")
public final class ServerLifeCycleRuntime_Instrumentation {
    ServerLifeCycleRuntime_Instrumentation(final ServerTemplateMBean serverMBean) {
        AgentBridge.publicApi.setServerInfo("WebLogic", version.getVersions().substring(16,26));
        AgentBridge.publicApi.setInstanceName(System.getProperty("weblogic.Name"));

    }
}
