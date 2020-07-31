/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package reactor.netty.channel;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import io.netty.channel.Channel_Instrumentation;

@Weave(type = MatchType.BaseClass, originalName = "reactor.netty.channel.ChannelOperations")
public abstract class ChannelOperations_Instrumentation {
    public abstract Channel_Instrumentation channel();

    protected void terminate() {
        if (channel().pipeline().reactiveLayerToken != null) {
            channel().pipeline().reactiveLayerToken.expire();
            channel().pipeline().reactiveLayerToken = null;
        }
        Weaver.callOriginal();
    }
}