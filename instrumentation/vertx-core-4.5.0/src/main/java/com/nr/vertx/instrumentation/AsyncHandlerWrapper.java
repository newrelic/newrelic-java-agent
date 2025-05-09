/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.nr.vertx.instrumentation;

import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

public class AsyncHandlerWrapper<T> implements Handler<AsyncResult<T>> {
    private Handler<AsyncResult<T>> original;

    private Token token;

    public AsyncHandlerWrapper(Handler<AsyncResult<T>> original, Token token) {
        this.original = original;
        this.token = token;
    }

    @Override
    @Trace(async = true, excludeFromTransactionTrace = true)
    public void handle(AsyncResult<T> event) {
        if (token != null) {
            token.linkAndExpire();
            token = null;
        }
        if (this.original != null) {
            this.original.handle(event);
            this.original = null;
        }
    }
}
