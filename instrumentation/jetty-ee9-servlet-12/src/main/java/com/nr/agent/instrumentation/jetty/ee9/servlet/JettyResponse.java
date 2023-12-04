/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.jetty.ee9.servlet;

import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.Response;
import jakarta.servlet.http.HttpServletResponse;

public class JettyResponse implements Response {

    private final HttpServletResponse delegate;

    public JettyResponse(HttpServletResponse response) {
        this.delegate = response;
    }

    @Override
    public int getStatus() throws Exception {
        return delegate.getStatus();
    }

    @Override
    public String getStatusMessage() throws Exception {
        return null;
    }

    @Override
    public void setHeader(String name, String value) {
        delegate.setHeader(name, value);
    }

    @Override
    public String getContentType() {
        return delegate.getContentType();
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.HTTP;
    }
}
