/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracers;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.service.ServiceFactory;

public abstract class AbstractTracerFactory implements TracerFactory {
    @Override
    public Tracer getTracer(Transaction transaction, ClassMethodSignature sig, Object object, Object[] args) {
        return (canCreateTracer()) ? doGetTracer(transaction, sig, object, args) : null;
    }

    public abstract Tracer doGetTracer(Transaction transaction, ClassMethodSignature sig, Object object, Object[] args);

    public boolean canCreateTracer() {
        return !ServiceFactory.getServiceManager().getCircuitBreakerService().isTripped();
    }
}
