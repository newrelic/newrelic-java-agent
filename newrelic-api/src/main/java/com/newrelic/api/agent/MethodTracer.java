/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.api.agent;

/**
 * A {@link MethodTracerFactory} can return a MethodTracer to be notified when a method invocation finishes.
 * 
 * @deprecated
 */
public interface MethodTracer {

    /**
     * Called if a method exits successfully.
     * 
     * @param returnValue The return value of the method invocation, or <code>null</code> if the return value is void.
     * @since 2.3.0
     */
    void methodFinished(Object returnValue);

    /**
     * Called if a method exits because of an uncaught exception.
     * 
     * @param exception The uncaught exception thrown during the method invocation.
     * @since 2.3.0
     */
    void methodFinishedWithException(Throwable exception);
}
