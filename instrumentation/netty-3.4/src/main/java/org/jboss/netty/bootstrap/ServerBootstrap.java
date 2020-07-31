/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.jboss.netty.bootstrap;

import com.agent.instrumentation.netty34.NettyUtil;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;

import java.net.SocketAddress;
import java.util.logging.Level;

@Weave
public abstract class ServerBootstrap extends Bootstrap {

    public Channel bind(final SocketAddress localAddress) {
        NettyUtil.setAppServerPort(localAddress);
        NettyUtil.setServerInfo();

        // initialize NettyDispatcher here to avoid classloader deadlocks
        NettyDispatcher.get();

        return Weaver.callOriginal();
    }

}
