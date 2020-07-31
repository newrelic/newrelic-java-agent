/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.sql;

import com.newrelic.agent.TransactionData;
import com.newrelic.agent.tracers.Tracer;

public class SlowQueryTracerInfo {

    private final Tracer tracer;
    private TransactionData transactionData;

    SlowQueryTracerInfo(TransactionData transactionData, Tracer tracer) {
        this.transactionData = transactionData;
        this.tracer = tracer;
    }

    public TransactionData getTransactionData() {
        return transactionData;
    }

    public Tracer getTracer() {
        return tracer;
    }

    public void setTransactionData(TransactionData td) {
        this.transactionData = td;
    }

}