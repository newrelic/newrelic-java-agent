/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.netty.handler.codec.http;

import java.util.List;

import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

import io.netty.buffer.ByteBuf;
import io.netty.channel.*;

@Weave(type = MatchType.BaseClass, originalName = "io.netty.handler.codec.http.HttpObjectDecoder")
public class HttpObjectDecoder_Instrumentation {

    // heading upstream
    protected void decode(ChannelHandlerContext ctx, ByteBuf buffer, List<Object> out) {
        Weaver.callOriginal();
		ChannelPipeline pipeline = ctx.pipeline();
		Token token = NRNettyChannelUtils.getToken(pipeline);
		for (Object msg : out) {
			if (msg instanceof HttpRequest && token == null) {
				if (pipeline.get(NRNettyChannelHandler.NR_CHANNEL_HANDLER) == null) {
					String name = ctx.name();
					pipeline.addAfter(name, NRNettyChannelHandler.NR_CHANNEL_HANDLER, new NRNettyChannelHandler());
				}
			}
		}
    }
}
