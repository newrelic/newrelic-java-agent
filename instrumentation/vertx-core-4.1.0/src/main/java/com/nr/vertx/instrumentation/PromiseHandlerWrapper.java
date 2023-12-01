/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.nr.vertx.instrumentation;

import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import io.vertx.core.Handler;
import io.vertx.core.Promise;

public class PromiseHandlerWrapper<T> implements Handler<Promise<T>> {

    private Handler<Promise<T>> original;

    private Token token;

    public PromiseHandlerWrapper(Handler<Promise<T>> original, Token token) {
        this.original = original;
        this.token = token;
    }

    @Override
    @Trace(async = true, excludeFromTransactionTrace = true)
    public void handle(Promise<T> event) {
        if (token != null) {
            token.linkAndExpire();
            token = null;
        }

        this.original.handle(event);
        this.original = null;
    }
}
