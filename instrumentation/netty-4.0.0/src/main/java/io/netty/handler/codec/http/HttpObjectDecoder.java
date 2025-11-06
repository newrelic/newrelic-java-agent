/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
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

@Weave(type = MatchType.BaseClass)
public class HttpObjectDecoder {

    // heading upstream
    protected void decode(ChannelHandlerContext_Instrumentation ctx, ByteBuf buffer, List<Object> out) {
        Weaver.callOriginal();
        for (Object msg : out) {
            if (msg instanceof HttpRequest && ctx.pipeline().token == null) {
                NettyDispatcher.channelRead(ctx, msg);
            }
        }
    }
}
