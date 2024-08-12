/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.nr.vertx.sqlclient.instrumentation;

import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import io.vertx.core.Handler;

import java.util.logging.Level;

public class NRSqlClientWrapper<E> implements Handler<E> {
    private final Handler<E> delegate;
    private Token token;
    private Segment segment;

    public NRSqlClientWrapper(Handler<E> delegate, Segment segment) {
        this.delegate = delegate;
        token = NewRelic.getAgent().getTransaction().getToken();
        this.segment = segment;
    }

    @Override
    @Trace(async = true)
    public void handle(E event) {
        if (token != null) {
            token.linkAndExpire();
            token = null;
        }

        if (segment != null) {
            segment.end();
            segment = null;
        }

        if (delegate != null) {
            NewRelic.getAgent().getTracedMethod().setMetricName("Java", delegate.getClass().getName(), "handle");
            delegate.handle(event);
        }
    }
}
