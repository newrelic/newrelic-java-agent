package io.netty.channel;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;

@Weave(type = MatchType.Interface, originalName = "io.netty.channel.ChannelHandlerContext")
public abstract class ChannelHandlerContext_Instrumentation {

    public abstract ChannelPipeline_Instrumentation pipeline();
    
}