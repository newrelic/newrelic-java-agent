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

@Weave(type = MatchType.ExactClass, originalName = "io.netty.handler.codec.http2.DefaultHttp2ConnectionDecoder$FrameReadListener")
class FrameReadListener_Instrumentation {

    // Process HTTP/2 request headers and start txn
    public void onHeadersRead(ChannelHandlerContext ctx, int streamId, Http2Headers headers, int streamDependency, short weight,
                              boolean exclusive, int padding, boolean endOfStream) {
        ChannelPipeline pipeline = ctx.pipeline();
        Token token = pipeline instanceof DefaultChannelPipeline_Instrumentation ? ((DefaultChannelPipeline_Instrumentation)pipeline).nettyToken : null;

        if (NettyUtil.START_HTTP2_FRAME_READ_LISTENER_TXN && token == null) {
            ChannelHandler channelHandler = pipeline.get(NRNettyChannelHandler.NR_CHANNEL_HANDLER);

            if (channelHandler == null) {
                NRNettyChannelHandler nrNettyChannelHandler = new NRNettyChannelHandler();
                nrNettyChannelHandler.channelRead(ctx, headers);
            }
        }
        // Order matters here!!! Weaver.callOriginal() must come after the call to NettyDispatcher.channelRead.
        Weaver.callOriginal();
    }
}
