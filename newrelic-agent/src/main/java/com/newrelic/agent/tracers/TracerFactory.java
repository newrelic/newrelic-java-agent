/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracers;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.instrumentation.PointCut;

/**
 * Tracer factories create {@link Tracer}s. They are strongly associated with {@link PointCut}s (point cuts usually
 * implement the factory).
 * 
 * TracerFactory implementations should have a default constructor. They are usually singletons.
 */
public interface TracerFactory extends PointCutInvocationHandler {
    /**
     * Returns a tracer to trace an individual method invocation.
     */
    Tracer getTracer(Transaction transaction, ClassMethodSignature sig, Object object, Object[] args);

}
