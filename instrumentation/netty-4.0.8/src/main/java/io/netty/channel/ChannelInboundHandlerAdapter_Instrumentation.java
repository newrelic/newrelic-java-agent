package io.netty.channel;

import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

/**
 * 
 * Needed in order for the class hierarchy to instrument properly.  If we 
 * don't the NRNettyChannelHandler won't compile
 *
 */
@Weave(type = MatchType.BaseClass, originalName = "io.netty.channel.ChannelInboundHandlerAdapter")
public abstract class ChannelInboundHandlerAdapter_Instrumentation extends ChannelHandlerAdapter_Instrumentation implements ChannelInboundHandler {

	@Trace(async = true, excludeFromTransactionTrace = true)
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

		ChannelPipeline pipeline = ctx.pipeline();

		if (pipeline instanceof DefaultChannelPipeline_Instrumentation) {
			DefaultChannelPipeline_Instrumentation cPipeline = (DefaultChannelPipeline_Instrumentation)pipeline;
			if (cPipeline.nettyToken != null) {
				cPipeline.nettyToken.link();
			} 
		} 
		Weaver.callOriginal();
	}

	public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
		Weaver.callOriginal();
	}

	public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
		Weaver.callOriginal();
	}

	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		Weaver.callOriginal();
	}

	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		Weaver.callOriginal();
	}

	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		Weaver.callOriginal();
	}

	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		Weaver.callOriginal();
	}

	public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
		Weaver.callOriginal();
	}

	@Trace(async = true, excludeFromTransactionTrace = true)
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		if (ctx != null) {
			ChannelPipeline pipeline = ctx.pipeline();

			if (pipeline instanceof DefaultChannelPipeline_Instrumentation) {
				DefaultChannelPipeline_Instrumentation cPipeline = (DefaultChannelPipeline_Instrumentation)pipeline;
				if (cPipeline.nettyToken != null) {
					cPipeline.nettyToken.link();
				}
			}
		}
		Weaver.callOriginal();
	}

}
