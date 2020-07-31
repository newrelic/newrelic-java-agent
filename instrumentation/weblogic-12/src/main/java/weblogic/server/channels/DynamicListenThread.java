/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package weblogic.server.channels;

import weblogic.common.internal.Version;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave
public class DynamicListenThread {

    protected int port;

    public boolean start(boolean b1, boolean b2, boolean b3) {
        AgentBridge.publicApi.setServerInfo("WebLogic", Version.VERSION_STRING);
        AgentBridge.publicApi.setAppServerPort(port);
        AgentBridge.publicApi.setInstanceName(System.getProperty("weblogic.Name"));
        return Weaver.callOriginal();
    }
}
