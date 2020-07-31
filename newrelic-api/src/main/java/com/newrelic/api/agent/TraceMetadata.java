/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.api.agent;

/**
 * Used to provide information about the currently executing trace.
 */
public interface TraceMetadata {

    /**
     * Returns the currently executing trace identifier.
     *
     * An empty String will be returned if the transaction does not support this functionality, distributed
     * tracing is disabled or if the trace identifier is not known at the time of the method call.
     *
     * @return the trace identifier associated with the current context.
     * @since 5.6.0
     */
    String getTraceId();

    /**
     * Returns the span identifier associated with the current executing span.
     *
     * An empty String will be returned if the span does not support this functionality, distributed
     * tracing is disabled or if the span identifier is not known at the time of the method call.
     *
     * @return the span identifier associated with the current executing span.
     * @since 5.6.0
     */
    String getSpanId();

    /**
     * Returns whether or not the current transaction is sampled from a distributed tracing perspective.
     *
     * If distributed tracing is disabled this method will always return false.
     *
     * @return true if distributed tracing is enabled and this transaction is sampled, false otherwise
     * @since 5.6.0
     */
    boolean isSampled();

}
