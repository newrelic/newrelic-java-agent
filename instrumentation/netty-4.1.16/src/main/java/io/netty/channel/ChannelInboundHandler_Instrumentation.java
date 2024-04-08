/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.netty.channel;

import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(type = MatchType.Interface, originalName = "io.netty.channel.ChannelInboundHandler")
public abstract class ChannelInboundHandler_Instrumentation {

    @Trace(async = true, excludeFromTransactionTrace = true)
    public void channelRead(ChannelHandlerContext_Instrumentation ctx, Object msg) throws Exception {
        if (ctx.pipeline().token != null) {
            ctx.pipeline().token.link();
        }
        Weaver.callOriginal();
    }
}
