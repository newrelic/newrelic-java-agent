package io.netty.channel;

import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;

@Weave(type = MatchType.BaseClass)
public abstract class DefaultChannelPipeline implements ChannelPipeline {

	@NewField
	public Token nettyToken = null;
	
	protected DefaultChannelPipeline(Channel channel) {
		
	}
}
