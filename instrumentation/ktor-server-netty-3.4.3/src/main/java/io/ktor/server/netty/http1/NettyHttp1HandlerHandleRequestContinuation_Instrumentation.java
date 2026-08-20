/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.ktor.server.netty.http1;

import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import io.ktor.server.netty.NettyApplicationCall_Instrumentation;

@Weave(originalName = "io.ktor.server.netty.http1.NettyHttp1Handler$handleRequest$1$1")
class NettyHttp1HandlerHandleRequestContinuation_Instrumentation {

    final NettyHttp1ApplicationCall_Instrumentation $call = Weaver.callOriginal();

    @Trace(async = true)
    public Object invokeSuspend(Object result) {
        NettyApplicationCall_Instrumentation baseCall = $call;
        if (baseCall != null) {
            Token routingToken = baseCall.routingToken;
            if (routingToken != null) {
                routingToken.linkAndExpire();
                baseCall.routingToken = null;
            }
        }
        return Weaver.callOriginal();
    }
}
