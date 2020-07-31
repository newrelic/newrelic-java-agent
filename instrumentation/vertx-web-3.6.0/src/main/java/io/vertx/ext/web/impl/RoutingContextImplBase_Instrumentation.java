/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.vertx.ext.web.impl;

import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.vertx.instrumentation.VertxUtil;
import io.vertx.ext.web.RoutingContext;

@Weave(originalName = "io.vertx.ext.web.impl.RoutingContextImplBase")
public abstract class RoutingContextImplBase_Instrumentation implements RoutingContext {

    @Trace(async = true)
    protected void unhandledFailure(int statusCode, Throwable failure, RouterImpl router) {
        VertxUtil.link(this);
        Weaver.callOriginal();
    }

    @Trace(async = true)
    protected boolean iterateNext() {
        VertxUtil.link(this);
        return Weaver.callOriginal();
    }

    protected void restart() {
        addHeadersEndHandler(VertxUtil.expireAndNameTxnHandler(this));
        Weaver.callOriginal();
    }

    protected int currentRouteNextHandlerIndex() {
        return Weaver.callOriginal();
    }

    protected int currentRouteNextFailureHandlerIndex() {
        return Weaver.callOriginal();
    }

}
