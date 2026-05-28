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
import io.netty.channel.*;

@Weave(type = MatchType.BaseClass, originalName = "io.netty.handler.codec.http2.Http2FrameCodec")
public class Http2FrameCodec_Instrumentation {

    // Handle the incoming request. For HTTP/2 there is no HttpRequest object
    // but rather a stream of Http2Frame objects that make up the full request.
    void onHttp2Frame(ChannelHandlerContext ctx, Http2Frame frame) {
        ChannelPipeline pipeline = ctx.pipeline();
        Token token = pipeline instanceof DefaultChannelPipeline_Instrumentation ? ((DefaultChannelPipeline_Instrumentation)pipeline).nettyToken : null;
        if (frame instanceof Http2HeadersFrame && token == null) {
            Http2HeadersFrame msg = (Http2HeadersFrame) frame;
            if (msg.isEndStream()) {
                ChannelHandler channelHandler = pipeline.get(NRNettyChannelHandler.NR_CHANNEL_HANDLER);

                if (channelHandler == null) {
                    NRNettyChannelHandler nrNettyChannelHandler = new NRNettyChannelHandler();
                    nrNettyChannelHandler.channelRead(ctx, frame);
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
            boolean expired = NettyUtil.processResponse(msg, (DefaultChannelPipeline_Instrumentation) ctx.pipeline());
            if (expired) {
                ((DefaultChannelPipeline_Instrumentation)ctx.pipeline()).nettyToken = null;
            }
        }
        // Order matters here!!! Weaver.callOriginal() must come after the call to NettyUtil.processResponse.
        Weaver.callOriginal();
    }
}