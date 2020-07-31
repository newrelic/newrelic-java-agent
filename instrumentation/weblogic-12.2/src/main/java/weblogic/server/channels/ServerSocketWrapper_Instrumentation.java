/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package weblogic.server.channels;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.weaver.Weave;

import weblogic.protocol.ServerChannel;

@Weave(originalName = "weblogic.server.channels.ServerSocketWrapper")
public class ServerSocketWrapper_Instrumentation
{
    protected int port;

    ServerSocketWrapper_Instrumentation(final ServerChannel[] channels) {
        AgentBridge.publicApi.setAppServerPort(port);
    }
}
