/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracers;

/**
 * Intercepts the invocation of an instrumented method to do some work. Implementations are not part of the transaction
 * stack.
 */
public interface EntryInvocationHandler extends PointCutInvocationHandler {
    void handleInvocation(ClassMethodSignature sig, Object object, Object[] args);
}
