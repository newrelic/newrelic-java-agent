/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.nr.agent.instrumentation.virtualthreads;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;

import java.util.concurrent.atomic.AtomicBoolean;

public class VirtualThreadRunnable implements Runnable {
    private static final AtomicBoolean isTransformed = new AtomicBoolean(false);

    private final Runnable delegate;
    private Token token;

    public VirtualThreadRunnable(Runnable delegate, Token token) {
        this.delegate = delegate;
        this.token = token;

        // Mirror the jboss-threads transform flow in case this class is loaded extremely early.
        if (!isTransformed.get()) {
            AgentBridge.instrumentation.retransformUninstrumentedClass(VirtualThreadRunnable.class);
            isTransformed.set(true);
        }
    }

    @Override
    @Trace(async = true)
    public void run() {
        NewRelic.getAgent().getTracedMethod().setMetricName("Java", "VirtualThread", "run");
        if (token != null) {
            token.linkAndExpire();
            token = null;
        }
        if (delegate != null) {
            delegate.run();
        }
    }
}