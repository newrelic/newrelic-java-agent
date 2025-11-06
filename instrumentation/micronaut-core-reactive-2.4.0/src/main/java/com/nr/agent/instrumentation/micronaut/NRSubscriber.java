/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.nr.agent.instrumentation.micronaut;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.atomic.AtomicBoolean;

public class NRSubscriber<T> implements Subscriber<T> {
    private Subscriber<T> delegate = null;
    private Token token = null;
    private static final AtomicBoolean isTransformed = new AtomicBoolean(false);

    public NRSubscriber(Subscriber<T> d) {
        delegate = d;
        if(!isTransformed.getAndSet(true)) {
            AgentBridge.instrumentation.retransformUninstrumentedClass(getClass());
        }
    }

    @Override
    public void onSubscribe(Subscription s) {
        delegate.onSubscribe(s);
        Token t = NewRelic.getAgent().getTransaction().getToken();
        if (t != null) {
            token = t;
        }
    }

    @Override
    @Trace(async = true)
    public void onNext(T t) {
        if (token != null) {
            token.link();
        }
        delegate.onNext(t);
    }

    @Override
    @Trace(async = true)
    public void onError(Throwable t) {
        NewRelic.noticeError(t);
        if (token != null) {
            token.linkAndExpire();
            token = null;
        }
        delegate.onError(t);
    }

    @Override
    @Trace(async = true)
    public void onComplete() {
        if (token != null) {
            token.linkAndExpire();
            token = null;
        }
        delegate.onComplete();
    }
}