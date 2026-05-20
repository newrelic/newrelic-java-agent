/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.micronaut.http.server.netty;

import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

import io.netty.channel.ChannelHandlerContext;

@Weave(originalName = "io.micronaut.http.server.netty.RoutingInBoundHandler", type = MatchType.ExactClass)
abstract class RoutingInBoundHandler_Instrumentation {

    @Trace(dispatcher = true)
    protected void channelRead0(ChannelHandlerContext ctx, io.micronaut.http.HttpRequest<?> request) {
        Weaver.callOriginal();
    }

}
