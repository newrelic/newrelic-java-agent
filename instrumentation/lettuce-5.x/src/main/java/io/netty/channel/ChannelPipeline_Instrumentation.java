package io.netty.channel;

import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;

@Weave(type = MatchType.BaseClass, originalName = "io.netty.channel.ChannelPipeline")
public class ChannelPipeline_Instrumentation {

    //this instrumentation is the same as in our netty instrumentation because all we do here is link between an async
    //jump in the reactive layer. This requires a token so instead of finding a new place to put the token, it just
    //lives next to the token that's used in the netty instrumentation
    @NewField
    public Token lettuceLayerToken;

}