/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.vertx.core.net.impl;

import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.vertx.instrumentation.VertxCoreUtil;
import io.netty.channel.ChannelHandlerContext;

@Weave(originalName = "io.vertx.core.net.impl.VertxHandler")
public abstract class VertxHandler_Instrumentation {
    public void handlerAdded(ChannelHandlerContext ctx) {
        VertxCoreUtil.BUILDING_CONNECTION.set(Boolean.TRUE);
        try {
            Weaver.callOriginal();
        } finally {
            VertxCoreUtil.BUILDING_CONNECTION.remove();
        }
    }
}
