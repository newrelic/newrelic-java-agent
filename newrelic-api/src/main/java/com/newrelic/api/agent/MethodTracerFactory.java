/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.api.agent;

/**
 * A MethodTracerFactory is called when an instrumented method is invoked. The MethodTracerFactory is registered using
 * an instrumentation extension file.
 * 
 * Implementations of this interface must have a public default constructor.
 * 
 * @deprecated
 */
public interface MethodTracerFactory {
    /**
     * Called when an instrumented method is invoked. This method can optionally return a method tracer to be notified
     * when the method invocation finishes (otherwise return <code>null</code>).
     * 
     * @param methodName The name of the traced method being invoked.
     * @param invocationTarget The object being invoked.
     * @param arguments The method arguments.
     * 
     * @return Can return a method tracer to be notified when a method invocation finishes, else returns
     *         <code>null</code>.
     * @since 2.3.0
     */
    MethodTracer methodInvoked(String methodName, Object invocationTarget, Object[] arguments);
}
