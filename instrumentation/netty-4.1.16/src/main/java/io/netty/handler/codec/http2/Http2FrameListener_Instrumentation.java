package io.netty.handler.codec.http2;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import io.netty.bootstrap.NettyDispatcher;
import io.netty.channel.ChannelHandlerContext_Instrumentation;

// TODO delete this class, just doing logging
@Weave(type = MatchType.Interface, originalName = "io.netty.handler.codec.http2.Http2FrameListener")
public class Http2FrameListener_Instrumentation {

    public void onHeadersRead(ChannelHandlerContext_Instrumentation ctx, int streamId, Http2Headers headers, int padding, boolean endOfStream) {
        NettyDispatcher.debug(ctx.pipeline().token, headers, this.getClass().getName());

//        if (ctx.pipeline().token == null) {
//            // NettyDispatcher class is usually initialized in AbstractBootstrap; however,
//            // that code is not always invoked when using recent Netty versions (4.1.54)
//            // so we check here and initialize if we haven't yet.
//            if (!NettyDispatcher.instrumented.get()) {
//                NettyDispatcher.get();
//            }
//            NettyDispatcher.channelRead(ctx, headers);
//        }

        // Order matters here!!! Weaver.callOriginal() must come after the call to NettyDispatcher.channelRead.
        Weaver.callOriginal();
    }

    public void onHeadersRead(ChannelHandlerContext_Instrumentation ctx, int streamId, Http2Headers headers, int streamDependency, short weight,
            boolean exclusive, int padding, boolean endOfStream) {
        NettyDispatcher.debug(ctx.pipeline().token, headers, this.getClass().getName());

//        if (ctx.pipeline().token == null) {
//            // NettyDispatcher class is usually initialized in AbstractBootstrap; however,
//            // that code is not always invoked when using recent Netty versions (4.1.54)
//            // so we check here and initialize if we haven't yet.
//            if (!NettyDispatcher.instrumented.get()) {
//                NettyDispatcher.get();
//            }
//            NettyDispatcher.channelRead(ctx, headers);
//        }

        // Order matters here!!! Weaver.callOriginal() must come after the call to NettyDispatcher.channelRead.
        Weaver.callOriginal();
    }
}
