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

        /*
         * Add tokenLift hook if it hasn't already been added. This allows for tokens to be retrieved from
         * the current context and linked across threads at various points of the Flux/Mono lifecycle.
         *
         * This hook will only be added when using Netty Reactor with SpringBoot. When using other embedded web
         * servers (e.g. Tomcat, Jetty, Undertow) the Schedulers_Instrumentation class will add the hook.
         */
        if (!Hooks_Instrumentation.instrumented.getAndSet(true)) {
            Hooks.onEachOperator(TokenLinkingSubscriber.class.getName(), tokenLift());
        }
        Weaver.callOriginal();
    }
}
