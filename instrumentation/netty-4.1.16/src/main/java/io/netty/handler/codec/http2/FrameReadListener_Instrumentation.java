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

@Weave(type = MatchType.ExactClass, originalName = "io.netty.handler.codec.http2.DefaultHttp2ConnectionDecoder$FrameReadListener")
class FrameReadListener_Instrumentation {

    // Process HTTP/2 request headers and start txn
    public void onHeadersRead(ChannelHandlerContext_Instrumentation ctx, int streamId, Http2Headers headers, int streamDependency, short weight,
            boolean exclusive, int padding, boolean endOfStream) {
        if (NettyUtil.START_HTTP2_FRAME_CODEC_TXN && ctx.pipeline().token == null) {
            // NettyDispatcher class is usually initialized in AbstractBootstrap; however,
            // that code is not always invoked when using recent Netty versions (4.1.54)
            // so we check here and initialize if we haven't yet.
            if (!NettyDispatcher.instrumented.get()) {
                NettyDispatcher.get();
            }
            NettyDispatcher.channelRead(ctx, headers);
        }
        // Order matters here!!! Weaver.callOriginal() must come after the call to NettyDispatcher.channelRead.
        Weaver.callOriginal();
    }
}
