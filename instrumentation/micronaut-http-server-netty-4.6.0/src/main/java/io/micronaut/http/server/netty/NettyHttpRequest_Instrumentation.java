package io.micronaut.http.server.netty;

import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import io.netty.channel.ChannelHandlerContext_Instrumentation;

@Weave(originalName = "io.micronaut.http.server.netty.NettyHttpRequest")
public class NettyHttpRequest_Instrumentation<T> {

    public ChannelHandlerContext_Instrumentation getChannelHandlerContext() {
        return Weaver.callOriginal();
    }
}
