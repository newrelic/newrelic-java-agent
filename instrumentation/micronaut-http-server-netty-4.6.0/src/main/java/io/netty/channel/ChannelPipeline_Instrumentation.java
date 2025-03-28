package io.netty.channel;

import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;

@Weave(type = MatchType.BaseClass, originalName = "io.netty.channel.ChannelPipeline")
public class ChannelPipeline_Instrumentation {

	@NewField
	public Token micronautToken;
	
}
