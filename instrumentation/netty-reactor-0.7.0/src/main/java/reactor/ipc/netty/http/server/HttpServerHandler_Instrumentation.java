/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package reactor.ipc.netty.http.server;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.TokenLinkingSubscriber;
import io.netty.channel.ChannelHandlerContext_Instrumentation;
import io.netty.handler.codec.http.HttpRequest;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Hooks_Instrumentation;

import static com.nr.instrumentation.TokenLinkingSubscriber.tokenLift;

@Weave(type = MatchType.BaseClass, originalName = "reactor.ipc.netty.http.server.HttpServerHandler")
class HttpServerHandler_Instrumentation {

    public void channelRead(ChannelHandlerContext_Instrumentation ctx, Object msg) {
        if (!Hooks_Instrumentation.instrumented.getAndSet(true)) {
            Hooks.onEachOperator(TokenLinkingSubscriber.class.getName(), tokenLift());
        }
        Weaver.callOriginal();
        if (msg instanceof HttpRequest) {
            if (ctx.pipeline().reactiveLayerToken == null) {
                ctx.pipeline().reactiveLayerToken = AgentBridge.getAgent().getTransaction().getToken();
            }
        }
    }
}
