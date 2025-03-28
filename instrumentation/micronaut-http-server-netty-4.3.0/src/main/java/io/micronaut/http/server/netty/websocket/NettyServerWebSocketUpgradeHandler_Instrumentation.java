/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.micronaut.http.server.netty.websocket;

import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import io.micronaut.http.server.netty.body.ByteBody;
import io.micronaut.http.server.netty.handler.PipeliningServerHandler_Instrumentation;
import io.netty.channel.ChannelHandlerContext;

@Weave(originalName = "io.micronaut.http.server.netty.websocket.NettyServerWebSocketUpgradeHandler", type = MatchType.ExactClass)
public abstract class NettyServerWebSocketUpgradeHandler_Instrumentation {

    @Trace(dispatcher = true)
    public void accept(ChannelHandlerContext ctx, io.netty.handler.codec.http.HttpRequest request, ByteBody body,
            PipeliningServerHandler_Instrumentation.OutboundAccess_Instrumentation outboundAccess) {
        Weaver.callOriginal();
    }

}
