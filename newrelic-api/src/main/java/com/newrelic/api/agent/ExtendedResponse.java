/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.api.agent;

/**
 * A {@link com.newrelic.api.agent.Response} that provides more information for reporting to New Relic. API users should
 * extend this class when wrapping their HTTP responses rather than simply implementing
 * {@link com.newrelic.api.agent.Response} to receive additional functionality.
 *
 * @since 3.41.0
 */
public abstract class ExtendedResponse implements Response {

    /**
     * The Content-Length for this response
     *
     * @return Content-Length (in bytes) for this response
     * @since 3.41.0
     */
    public abstract long getContentLength();
    
}
