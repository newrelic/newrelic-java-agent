package io.netty.handler.codec.http2;

import com.agent.instrumentation.netty4116.NettyUtil;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import io.netty.bootstrap.NettyDispatcher;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext_Instrumentation;
import io.netty.channel.ChannelPromise;

// TODO delete this class, just doing logging
@Weave(type = MatchType.Interface, originalName = "io.netty.handler.codec.http2.Http2FrameWriter")
public class Http2FrameWriter_Instrumentation {

    // Process HTTP/2 response headers and end txn
    public ChannelFuture writeHeaders(ChannelHandlerContext_Instrumentation ctx, int streamId, Http2Headers headers,
            int padding, boolean endStream, ChannelPromise promise) {

        NettyDispatcher.debug2(headers, this.getClass().getName(), "writeHeaders");

        boolean expired = NettyUtil.processResponse(headers, ctx.pipeline().token);
        if (expired) {
            ctx.pipeline().token = null;
        }

        return Weaver.callOriginal();
    }

    // Process HTTP/2 response headers and end txn
    public ChannelFuture writeHeaders(ChannelHandlerContext_Instrumentation ctx, int streamId, Http2Headers headers,
            int streamDependency, short weight, boolean exclusive, int padding, boolean endStream,
            ChannelPromise promise) {

        NettyDispatcher.debug2(headers, this.getClass().getName(), "writeHeaders");

        boolean expired = NettyUtil.processResponse(headers, ctx.pipeline().token);
        if (expired) {
            ctx.pipeline().token = null;
        }

        return Weaver.callOriginal();
    }
}
