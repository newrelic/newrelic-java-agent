/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.netty.bootstrap;

import java.net.SocketAddress;

import com.agent.instrumentation.netty4116.NettyUtil;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

import io.netty.channel.ChannelFuture;

@Weave(type = MatchType.ExactClass, originalName = "io.netty.bootstrap.AbstractBootstrap")
public abstract class AbstractBootstrap_Instrumentation {

    @SuppressWarnings("unused")
	private ChannelFuture doBind(final SocketAddress localAddress) {
        NettyUtil.setAppServerPort(localAddress);
        NettyUtil.setServerInfo();
        return Weaver.callOriginal();
    }
    
}
