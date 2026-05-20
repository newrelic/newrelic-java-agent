package io.micronaut.http.server.netty.handler;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

import io.netty.channel.ChannelHandlerContext_Instrumentation;
import io.netty.channel.ChannelPipeline_Instrumentation;
import io.netty.handler.codec.http.HttpRequest;

@Weave(type = MatchType.Interface, originalName = "io.micronaut.http.server.netty.handler.RequestHandler")
public abstract class RequestHandler_Instrumentation {

    @Trace
    public void accept(ChannelHandlerContext_Instrumentation ctx, HttpRequest request, PipeliningServerHandler.OutboundAccess outboundAccess) {
        if(ctx != null && ctx.pipeline() != null) {
            ChannelPipeline_Instrumentation pipeline = ctx.pipeline();
            if(pipeline.micronautToken == null) {
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

}
