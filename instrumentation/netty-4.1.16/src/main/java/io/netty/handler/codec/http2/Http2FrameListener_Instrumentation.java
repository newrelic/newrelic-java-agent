package io.netty.handler.codec.http2;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import io.netty.bootstrap.NettyDispatcher;
import io.netty.channel.ChannelHandlerContext_Instrumentation;

@Weave(type = MatchType.Interface, originalName = "io.netty.handler.codec.http2.Http2FrameListener")
public class Http2FrameListener_Instrumentation {

    public void onHeadersRead(ChannelHandlerContext_Instrumentation ctx, int streamId, Http2Headers headers, int padding, boolean endOfStream) {
        NettyDispatcher.debug(ctx.pipeline().token, headers, this.getClass().getName());
        Weaver.callOriginal();
    }

    public void onHeadersRead(ChannelHandlerContext_Instrumentation ctx, int streamId, Http2Headers headers, int streamDependency, short weight,
            boolean exclusive, int padding, boolean endOfStream) {
        NettyDispatcher.debug(ctx.pipeline().token, headers, this.getClass().getName());
        Weaver.callOriginal();
    }
}
