/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.vertx.test.handlers;

import com.newrelic.api.agent.NewRelic;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;

public class SimpleHandlers {
    public static Handler<RoutingContext> createHandler(final String handlerName, final boolean sendsResponse) {
        return new Handler<RoutingContext>() {
            @Override
            public void handle(RoutingContext context) {
                NewRelic.addCustomParameter(handlerName, "y");

                if (sendsResponse) {
                    context.response()
                           .putHeader("content-type", "text/plain")
                           .end(handlerName + " sent response");
                } else {
                    context.next();
                }
            }
        };

    }

    public static Handler<RoutingContext> createLambdaHandler(final String handlerName, final boolean sendsResponse) {
        return context -> {
            NewRelic.addCustomParameter(handlerName, "y");

            if (sendsResponse) {
                context.response()
                       .putHeader("content-type", "text/plain")
                       .end(handlerName + " sent response");
            } else {
                context.next();
            }
        };

    }
}
