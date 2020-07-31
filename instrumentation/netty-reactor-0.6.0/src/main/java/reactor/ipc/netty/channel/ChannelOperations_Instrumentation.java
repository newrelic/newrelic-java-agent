/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package reactor.ipc.netty.channel;

import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import io.netty.channel.Channel_Instrumentation;

@Weave(type = MatchType.BaseClass, originalName = "reactor.ipc.netty.channel.ChannelOperations")
public class ChannelOperations_Instrumentation {

    final Channel_Instrumentation channel = Weaver.callOriginal();

    /**
     * Currently misses the majority of the work inside applyHandler because it only does:
     * Mono.fromDirect(handler.apply((INBOUND) this, (OUTBOUND) this)).subscribe(this);
     *
     * And we aren't instrumenting Mono
     */
    @Trace(async = true)
    protected final void applyHandler() { // not an interface method

        Token token = channel.pipeline().reactiveLayerToken;
        if (token != null) {
            token.linkAndExpire();
            channel.pipeline().reactiveLayerToken = null;
        }
        Weaver.callOriginal();
    }

}
