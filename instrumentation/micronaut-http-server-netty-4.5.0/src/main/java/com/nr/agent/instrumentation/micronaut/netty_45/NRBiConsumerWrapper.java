/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.micronaut.netty_45;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

public class NRBiConsumerWrapper<R> implements BiConsumer<R, Throwable> {

    private static final AtomicBoolean isTransformed = new AtomicBoolean(false);
    BiConsumer<R, Throwable> delegate = null;
    private Token token = null;

    public NRBiConsumerWrapper(BiConsumer<R, Throwable> d, Token t) {
        delegate = d;
        token = t;
        if (!isTransformed.getAndSet(true)) {
            AgentBridge.instrumentation.retransformUninstrumentedClass(getClass());
        }
    }

    @Override
    @Trace(async = true)
    public void accept(R t, Throwable u) {
        if (token != null) {
            token.linkAndExpire();
            token = null;
        }
        if (u != null) {
            NewRelic.noticeError(u);
        }
        if (delegate != null) {
            delegate.accept(t, u);
        }
    }

}
