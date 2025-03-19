package io.micronaut.http.server.netty.websocket;

import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

import io.netty.channel.ChannelHandlerContext;

@Weave
public abstract class NettyServerWebSocketHandler {

	@Trace(dispatcher = true)
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
		Weaver.callOriginal();
	}
}
