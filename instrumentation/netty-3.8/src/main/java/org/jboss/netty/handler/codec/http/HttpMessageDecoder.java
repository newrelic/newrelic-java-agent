/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.jboss.netty.handler.codec.http;

import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import org.jboss.netty.bootstrap.NettyDispatcher;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext_Instrumentation;

@Weave(type = MatchType.BaseClass)
public class HttpMessageDecoder {
    @Trace
    protected Object decode(ChannelHandlerContext_Instrumentation ctx, Channel channel, ChannelBuffer buffer, State state) {
        Object request = Weaver.callOriginal();
        if (request instanceof HttpRequest && ctx.getPipeline().token == null) {
            NettyDispatcher.upstreamDispatcher(ctx, request);
        }
        return request;
    }

    @Weave
    protected static class State {
    }
}
