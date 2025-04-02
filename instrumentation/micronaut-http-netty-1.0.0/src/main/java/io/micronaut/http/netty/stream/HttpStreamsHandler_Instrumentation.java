/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.micronaut.http.netty.stream;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpMessage;

@Weave(originalName = "io.micronaut.http.netty.stream.HttpStreamsHandler", type = MatchType.BaseClass)
abstract class HttpStreamsHandler_Instrumentation<In extends HttpMessage, Out extends HttpMessage> {

    @Trace(dispatcher = true)
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        NewRelic.getAgent().getTracedMethod().setMetricName("Micronaut", "HttpStreamsHandler", getClass().getSimpleName(), "channelRead");
        NewRelic.getAgent().getTracedMethod().addCustomAttribute("Context", ctx.channel().getClass().getName());

        Weaver.callOriginal();
    }

    @Trace(dispatcher = true)
    protected void consumedInMessage(ChannelHandlerContext ctx) {
        NewRelic.getAgent().getTracedMethod().setMetricName("Micronaut", "HttpStreamsHandler", getClass().getSimpleName(), "consumedInMessage");
        NewRelic.getAgent().getTracedMethod().addCustomAttribute("Context", ctx.channel().getClass().getName());

        Weaver.callOriginal();
    }

    @Trace(dispatcher = true)
    protected void receivedInMessage(ChannelHandlerContext ctx) {
        NewRelic.getAgent().getTracedMethod().setMetricName("Micronaut", "HttpStreamsHandler", getClass().getSimpleName(), "receivedInMessage");
        NewRelic.getAgent().getTracedMethod().addCustomAttribute("Context", ctx.channel().getClass().getName());

        Weaver.callOriginal();
    }

    @Trace(dispatcher = true)
    protected void receivedOutMessage(ChannelHandlerContext ctx) {
        NewRelic.getAgent().getTracedMethod().setMetricName("Micronaut", "HttpStreamsHandler", getClass().getSimpleName(), "receivedOutMessage");
        NewRelic.getAgent().getTracedMethod().addCustomAttribute("Context", ctx.channel().getClass().getName());

        Weaver.callOriginal();
    }

    @Trace(dispatcher = true)
    public void write(final ChannelHandlerContext ctx, Object msg, final ChannelPromise promise) {
        NewRelic.getAgent().getTracedMethod().setMetricName("Micronaut", "HttpStreamsHandler", getClass().getSimpleName(), "write");
        NewRelic.getAgent().getTracedMethod().addCustomAttribute("Context", ctx.channel().getClass().getName());

        Weaver.callOriginal();
    }
}
