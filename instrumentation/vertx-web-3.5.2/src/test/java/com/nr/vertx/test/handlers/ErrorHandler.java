/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.vertx.test.handlers;

import com.newrelic.api.agent.NewRelic;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class ErrorHandler {

    public static Handler<RoutingContext> createHandlerWithStatusCode(String errorHandler, int statusCode) {
        return routingContext -> {
            NewRelic.addCustomParameter(errorHandler, "y");
            routingContext.fail(statusCode);
        };
    }

    public static Handler<RoutingContext> createHandlerWithException(String errorHandler, Throwable exception) {
        return routingContext -> {
            NewRelic.addCustomParameter(errorHandler, "y");
            routingContext.fail(exception);
        };
    }

    public static Handler<RoutingContext> createFailureHandler(String failureHandler) {
        // Handles failures
        return failureRoutingContext -> {
            NewRelic.addCustomParameter(failureHandler, "y");
            failureRoutingContext.response().setStatusCode(530).end("It's cold here!");
        };
    }

    public static Handler<RoutingContext> createFailureHandlerException(String failureHandler) {
        // Fail to handle failure
        return failureRoutingContext -> {
            NewRelic.addCustomParameter(failureHandler, "y");
            throw new RuntimeException("Error handler failed");
        };
    }
}
