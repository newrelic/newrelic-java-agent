/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.micronaut.http.server.netty.handler;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import io.netty.channel.ChannelHandlerContext;

@Weave(originalName = "io.micronaut.http.server.netty.handler.PipeliningServerHandler", type = MatchType.ExactClass)
public abstract class PipeliningServerHandler_Instrumentation {

    @Trace(dispatcher = true)
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        Weaver.callOriginal();
    }

    @Weave(type = MatchType.ExactClass, originalName = "io.micronaut.http.server.netty.handler.PipeliningServerHandler$MessageInboundHandler")

    private static class MessageInboundHandler_Instrumentation {

        @Trace
        void read(Object message) {
            NewRelic.getAgent().getTracedMethod().setMetricName("Micronaut", "HTTP", "Netty", "InboundHander", "MessageInboundHandler", "read");
            Weaver.callOriginal();
        }

    }

    @Weave(type = MatchType.ExactClass, originalName = "io.micronaut.http.server.netty.handler.PipeliningServerHandler$DecompressingInboundHandler")

    private static class DecompressingInboundHandler_Instrumentation {

        @Trace
        void read(Object message) {
            NewRelic.getAgent().getTracedMethod().setMetricName("Micronaut", "HTTP", "Netty", "InboundHander", "DecompressingInboundHandler", "read");
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

    @Weave(type = MatchType.ExactClass, originalName = "io.micronaut.http.server.netty.handler.PipeliningServerHandler$BlockingOutboundHandler")

    private static class BlockingOutboundHandler_Instrumentation {

        @Trace
        void writeSome() {
            NewRelic.getAgent()
                    .getTracedMethod()
                    .setMetricName("Micronaut", "HTTP", "Netty", "OutboundHander", "BlockingOutboundHandler", "writeSome");
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
