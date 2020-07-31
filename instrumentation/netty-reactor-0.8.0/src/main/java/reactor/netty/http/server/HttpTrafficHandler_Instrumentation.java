/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package reactor.netty.http.server;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import io.netty.channel.ChannelHandlerContext_Instrumentation;

@Weave(originalName = "reactor.netty.http.server.HttpTrafficHandler")
class HttpTrafficHandler_Instrumentation {

    @Trace(async = true)
    public void channelRead(ChannelHandlerContext_Instrumentation ctx, Object msg) {
        if (ctx.pipeline().reactiveLayerToken == null) {
            ctx.pipeline().reactiveLayerToken = NewRelic.getAgent().getTransaction().getToken();
        } else {
            ctx.pipeline().reactiveLayerToken.link();
        }
        Weaver.callOriginal();
    }

}
