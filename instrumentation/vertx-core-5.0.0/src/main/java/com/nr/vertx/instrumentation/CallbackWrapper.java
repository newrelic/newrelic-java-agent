/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.vertx.instrumentation;

import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import io.vertx.core.Handler;

public class CallbackWrapper<T> implements Handler<T> {

    private final Handler<T> original;
    private Token token;

    public CallbackWrapper(Handler<T> original, Token token) {
        this.original = original;
        this.token = token;
    }

    @Override
    @Trace(async = true)
    public void handle(T event) {
        if (token != null) {
            token.linkAndExpire();
        }
        original.handle(event);
    }
}
