/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */


package io.micronaut.http.netty.websocket;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import io.micronaut.core.bind.BoundExecutable;
import io.micronaut.inject.MethodExecutionHandle;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;

@Weave(originalName = "io.micronaut.http.netty.websocket.AbstractNettyWebSocketHandler", type = MatchType.BaseClass)
public abstract class AbstractNettyWebSocketHandler_Instrumentation {

    @Trace(dispatcher = true)
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
        NewRelic.getAgent().getTracedMethod().setMetricName("Micronaut", "NettyWebSocketHandler", getClass().getSimpleName(), "channelRead0");
        NewRelic.getAgent().getTracedMethod().addCustomAttribute("Context", ctx.channel().getClass().getName());

        Weaver.callOriginal();
    }

    @Trace(dispatcher = true)
    protected void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame msg) {
        NewRelic.getAgent().getTracedMethod().setMetricName("Micronaut", "NettyWebSocketHandler", getClass().getSimpleName(), "handleWebSocketFrame");
        NewRelic.getAgent().getTracedMethod().addCustomAttribute("Context", ctx.channel().getClass().getName());

        Weaver.callOriginal();
    }

    @SuppressWarnings("rawtypes")
    @Trace(dispatcher = true)
    protected Object invokeExecutable(BoundExecutable boundExecutable, MethodExecutionHandle<?, ?> messageHandler) {
        NewRelic.getAgent().getTracedMethod().setMetricName("Micronaut", "NettyWebSocketHandler", getClass().getSimpleName(), "invokeExecutable");

        return Weaver.callOriginal();
    }
}
