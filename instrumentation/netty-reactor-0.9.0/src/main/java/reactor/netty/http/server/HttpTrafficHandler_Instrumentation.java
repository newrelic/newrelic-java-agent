/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package reactor.netty.http.server;

import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.reactor.netty.TokenLinkingSubscriber;
import io.netty.channel.ChannelHandlerContext;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Hooks_Instrumentation;

import static com.nr.instrumentation.reactor.netty.TokenLinkingSubscriber.tokenLift;

@Weave(originalName = "reactor.netty.http.server.HttpTrafficHandler")
class HttpTrafficHandler_Instrumentation {
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (!Hooks_Instrumentation.instrumented.getAndSet(true)) {
            Hooks.onEachOperator(TokenLinkingSubscriber.class.getName(), tokenLift());
        }
        Weaver.callOriginal();
    }
}
