/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.nr.agent.instrumentation;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;

import java.util.concurrent.atomic.AtomicBoolean;

public class NRRunnable implements Runnable {
    private Runnable delegate = null;
    protected Token token = null;
    private static final AtomicBoolean isTransformed = new AtomicBoolean(false);

    public NRRunnable(Runnable runnable, Token token) {
        delegate = runnable;
        this.token = token;

        if (!isTransformed.get()) {
            AgentBridge.instrumentation.retransformUninstrumentedClass(NRRunnable.class);
            isTransformed.set(true);
        }
    }

    @Override
    @Trace(async=true)
    public void run() {
        NewRelic.getAgent().getTracedMethod().setMetricName("JBossExecutors", "SubmittedRunnable", delegate.getClass().getSimpleName());

        if (token != null) {
            token.linkAndExpire();
            token = null;
        }
        delegate.run();
    }

    public void cancel() {
        if (token != null) {
            token.expire();
            token = null;
        }
    }

}
