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
    boolean iterateNext() {
        VertxUtil.link(this);
        return Weaver.callOriginal();
    }

    void restart() {
        addHeadersEndHandler(VertxUtil.expireAndNameTxnHandler(this));
        Weaver.callOriginal();
    }

    int currentRouteNextHandlerIndex() {
        return Weaver.callOriginal();
    }

    int currentRouteNextFailureHandlerIndex() {
        return Weaver.callOriginal();
    }

}
