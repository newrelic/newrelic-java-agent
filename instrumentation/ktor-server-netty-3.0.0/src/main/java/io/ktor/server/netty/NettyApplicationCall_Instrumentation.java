package io.ktor.server.netty;

import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;

import io.ktor.server.application.Application;
import io.netty.channel.ChannelHandlerContext;


@Weave(originalName = "io.ktor.server.netty.NettyApplicationCall")
public class NettyApplicationCall_Instrumentation {

	@NewField
	public Token token = null;
	
	public NettyApplicationCall_Instrumentation(Application application, ChannelHandlerContext context, Object requestMessage) {
		
	}
}
