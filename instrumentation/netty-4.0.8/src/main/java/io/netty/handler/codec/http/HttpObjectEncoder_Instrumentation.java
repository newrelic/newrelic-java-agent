/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.netty.handler.codec.http;

import java.util.List;

import com.agent.instrumentation.netty40.NettyUtil;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.DefaultChannelPipeline_Instrumentation;

@Weave(type = MatchType.BaseClass, originalName = "io.netty.handler.codec.http.HttpObjectEncoder")
public class HttpObjectEncoder_Instrumentation {

    // heading downstream
    protected void encode(ChannelHandlerContext ctx, Object msg, List<Object> out) {
    	
    	ChannelPipeline pipeline = ctx.pipeline();
    	
        boolean expired = NettyUtil.processResponse(msg, (DefaultChannelPipeline_Instrumentation)pipeline);
        if (expired) {
        	((DefaultChannelPipeline_Instrumentation)pipeline).nettyToken = null;
        }
        Weaver.callOriginal();
    }

}
