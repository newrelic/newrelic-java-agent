/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracers;

import com.newrelic.agent.Transaction;

public final class IgnoreTransactionTracerFactory extends AbstractTracerFactory {
    public static final String TRACER_FACTORY_NAME = IgnoreTransactionTracerFactory.class.getName();

    @Override
    public Tracer doGetTracer(Transaction transaction, ClassMethodSignature sig, Object object, Object[] args) {
        transaction.setIgnore(true);
        // we want to add a tracer to the stack to keep the transaction open & skipping child tracers
        return new MethodExitTracerNoSkip(sig, transaction) {
            @Override
            public String getGuid() {
                return null;
            }

            @Override
            protected void doFinish(int opcode, Object returnValue) {

            }
        };
    }
}
