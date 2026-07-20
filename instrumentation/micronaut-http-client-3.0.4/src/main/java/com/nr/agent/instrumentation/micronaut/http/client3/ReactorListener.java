/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.micronaut.http.client3;

import java.util.function.Consumer;

import com.newrelic.agent.bridge.AgentBridge;
import org.reactivestreams.Subscription;

import com.newrelic.api.agent.HttpParameters;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Transaction;

public class ReactorListener implements Runnable, Consumer<Subscription> {

    private final String httpMethod;
    private Segment segment = null;
    private Transaction txn = null;
    private HttpParameters params = null;

    public ReactorListener(Transaction t, HttpParameters p, String httpMethod) {
        this.httpMethod = httpMethod;
        txn = t;
        params = p;
    }

    @Override
    public void run() {
        if (segment != null) {
            segment.end();
        }
    }

    @Override
    public void accept(Subscription t) {
        if (txn != null && segment == null) {
            if (params != null) {
                String proc = params.getProcedure();
                segment = txn.startSegment("MicronautClient/" + proc);
                segment.reportAsExternal(params);
                AgentBridge.getAgent().setHttpMethod(segment, httpMethod);
            }
        }
    }

}