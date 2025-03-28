package io.netty.channel;

import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;

@Weave(originalName = "io.netty.channel.ChannelPipeline", type = MatchType.BaseClass)
public class ChannelPipeline_Instrumentation {

    @NewField
    public Token micronautToken;

}
