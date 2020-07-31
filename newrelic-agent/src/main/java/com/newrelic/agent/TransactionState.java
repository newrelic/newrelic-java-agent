/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.TracerFactory;

public interface TransactionState {

    Tracer getTracer(Transaction tx, TracerFactory tracerFactory, ClassMethodSignature sig, Object obj, Object... args);

    Tracer getTracer(Transaction tx, String tracerFactoryName, ClassMethodSignature sig, Object obj, Object... args);

    Tracer getTracer(Transaction tx, Object invocationTarget, ClassMethodSignature sig, String metricName, int flags);

    Tracer getSqlTracer(Transaction tx, Object invocationTarget, ClassMethodSignature sig, String metricName, int flags);

    /**
     * @return true if transaction should finish, false if should suspend (async).
     */
    boolean finish(Transaction tx, Tracer tracer);

    void resume();

    void suspend();

    void suspendRootTracer();

    void complete();

    Tracer getRootTracer();
}
