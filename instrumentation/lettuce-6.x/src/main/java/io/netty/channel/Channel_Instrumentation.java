package io.netty.channel;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(type = MatchType.Interface, originalName = "io.netty.channel.Channel")
public class Channel_Instrumentation {

    public ChannelPipeline_Instrumentation pipeline() {
        return Weaver.callOriginal();
    }

}