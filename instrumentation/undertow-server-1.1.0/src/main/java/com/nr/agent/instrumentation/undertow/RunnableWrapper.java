/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.nr.agent.instrumentation.undertow;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;

import java.util.logging.Level;

public class RunnableWrapper implements Runnable {
    private final Runnable delegate;
    private Token token;

    public RunnableWrapper(Runnable delegate, Token token) {
        this.delegate = delegate;
        this.token = token;
    }

    public void run() {
        if (token != null) {
            token.link();
            token = null;
        }
        if (delegate != null) {
            delegate.run();
        }
    }
}
