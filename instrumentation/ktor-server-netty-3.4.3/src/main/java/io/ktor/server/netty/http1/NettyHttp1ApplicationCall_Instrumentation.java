/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.ktor.server.netty.http1;

import com.newrelic.api.agent.weaver.Weave;
import io.ktor.server.application.Application;
import io.ktor.server.netty.NettyApplicationCall_Instrumentation;
import io.ktor.utils.io.ByteReadChannel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import kotlin.coroutines.CoroutineContext;

@Weave(originalName = "io.ktor.server.netty.http1.NettyHttp1ApplicationCall")
public class NettyHttp1ApplicationCall_Instrumentation extends NettyApplicationCall_Instrumentation {

    public NettyHttp1ApplicationCall_Instrumentation(Application application, ChannelHandlerContext context,
            HttpRequest requestMessage, ByteReadChannel contentChannel, CoroutineContext engineContext, CoroutineContext userContext) {
        super(application, context, requestMessage);
    }
}
