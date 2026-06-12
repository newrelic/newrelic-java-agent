/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.vertx.instrumentation;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.newrelic.agent.bridge.Transaction;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

import java.net.UnknownHostException;

@SuppressWarnings("rawtypes")
public class AsyncRequestResultHandler implements Handler<AsyncResult> {

    private Token token;

    public AsyncRequestResultHandler(Token token) {
        this.token = token;
    }

    @Override
    @Trace(async = true)
    public void handle(AsyncResult ar) {
        if (token != null) {
            token.linkAndExpire();
        }

        Transaction txn = AgentBridge.getAgent().getTransaction(false);
        if (txn != null) {
            if (ar.failed() && ar.cause() instanceof UnknownHostException) {
                Segment seg = NewRelic.getAgent().getTransaction()
                        .startSegment(VertxCoreUtil.VERTX_CLIENT, VertxCoreUtil.END);
                VertxCoreUtil.reportUnknownHost(seg);
                seg.end();
            }

            txn.expireAllTokens();
        }
    }
}
