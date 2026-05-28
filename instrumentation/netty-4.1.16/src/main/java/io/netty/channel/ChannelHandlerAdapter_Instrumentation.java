package io.netty.channel;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;


/**
 * 
 * Needed in order for the class hierarchy to instrument properly.  If we 
 * don't the NRNettyChannelHandler won't compile
 *
 */
@Weave(type = MatchType.BaseClass, originalName = "io.netty.channel.ChannelHandlerAdapter")
public abstract class ChannelHandlerAdapter_Instrumentation implements ChannelHandler {

	public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
		Weaver.callOriginal();
	}
	
	 public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
		 Weaver.callOriginal();
	 }
	 
	 public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		 Weaver.callOriginal();
	 }
}
