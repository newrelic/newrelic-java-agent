package io.netty.channel;

import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;

@Weave(type = MatchType.BaseClass, originalName = "io.netty.channel.DefaultChannelPipeline")
abstract class DefaultChannelPipeline_Instrumentation implements ChannelPipeline {

	@NewField
	public Token nettyToken = null;
	
	public DefaultChannelPipeline_Instrumentation(AbstractChannel channel) {
		
	}
}
