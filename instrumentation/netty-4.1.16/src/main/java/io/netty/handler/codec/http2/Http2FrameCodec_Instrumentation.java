/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.netty.handler.codec.http2;

import com.agent.instrumentation.netty4116.NettyUtil;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import io.netty.bootstrap.NettyDispatcher;
import io.netty.channel.ChannelHandlerContext_Instrumentation;
import io.netty.channel.ChannelPromise;

@Weave(type = MatchType.BaseClass, originalName = "io.netty.handler.codec.http2.Http2FrameCodec")
public class Http2FrameCodec_Instrumentation {

    // Handle the incoming request. For HTTP/2 there is no HttpRequest object
    // but rather a stream of Http2Frame objects that make up the full request.
    void onHttp2Frame(ChannelHandlerContext_Instrumentation ctx, Http2Frame frame) {
        if (!NettyUtil.START_HTTP2_FRAME_READ_LISTENER_TXN && frame instanceof Http2HeadersFrame && ctx.pipeline().token == null) {
            Http2HeadersFrame msg = (Http2HeadersFrame) frame;
            if (msg.isEndStream()) {
                // NettyDispatcher class is usually initialized in AbstractBootstrap; however,
                // that code is not always invoked when using recent Netty versions (4.1.54)
                // so we check here and initialize if we haven't yet.
                if (!NettyDispatcher.instrumented.get()) {
                    NettyDispatcher.get();
                }
                NettyDispatcher.channelRead(ctx, msg.headers());
            }
        }
        // Order matters here!!! Weaver.callOriginal() must come after the call to NettyDispatcher.channelRead.
        Weaver.callOriginal();
    }

    // Handle the outgoing response. For HTTP/2 there is no HttpResponse object
    // but rather a stream of Http2Frame objects that make up the full response.
    public void write(ChannelHandlerContext_Instrumentation ctx, Object msg, ChannelPromise promise) {
        if (msg instanceof Http2HeadersFrame) {
            boolean expired = NettyUtil.processResponse(msg, ctx.pipeline().token);
            if (expired) {
                ctx.pipeline().token = null;
            }
        }
        // Order matters here!!! Weaver.callOriginal() must come after the call to NettyUtil.processResponse.
        Weaver.callOriginal();
    }
}