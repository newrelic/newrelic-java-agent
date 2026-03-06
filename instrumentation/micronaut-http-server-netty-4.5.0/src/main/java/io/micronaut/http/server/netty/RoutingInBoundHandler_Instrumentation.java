/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.micronaut.http.server.netty;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.TransactionNamePriority;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.body.CloseableByteBody;
import io.micronaut.http.server.netty.handler.OutboundAccess;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelHandlerContext_Instrumentation;
import io.netty.channel.ChannelPipeline_Instrumentation;
import io.netty.handler.codec.http.HttpMethod;

@Weave(type = MatchType.ExactClass, originalName = "io.micronaut.http.server.netty.RoutingInBoundHandler")
public abstract class RoutingInBoundHandler_Instrumentation {

    @Trace(async = true)
    public void accept(ChannelHandlerContext_Instrumentation ctx, io.netty.handler.codec.http.HttpRequest request, CloseableByteBody body, OutboundAccess outboundAccess) {
        ChannelPipeline_Instrumentation pipeline = ctx.pipeline();
        if(pipeline != null) {
            if(pipeline.micronautToken == null) {
                Token token = NewRelic.getAgent().getTransaction().getToken();
                if (token != null) {
                    if (!token.isActive()) {
                        token.expire();
                    } else {
                        pipeline.micronautToken = token;
                    }
                }
            } else {
                pipeline.micronautToken.link();
            }
        }
        if(request != null) {
            StringBuffer sb = new StringBuffer();
            HttpMethod method = request.method();
            if(method != null) {
                sb.append(method.name());
            } else {
                sb.append("UnknownMethod");
            }
            sb.append(" - ");
            String uri = request.uri();
            if(uri != null) {
                sb.append(uri);
            } else {
                sb.append("UnknownURI");
            }
            NewRelic.getAgent().getTransaction().setTransactionName(TransactionNamePriority.FRAMEWORK_LOW, false, "Micronaut-Netty", sb.toString());
        }
        Weaver.callOriginal();
    }

    @Trace(async = true)
    public void writeResponse(OutboundAccess outboundAccess,
                              NettyHttpRequest_Instrumentation<?> nettyHttpRequest,
                              HttpResponse<?> response,
                              Throwable throwable) {
        if(throwable != null) {
            NewRelic.noticeError(throwable);
        }
        if(nettyHttpRequest != null) {
            ChannelHandlerContext_Instrumentation ctx = nettyHttpRequest.getChannelHandlerContext();
            if(ctx != null) {
                ChannelPipeline_Instrumentation pipeline = ctx.pipeline();
                if(pipeline != null) {
                    if(pipeline.micronautToken != null) {
                        pipeline.micronautToken.linkAndExpire();
                        pipeline.micronautToken = null;
                    }
                }

            }
        }
        Weaver.callOriginal();
    }
}
