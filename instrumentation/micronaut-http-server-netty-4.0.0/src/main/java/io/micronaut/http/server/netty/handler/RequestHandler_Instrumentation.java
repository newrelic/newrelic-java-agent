package io.micronaut.http.server.netty.handler;

import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;

@Weave(type = MatchType.Interface, originalName = "io.micronaut.http.server.netty.handler.RequestHandler")
public abstract class RequestHandler_Instrumentation {

    @Trace
    public void accept(ChannelHandlerContext ctx, HttpRequest request, PipeliningServerHandler.OutboundAccess outboundAccess) {
        Weaver.callOriginal();
    }

}
