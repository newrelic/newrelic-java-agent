/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracers;

public interface ErrorTracer {
    /**
     * Sets an error that was identified by the public API.
     */
    void setNoticedError(Throwable throwable);

    /**
     * Captures the exception that the was thrown during the tracer's execution.
     */
    void setThrownException(Throwable throwable);

    /**
     * Returns the exception, if any, that was noticed by the API or thrown by the tracer.
     */
    Throwable getException();

    /**
     * Returns {@literal true} if the exception was set by {@link #setNoticedError(Throwable)}
     */
    boolean wasExceptionSetByAPI();

    /**
     * Gets the random identifier of this Tracer.
     */
    String getGuid();
}
