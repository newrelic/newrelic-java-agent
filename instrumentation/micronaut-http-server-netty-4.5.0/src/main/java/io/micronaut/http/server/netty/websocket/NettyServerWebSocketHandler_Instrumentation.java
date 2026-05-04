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
import io.netty.channel.ChannelHandlerContext;

@Weave(originalName = "io.micronaut.http.server.netty.websocket.NettyServerWebSocketHandler", type = MatchType.ExactClass)
public abstract class NettyServerWebSocketHandler_Instrumentation {

    @Trace(dispatcher = true)
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        Weaver.callOriginal();
    }
}
