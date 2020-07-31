/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge;

public interface AsyncApi {

    /**
     * Record an error for the asynchronous operation represented by the given async context.
     * 
     * @param asyncContext
     */
    @Deprecated
    public void errorAsync(Object asyncContext, Throwable t);

    /**
     * Suspend the asynchronous operation represented by the given async context.
     * 
     * @param asyncContext
     */
    @Deprecated
    void suspendAsync(Object asyncContext);

    /**
     * Resume the asynchronous operation represented by the given async context.
     * 
     * @param asyncContext
     */
    @Deprecated
    Transaction resumeAsync(Object asyncContext);

    /**
     * Complete the asynchronous operation represented by the given async context.
     * 
     * @param asyncContext
     */
    @Deprecated
    void completeAsync(Object asyncContext);

    /**
     * Finish the root tracer for the current transaction.
     */
    @Deprecated
    void finishRootTracer();

}
