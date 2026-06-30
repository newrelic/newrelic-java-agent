package io.netty.channel;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

import io.netty.handler.codec.http.FullHttpResponse;

@Weave(type = MatchType.Interface, originalName = "io.netty.channel.ChannelOutboundHandler")
public abstract class ChannelOutboundHandler_Instrumentation {

	public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
		Weaver.callOriginal();
		if(msg instanceof FullHttpResponse) {
			ChannelPipeline pipeline = ctx.pipeline();
			if (pipeline instanceof DefaultChannelPipeline) {
				DefaultChannelPipeline dPipeline = (DefaultChannelPipeline)pipeline;
				if(dPipeline.nettyToken != null) {
					if(dPipeline.nettyToken.isActive()) {
						dPipeline.nettyToken.expire();
					}
					dPipeline.nettyToken = null;
				}
			}
		}
	}

}
