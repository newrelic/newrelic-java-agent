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
import io.netty.handler.codec.http2.Http2Headers;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.logging.Level;

public class NettyUtil {

    // This config is added so customers can also start netty transactions at a lower level.
    // However, this risks netty producing transactions with 'unknown' urls.
    // Only use it if it provides the coverage you need for your application's use case.
    public static final Boolean START_HTTP2_FRAME_READ_LISTENER_TXN =
            NewRelic.getAgent().getConfig().getValue("netty.http2.frame_read_listener.start_transaction", false);

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

    /*
     * processResponse is invoked when a Netty response is encoded (see weave classes in
     * io.netty.handler.codec package). This is where the token is stored in the Netty
     * context pipeline is expired and the response is processed.
     */
    public static boolean processResponse(Object msg, Token token) {
        NewRelic.getAgent().getLogger().log(Level.INFO, "Netty Debug: in processResponse with msg: {0} and token: {1}", msg, token);
        if (token != null) {
            if (msg instanceof HttpResponse || msg instanceof Http2Headers) {
                com.newrelic.api.agent.Transaction tx = token.getTransaction();
                NewRelic.getAgent().getLogger().log(Level.INFO, "Netty Debug: in processResponse. Transaction for token {0} was {1}", token, tx);
                if (tx != null) {
                    try {
                        if (msg instanceof HttpResponse) {
                            // HTTP/1 response
                            NewRelic.getAgent().getLogger().log(Level.INFO, "Netty Debug: in processResponse. Setting Http1 Web Response.");
                            tx.setWebResponse(new ResponseWrapper((HttpResponse) msg));
                        } else {
                            // HTTP/2 response
                            NewRelic.getAgent().getLogger().log(Level.INFO, "Netty Debug: in processResponse. Setting Http2 Web Response.");
                            tx.setWebResponse(new Http2ResponseHeaderWrapper((Http2Headers) msg));
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
