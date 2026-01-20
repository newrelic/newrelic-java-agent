/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge;

import java.lang.reflect.InvocationHandler;


public interface ExitTracer extends InvocationHandler, TracedMethod {
    /**
     * Called after an invocation completes.
     * 
     * @param opcode the return code of the invocation
     * @param returnValue the object returned by the invocation
     */
    void finish(int opcode, Object returnValue);

    /**
     * Called when a method invocation throws an exception.
     * 
     * @param throwable
     */
    void finish(Throwable throwable);
}
