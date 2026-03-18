/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation;

import com.newrelic.api.agent.Token;
import org.reactivestreams.Subscription;
import reactor.util.context.Context;

public class SubscriptionWrapper implements Subscription {

    Subscription delegate;
    Context currentContext;

    public SubscriptionWrapper(Subscription delegate, Context context) {
        this.delegate = delegate;
        this.currentContext = context;
    }

    @Override
    public void request(long n) {
        delegate.request(n);
    }

    @Override
    public void cancel() {
        Token token = currentContext.getOrDefault("newrelic-token", null);
        if (token != null) {
            token.linkAndExpire();
        }
        if (delegate != null) delegate.cancel();
    }
}
