/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.micronaut.http.client3;

import java.util.function.Consumer;

import org.reactivestreams.Subscription;

import com.newrelic.api.agent.HttpParameters;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Transaction;

public class ReactorListener implements Runnable, Consumer<Subscription> {

    private Segment segment = null;
    private Transaction txn = null;
    private HttpParameters params = null;

    public ReactorListener(Transaction t, HttpParameters p) {
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
            }
        }
    }

}