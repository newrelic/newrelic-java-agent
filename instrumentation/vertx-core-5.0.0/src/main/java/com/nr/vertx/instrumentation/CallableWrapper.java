/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.vertx.instrumentation;

import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;

import java.util.concurrent.Callable;

public class CallableWrapper<T> implements Callable<T> {

    private final Callable<T> original;
    private Token token;

    public CallableWrapper(Callable<T> original, Token token) {
        this.original = original;
        this.token = token;
    }

    @Override
    @Trace(async = true)
    public T call() throws Exception {
        if (token != null) {
            token.linkAndExpire();
        }
        return original.call();
    }
}
