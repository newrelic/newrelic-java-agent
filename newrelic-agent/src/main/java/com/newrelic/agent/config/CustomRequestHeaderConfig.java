/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

public interface CustomRequestHeaderConfig {


    /**
     * Returns the request header name that should be captured as a custom attribute. e.g:
     *
     * <pre>X-trace-id</pre>
     *
     * @return the request header to be captured as a custom attribute
     */
    String getHeaderName();

    /**
     * Returns an optional string alias for the header name.
     *
     * <b>NOTE:</b> This will return null if no alias is given
     * <pre>TraceId</pre>
     * @return the alias to use for the request header. null if alias is not given
     */
    String getHeaderAlias();

}



