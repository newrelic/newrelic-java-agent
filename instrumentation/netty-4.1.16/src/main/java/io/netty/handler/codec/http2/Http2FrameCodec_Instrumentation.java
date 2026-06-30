/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.netty.handler.codec.http2;

import com.agent.instrumentation.netty4116.NettyUtil;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.channel.DefaultChannelPipeline;
import io.netty.channel.NRNettyChannelHandler;

@Weave(type = MatchType.BaseClass, originalName = "io.netty.handler.codec.http2.Http2FrameCodec")
public class Http2FrameCodec_Instrumentation {

    // Handle the incoming request. For HTTP/2 there is no HttpRequest object
    // but rather a stream of Http2Frame objects that make up the full request.
    void onHttp2Frame(ChannelHandlerContext ctx, Http2Frame frame) {
		ChannelPipeline pipeline = ctx.pipeline();
		Token token = ((DefaultChannelPipeline)pipeline).nettyToken;
        if (frame instanceof Http2HeadersFrame && token == null) {
            Http2HeadersFrame msg = (Http2HeadersFrame) frame;
            if (msg.isEndStream()) {
				if (pipeline.get(NRNettyChannelHandler.NR_CHANNEL_HANDLER) == null) {
					String name = ctx.name();
					pipeline.addAfter(name, NRNettyChannelHandler.NR_CHANNEL_HANDLER, new NRNettyChannelHandler());
				}
            }
        }
        // Order matters here!!! Weaver.callOriginal() must come after the call to NettyDispatcher.channelRead.
        Weaver.callOriginal();
    }

    // Handle the outgoing response. For HTTP/2 there is no HttpResponse object
    // but rather a stream of Http2Frame objects that make up the full response.
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        if (msg instanceof Http2HeadersFrame) {
            boolean expired = NettyUtil.processResponse(msg, (DefaultChannelPipeline)ctx.pipeline());
            if (expired) {
            	((DefaultChannelPipeline)ctx.pipeline()).nettyToken = null;
            }
        }
        // Order matters here!!! Weaver.callOriginal() must come after the call to NettyUtil.processResponse.
        Weaver.callOriginal();
    }
}