/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.agent.instrumentation.netty34;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.logging.Level;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Token;
import com.newrelic.api.agent.NewRelic;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.util.Version;

public abstract class NettyUtil {

    public static String getNettyVersion() {
        return Version.ID;
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

    public static boolean processResponse(Object msg, Token token) {
        if (token != null) {
            if (msg instanceof DefaultHttpResponse) {
                com.newrelic.api.agent.Transaction tx = token.getTransaction();
                if (tx != null) {
                    try {
                        tx.setWebResponse(new ResponseWrapper((DefaultHttpResponse) msg));
                        tx.addOutboundResponseHeaders();
                        tx.markResponseSent();
                    } catch (Exception e) {
                        AgentBridge.getAgent().getLogger().log(Level.FINER, e, "Unable to set web request on transaction: {0}", tx);
                    }
                }
                token.expire();
                return true;
            }
        }
        return false;
    }

}
