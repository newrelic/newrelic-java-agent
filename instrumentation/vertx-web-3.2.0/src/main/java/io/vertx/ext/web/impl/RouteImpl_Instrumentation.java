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

import java.util.regex.Pattern;

@Weave(originalName = "io.vertx.ext.web.impl.RouteImpl")
public abstract class RouteImpl_Instrumentation {

    private Handler<RoutingContext> contextHandler = Weaver.callOriginal();
    private Handler<RoutingContext> failureHandler = Weaver.callOriginal();

    private Pattern pattern = Weaver.callOriginal();

    @Trace
    synchronized void handleContext(RoutingContext context) {
        VertxUtil.nameSegment(contextHandler);
        Weaver.callOriginal();
    }

    @Trace
    synchronized void handleFailure(RoutingContext context) {
        VertxUtil.nameSegment(failureHandler);
        Weaver.callOriginal();
    }

    public abstract String getPath();

    synchronized boolean matches(RoutingContext context, String mountPoint, boolean failure) {
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
