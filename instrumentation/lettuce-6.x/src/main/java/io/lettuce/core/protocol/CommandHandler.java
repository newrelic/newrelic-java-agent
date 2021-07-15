package io.lettuce.core.protocol;

import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

import io.netty.channel.ChannelHandlerContext_Instrumentation;
//import io.netty.channel.ChannelHandlerContext_Instrumentation;
import io.netty.channel.ChannelPromise;

@Weave
public abstract class CommandHandler {

	@Trace(async=true)
	public void channelRead(ChannelHandlerContext_Instrumentation ctx, Object msg) {
		if(ctx.pipeline().lettuceLayerToken != null) {
			ctx.pipeline().lettuceLayerToken.linkAndExpire();
			ctx.pipeline().lettuceLayerToken = null;
		}
		Weaver.callOriginal();
	}

	@Trace(async=true)
	public void write(ChannelHandlerContext_Instrumentation ctx, Object msg, ChannelPromise promise) {
		if(ctx.pipeline().lettuceLayerToken != null) {
			ctx.pipeline().lettuceLayerToken.link();
		}
		Weaver.callOriginal();
	}
}
