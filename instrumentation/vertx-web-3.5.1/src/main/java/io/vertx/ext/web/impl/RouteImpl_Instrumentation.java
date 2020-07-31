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
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

import java.util.List;
import java.util.regex.Pattern;

@Weave(originalName = "io.vertx.ext.web.impl.RouteImpl")
public abstract class RouteImpl_Instrumentation {

    private List<Handler<RoutingContext>> contextHandlers = Weaver.callOriginal();
    private List<Handler<RoutingContext>> failureHandlers = Weaver.callOriginal();

    private Pattern pattern = Weaver.callOriginal();

    @Trace
    synchronized void handleContext(RoutingContextImplBase_Instrumentation context) {
        VertxUtil.nameSegment(contextHandlers.get(context.currentRouteNextHandlerIndex() - 1));
        Weaver.callOriginal();
    }

    @Trace
    synchronized void handleFailure(RoutingContextImplBase_Instrumentation context) {
        VertxUtil.nameSegment(failureHandlers.get(context.currentRouteNextFailureHandlerIndex() - 1));
        Weaver.callOriginal();
    }

    public abstract String getPath();

    synchronized boolean matches(RoutingContextImplBase_Instrumentation context, String mountPoint, boolean failure) {
        boolean matches = Weaver.callOriginal();

        if (matches) {
            if (getPath() != null) {
                // Remove first slash
                VertxUtil.pushPath(context, getPath().substring(1));
            } else if (pattern != null) {
                VertxUtil.pushPath(context, pattern.toString());
            } else {
                VertxUtil.pushPath(context, VertxUtil.UNNAMED_PATH);
            }
        }

        return matches;
    }
}
