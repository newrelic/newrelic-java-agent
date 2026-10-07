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

import java.util.concurrent.atomic.AtomicBoolean;

public class NRTokenRunnableWrapper implements Runnable {

    private static final AtomicBoolean isTransformed = new AtomicBoolean(false);

    private final Runnable delegate;
    private Token token;

    public NRTokenRunnableWrapper(Runnable delegate, Token token) {
        this.delegate = delegate;
        this.token = token;
        if (!isTransformed.getAndSet(true)) {
            AgentBridge.instrumentation.retransformUninstrumentedClass(getClass());
        }
    }

    @Override
    @Trace(async = true)
    public void run() {
        if (token != null) {
            token.linkAndExpire();
            token = null;
        }
        delegate.run();
    }
}
