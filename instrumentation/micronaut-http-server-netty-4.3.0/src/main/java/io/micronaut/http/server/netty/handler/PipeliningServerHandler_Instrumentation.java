/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.micronaut.http.server.netty.handler;

import com.newrelic.api.agent.*;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.micronaut.netty_43.NettyExtendedRequest;
import com.nr.agent.instrumentation.micronaut.netty_43.NettyHeaders;
import io.netty.channel.ChannelHandlerContext_Instrumentation;
import io.netty.channel.ChannelPipeline_Instrumentation;
import io.netty.handler.codec.http.HttpRequest;

@Weave(type = MatchType.ExactClass, originalName = "io.micronaut.http.server.netty.handler.PipeliningServerHandler")
public abstract class PipeliningServerHandler_Instrumentation {

    @Trace
    public void channelRead(ChannelHandlerContext_Instrumentation ctx, Object msg) {
        ChannelPipeline_Instrumentation pipeline = ctx != null ? ctx.pipeline() : null;
        if(pipeline != null) {
            if(pipeline.micronautToken != null) {
                pipeline.micronautToken.link();
            } else {
                Token token = NewRelic.getAgent().getTransaction().getToken();
                if(token != null) {
                    if(token.isActive()) {
                        pipeline.micronautToken = token;
                    } else {
                        token.expire();
                        token = null;
                    }
                }
            }
        }
        Weaver.callOriginal();
    }

    public void channelReadComplete(ChannelHandlerContext_Instrumentation ctx) {
        ChannelPipeline_Instrumentation pipeline = ctx != null ? ctx.pipeline() : null;
        if(pipeline != null) {
            if(pipeline.micronautToken != null) {
                pipeline.micronautToken.expire();
                pipeline.micronautToken = null;
            }
        }
        Weaver.callOriginal();
    }

    @Weave(type = MatchType.ExactClass, originalName = "io.micronaut.http.server.netty.handler.PipeliningServerHandler$MessageInboundHandler")
    private static class MessageInboundHandler_Instrumentation {

        @Trace(dispatcher = true)
        void read(Object message) {
            NewRelic.getAgent().getTracedMethod().setMetricName("Micronaut", "HTTP", "Netty", "InboundHandler", "MessageInboundHandler", "read");
            if(message instanceof HttpRequest) {
                HttpRequest httpRequest = (HttpRequest) message;
                NettyExtendedRequest  nettyExtendedRequest = new NettyExtendedRequest(httpRequest);
                NettyHeaders nettyHeaders = new NettyHeaders(httpRequest);
                Transaction transaction = NewRelic.getAgent().getTransaction();
                transaction.acceptDistributedTraceHeaders(TransportType.HTTP, nettyHeaders);
                transaction.setWebRequest(nettyExtendedRequest);
                transaction.convertToWebTransaction();
            }

            Weaver.callOriginal();
        }

    }

    @Weave(type = MatchType.ExactClass, originalName = "io.micronaut.http.server.netty.handler.PipeliningServerHandler$OptimisticBufferingInboundHandler")
    private static class OptimisticBufferingInboundHandler_Instrumentation {

        @Trace
        void read(Object message) {
            NewRelic.getAgent()
                    .getTracedMethod()
                    .setMetricName("Micronaut", "HTTP", "Netty", "InboundHander", "OptimisticBufferingInboundHandler", "read");
            Weaver.callOriginal();
        }

    }

    @Weave(type = MatchType.ExactClass, originalName = "io.micronaut.http.server.netty.handler.PipeliningServerHandler$DroppingInboundHandler")
    private static class DroppingInboundHandler_Instrumentation {

        @Trace
        void read(Object message) {
            NewRelic.getAgent().getTracedMethod().setMetricName("Micronaut", "HTTP", "Netty", "InboundHander", "DroppingInboundHandler", "read");
            Weaver.callOriginal();
        }

    }

    @Weave(type = MatchType.ExactClass, originalName = "io.micronaut.http.server.netty.handler.PipeliningServerHandler$StreamingInboundHandler")
    private static class StreamingInboundHandler_Instrumentation {

        @Trace
        void read(Object message) {
            NewRelic.getAgent().getTracedMethod().setMetricName("Micronaut", "HTTP", "Netty", "InboundHander", "StreamingInboundHandler", "read");
            Weaver.callOriginal();
        }

    }

    @Weave(type = MatchType.ExactClass, originalName = "io.micronaut.http.server.netty.handler.PipeliningServerHandler$ContinueOutboundHandler")
    private static class ContinueOutboundHandler_Instrumentation {

        @Trace
        void writeSome() {
            NewRelic.getAgent()
                    .getTracedMethod()
                    .setMetricName("Micronaut", "HTTP", "Netty", "OutboundHander", "ContinueOutboundHandler", "writeSome");
            Weaver.callOriginal();
        }
    }

    @Weave(type = MatchType.ExactClass, originalName = "io.micronaut.http.server.netty.handler.PipeliningServerHandler$FullOutboundHandler")
    private static class FullOutboundHandler_Instrumentation {

        @Trace
        void writeSome() {
            NewRelic.getAgent().getTracedMethod().setMetricName("Micronaut", "HTTP", "Netty", "OutboundHander", "FullOutboundHandler", "writeSome");
            Weaver.callOriginal();
        }
    }

    @Weave(type = MatchType.ExactClass, originalName = "io.micronaut.http.server.netty.handler.PipeliningServerHandler$StreamingOutboundHandler")
    private static class StreamingOutboundHandler_Instrumentation {

        @Trace
        void writeSome() {
            NewRelic.getAgent()
                    .getTracedMethod()
                    .setMetricName("Micronaut", "HTTP", "Netty", "OutboundHander", "StreamingOutboundHandler", "writeSome");
            Weaver.callOriginal();
        }
    }

    @Weave(type = MatchType.ExactClass, originalName = "io.micronaut.http.server.netty.handler.PipeliningServerHandler$OutboundAccess")
    public static class OutboundAccess_Instrumentation {

    }

}
