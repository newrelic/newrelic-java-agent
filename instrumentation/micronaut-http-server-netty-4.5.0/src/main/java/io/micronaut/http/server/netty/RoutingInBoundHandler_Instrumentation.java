/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.micronaut.http.server.netty;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.Transaction;
import com.newrelic.api.agent.TransportType;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

import com.nr.agent.instrumentation.micronaut.netty_45.NettyExtendedRequest;
import com.nr.agent.instrumentation.micronaut.netty_45.NettyHeaders;
import io.micronaut.http.body.CloseableByteBody;
import io.micronaut.http.server.netty.handler.OutboundAccess;
import io.netty.channel.ChannelHandlerContext_Instrumentation;
import io.netty.channel.ChannelPipeline_Instrumentation;

@Weave(originalName = "io.micronaut.http.server.netty.RoutingInBoundHandler", type = MatchType.ExactClass)
public abstract class RoutingInBoundHandler_Instrumentation {

    @Trace(async = true)
    public void accept(ChannelHandlerContext_Instrumentation ctx, io.netty.handler.codec.http.HttpRequest request, CloseableByteBody body,
                       OutboundAccess outboundAccess) {
        if(ctx != null) {
            ChannelPipeline_Instrumentation pipeline = ctx.pipeline();
            if(pipeline != null) {
                if(pipeline.micronautToken != null) {
                    pipeline.micronautToken.link();
                }
            }
        }
        Transaction transaction = NewRelic.getAgent().getTransaction();
        if(!transaction.isWebTransaction()) {
            transaction.convertToWebTransaction();
        }
        transaction.setWebRequest(new NettyExtendedRequest(request));
        transaction.acceptDistributedTraceHeaders(TransportType.HTTP, new NettyHeaders(request));
        Weaver.callOriginal();
    }
}
