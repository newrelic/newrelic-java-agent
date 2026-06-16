/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.vertx.ext.web.impl;

import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.vertx.instrumentation.VertxUtil;
import io.vertx.core.Handler;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.RoutingContext;

@Weave(originalName = "io.vertx.ext.web.impl.BlockingHandlerDecorator")
public class BlockingHandlerDecorator_Instrumentation {

    private final Handler<RoutingContext> decoratedHandler = Weaver.callOriginal();

    @Trace(async = true)
    private Object lambda$handle$0(Route route, RoutingContext context) throws Exception {
        VertxUtil.link(context);
        VertxUtil.nameSegment(decoratedHandler);
        return Weaver.callOriginal();
    }
}
