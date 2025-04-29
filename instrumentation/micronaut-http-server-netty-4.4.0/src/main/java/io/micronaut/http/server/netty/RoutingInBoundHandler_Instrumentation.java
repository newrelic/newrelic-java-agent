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
import io.micronaut.http.server.netty.body.ByteBody;
import io.micronaut.http.server.netty.handler.OutboundAccess;
import io.netty.channel.ChannelHandlerContext;

@Weave(originalName = "io.micronaut.http.server.netty.RoutingInBoundHandler", type = MatchType.ExactClass)

public abstract class RoutingInBoundHandler_Instrumentation {

    @Trace(dispatcher = true)
    public void accept(ChannelHandlerContext ctx, io.netty.handler.codec.http.HttpRequest request, ByteBody body,
            OutboundAccess outboundAccess) {
        Weaver.callOriginal();
    }
}
