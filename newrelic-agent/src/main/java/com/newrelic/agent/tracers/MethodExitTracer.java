/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracers;

import java.text.MessageFormat;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.Transaction;

public abstract class MethodExitTracer extends MethodExitTracerNoSkip implements SkipTracer {

    public MethodExitTracer(ClassMethodSignature signature, Transaction transaction) {
        super(signature, transaction);
    }

    @Override
    public String getGuid() {
        return null;
    }

    @Override
    public final void childTracerFinished(Tracer child) {
        if (Agent.LOG.isLoggable(Level.FINER)) {
            String msg = MessageFormat.format("childTracerFinished called on {0} with child {1}",
                    this.getClass().getName(), child.getClass().getName());
            Agent.LOG.finer(msg);
        }
    }

}
