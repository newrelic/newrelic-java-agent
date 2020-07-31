/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.servlet24;

import javax.servlet.http.HttpServletResponse;

import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.Response;

public class NRResponseWrapper implements Response {

    private final HttpServletResponse response;

    public NRResponseWrapper(HttpServletResponse response) {
        this.response = response;
    }

    /**
     * Since {@link #getStatus()} does not exist prior to Servlet 3.0, return 0, meaning "unknown".
     * 
     * @since Servlet 3.0
     */
    @Override
    public int getStatus() throws Exception {
        return 0;
    }

    @Override
    public String getStatusMessage() throws Exception {
        return null;
    }

    @Override
    public void setHeader(String name, String value) {
        response.setHeader(name, value);
    }

    @Override
    public String getContentType() {
        return response.getContentType();
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.HTTP;
    }
}
