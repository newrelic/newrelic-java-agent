/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.ktor.server.netty.cio;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.TransportType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

import com.newrelic.instrumentation.labs.ktor.netty.KtorNettyHeaders;
import io.ktor.server.netty.NettyApplicationCall_Instrumentation;

@Weave(originalName = "io.ktor.server.netty.cio.NettyHttpResponsePipeline")
public abstract class NettyHttpResponsePipeline_Instrumentation {

    @Trace(async = true)
    private void handleRequestMessage(NettyApplicationCall_Instrumentation call) {
        if (call.token != null) {
            call.token.linkAndExpire();
            call.token = null;
        }
        com.newrelic.api.agent.Transaction transaction = NewRelic.getAgent().getTransaction();
        if (!transaction.isWebTransaction()) {
            transaction.convertToWebTransaction();
        }
        if (call.nrHttpHeaders != null) {
            transaction.acceptDistributedTraceHeaders(TransportType.HTTP, new KtorNettyHeaders(call.nrHttpHeaders));
        }

        if (call.token == null) {
            Token t = transaction.getToken();
            if (t != null && t.isActive()) {
                call.token = t;
            } else if (t != null) {
                t.expire();
            }
        }
        Weaver.callOriginal();
    }

    @Trace(async = true)
    public void processResponse$ktor_server_netty(NettyApplicationCall_Instrumentation call) {
        if (call.token != null) {
            call.token.linkAndExpire();
            call.token = null;
        }
        Weaver.callOriginal();
    }

}
