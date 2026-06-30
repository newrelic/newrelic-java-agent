/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.agent.instrumentation.netty40;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.logging.Level;

public class NettyUtil {

    public static String getNettyVersion() {
        return "4";
    }

    public static void setAppServerPort(SocketAddress localAddress) {
        if (localAddress instanceof InetSocketAddress) {
            int port = ((InetSocketAddress) localAddress).getPort();
            NewRelic.setAppServerPort(port);
        } else {
            AgentBridge.getAgent().getLogger().log(Level.FINE, "Unable to get Netty port number");
        }
    }

    public static void setServerInfo() {
        AgentBridge.publicApi.setServerInfo("Netty", getNettyVersion());
    }

}
