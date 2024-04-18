/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.agent.instrumentation.netty4116;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Token;
import com.newrelic.api.agent.NewRelic;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http2.Http2HeadersFrame;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.logging.Level;

public class NettyUtil {

    public static String getNettyVersion() {
        return "4.1.16";
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

        NewRelic.getAgent().getLogger().log(Level.INFO, "[NettyDebug][1] NettyUtil.processResponse: (token != null) = " + (token != null));

        if (token != null) {

            NewRelic.getAgent().getLogger().log(Level.INFO, "[NettyDebug][2] NettyUtil.processResponse: msg = " + msg);

            NewRelic.getAgent().getLogger().log(Level.INFO, "[NettyDebug][3] NettyUtil.processResponse: (msg instanceof Http2HeadersFrame) = " + (msg instanceof Http2HeadersFrame));

            if (msg instanceof HttpResponse || msg instanceof Http2HeadersFrame) {
                com.newrelic.api.agent.Transaction tx = token.getTransaction();

                NewRelic.getAgent().getLogger().log(Level.INFO, "[NettyDebug][4] NettyUtil.processResponse: (tx != null) = " + (tx != null));

                if (tx != null) {
                    try {
                        if (msg instanceof HttpResponse) {
                            tx.setWebResponse(new ResponseWrapper((HttpResponse) msg));
                        } else {
                            tx.setWebResponse(new Http2ResponseWrapper((Http2HeadersFrame) msg));

                            NewRelic.getAgent().getLogger().log(Level.INFO, "[NettyDebug][5] NettyUtil.processResponse: called tx.setWebResponse(new Http2ResponseWrapper((Http2HeadersFrame) msg))");
                        }
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
