/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.micronaut.http.netty.reactive;

import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import io.netty.channel.ChannelHandlerContext;

@Weave(originalName = "io.micronaut.http.netty.reactive.HandlerPublisher", type = MatchType.ExactClass)
public abstract class HandlerPublisher_Instrumentation<T> {

    @Trace
    public void channelRead(ChannelHandlerContext ctx, Object message) {
        Weaver.callOriginal();
    }
}
