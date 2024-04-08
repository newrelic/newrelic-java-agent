/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.netty.handler.codec.http;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import io.netty.bootstrap.NettyDispatcher;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext_Instrumentation;

import java.util.List;

@Weave(type = MatchType.BaseClass, originalName = "io.netty.handler.codec.http.HttpObjectDecoder")
public class HttpObjectDecoder_Instrumentation {

    // heading upstream
    protected void decode(ChannelHandlerContext_Instrumentation ctx, ByteBuf buffer, List<Object> out) {
        Weaver.callOriginal();
        for (Object msg : out) {
            if (msg instanceof HttpRequest && ctx.pipeline().token == null) {
                // NettyDispatcher class is usually initialized in AbstractBootstrap; however,
                // that code is not always invoked when using recent Netty versions (4.1.54)
                // so we check here and initialize if we haven't yet.
                if (!NettyDispatcher.instrumented.get()) {
                    NettyDispatcher.get();
                }
                NettyDispatcher.channelRead(ctx, msg);
            }
        }
    }
}
