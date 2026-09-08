/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.micronaut;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

public class NRTokenCallableWrapper<V> implements Callable<V> {

    private static final AtomicBoolean isTransformed = new AtomicBoolean(false);

    private final Callable<V> delegate;
    private Token token;

    public NRTokenCallableWrapper(Callable<V> delegate, Token token) {
        this.delegate = delegate;
        this.token = token;
        if (!isTransformed.getAndSet(true)) {
            AgentBridge.instrumentation.retransformUninstrumentedClass(getClass());
        }
    }

    @Override
    @Trace(async = true)
    public V call() throws Exception {
        if (token != null) {
            token.linkAndExpire();
            token = null;
        }
        return delegate.call();
    }
}
