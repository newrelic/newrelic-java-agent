/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.micronaut.http.client3;

import java.util.function.Consumer;

import com.newrelic.api.agent.Transaction;
import com.newrelic.api.agent.TransportType;

import io.micronaut.http.HttpResponse;

public class ResponseConsumer implements Consumer<HttpResponse<?>> {

    private Transaction txn = null;

    public ResponseConsumer(Transaction t) {
        txn = t;
    }

    @Override
    public void accept(HttpResponse<?> response) {
        if (txn != null && response != null) {
            MicronautHeaders headers = new MicronautHeaders(response);
            txn.acceptDistributedTraceHeaders(TransportType.HTTP, headers);
        }
    }

}
