/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.netty.bootstrap;

import com.agent.instrumentation.netty4116.NettyUtil;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.WeaveAllConstructors;
import com.newrelic.api.agent.weaver.Weaver;
import io.netty.channel.ChannelFuture;

import java.net.SocketAddress;

@Weave
public abstract class AbstractBootstrap {

    @WeaveAllConstructors
    AbstractBootstrap() {
        // initialize NettyDispatcher here to avoid classloader deadlocks
        NettyDispatcher.get();
    }

    private ChannelFuture doBind(final SocketAddress localAddress) {
        NettyUtil.setAppServerPort(localAddress);
        NettyUtil.setServerInfo();
        return Weaver.callOriginal();
    }
}
