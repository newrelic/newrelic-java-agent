/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.jboss.netty.handler.codec.http;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;

import com.agent.instrumentation.netty38.NettyUtil;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import org.jboss.netty.channel.ChannelHandlerContext_Instrumentation;

@Weave(type = MatchType.BaseClass)
public class HttpMessageEncoder {
    @Trace
    protected Object encode(ChannelHandlerContext_Instrumentation ctx, Channel channel, Object msg) {
        boolean expired = NettyUtil.processResponse(msg, ctx.getPipeline().token);
        if (expired) {
            ctx.getPipeline().token = null;
        }
        return Weaver.callOriginal();
    }

}
