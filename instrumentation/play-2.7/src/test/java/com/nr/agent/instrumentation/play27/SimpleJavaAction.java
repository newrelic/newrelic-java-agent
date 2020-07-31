/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.play27;


import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Singleton;

import play.Logger;
import play.libs.concurrent.Futures;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Results;

import static play.mvc.Http.Status.GATEWAY_TIMEOUT;

@Singleton
public class SimpleJavaAction extends play.mvc.Action.Simple {
    private final Logger.ALogger logger = play.Logger.of("application.SimpleJavaAction");

    private final HttpExecutionContext ec;
    private final Futures futures;

    @Inject
    public SimpleJavaAction(HttpExecutionContext ec, Futures futures) {
        this.ec = ec;
        this.futures = futures;
    }

    public CompletionStage<Result> call(Http.Context ctx) {
        if (logger.isTraceEnabled()) {
            logger.trace("call: ctx = " + ctx);
        }

        return futures.timeout(doCall(ctx), 1L, TimeUnit.SECONDS).exceptionally(e ->
                (Results.status(GATEWAY_TIMEOUT, "No")));
    }

    private CompletionStage<Result> doCall(Http.Context ctx) {
        return delegate.call(ctx).handleAsync((result, e) -> {
            if (e != null) {
                if (e instanceof CompletionException) {
                    logger.error("Direct exception " + e.getMessage(), e);
                    return internalServerError();
                } else {
                    logger.error("Unknown exception " + e.getMessage(), e);
                    return internalServerError();
                }
            } else {
                return result;
            }
        }, ec.current());
    }
}