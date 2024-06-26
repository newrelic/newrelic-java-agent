//package io.netty.handler.codec.http2;
//
//import com.newrelic.agent.bridge.Token;
//import com.newrelic.api.agent.weaver.MatchType;
//import com.newrelic.api.agent.weaver.Weave;
//import com.newrelic.api.agent.weaver.Weaver;
//import io.netty.bootstrap.NettyDispatcher;
//import io.netty.channel.ChannelHandlerContext_Instrumentation;
//
//@Weave(type = MatchType.ExactClass, originalName = "io.netty.handler.codec.http2.DefaultHttp2ConnectionDecoder$FrameReadListener")
//class FrameReadListener_Instrumentation {
//    public void onHeadersRead(ChannelHandlerContext_Instrumentation ctx, int streamId, Http2Headers headers, int streamDependency,
//            short weight, boolean exclusive, int padding, boolean endOfStream) {
//
//        String method = headers.get(":method") != null ? headers.get(":method").toString() : null;
////        if (method != null) {
////            System.out.println("[1-1 onHeadersRead] method in headers: " + headers.names().toString());
////        } else {
////            System.out.println("[1-2 onHeadersRead] No method in headers: " + headers.names().toString());
////        }
//        Token token = ctx.pipeline().token;
//        NettyDispatcher.debug(token);
//        if (ctx.pipeline().token == null) {
//            // NettyDispatcher class is usually initialized in AbstractBootstrap; however,
//            // that code is not always invoked when using recent Netty versions (4.1.54)
//            // so we check here and initialize if we haven't yet.
//            if (!NettyDispatcher.instrumented.get()) {
//                NettyDispatcher.get();
//            }
//            NettyDispatcher.channelRead(ctx, headers);
//        }
//        Weaver.callOriginal();
//    }
//}
//
