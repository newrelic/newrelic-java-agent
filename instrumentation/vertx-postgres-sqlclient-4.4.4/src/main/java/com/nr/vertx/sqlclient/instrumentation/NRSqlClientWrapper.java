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
    private Handler<E> delegate;
    private Token token;
    private Segment segment;
    private DatastoreParameters params;

    public NRSqlClientWrapper(Handler<E> d, Segment s, DatastoreParameters p) {
        delegate = d;
        token = NewRelic.getAgent().getTransaction().getToken();
        segment = s;
        params = p;
    }

    @Override
    @Trace(async = true)
    public void handle(E event) {
        NewRelic.getAgent().getLogger().log(Level.INFO, "DUF - NRSqlClientWrapper.handle - Event: {0}", event.toString());
        if (token != null) {
            token.linkAndExpire();
            token = null;
        }
        if (segment != null) {
            if (params != null) {
                segment.reportAsExternal(params);
            }
            segment.end();
            segment = null;
        } else if (params != null) {
            NewRelic.getAgent().getTracedMethod().reportAsExternal(params);
        }
        if (delegate != null) {
            delegate.handle(event);
        }
    }
}
