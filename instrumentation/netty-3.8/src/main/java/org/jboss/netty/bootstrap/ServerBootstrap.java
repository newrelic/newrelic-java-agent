/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.jboss.netty.bootstrap;

import java.net.SocketAddress;

import com.agent.instrumentation.netty38.NettyUtil;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import org.jboss.netty.channel.Channel;

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
