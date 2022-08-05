/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.glassfish6;

import jakarta.servlet.http.HttpServletResponse;

import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.Response;

public class GlassfishResponse implements Response {

    private final org.apache.catalina.Response delegate;
    private final HttpServletResponse response;

    public GlassfishResponse(org.apache.catalina.Response response) {
        this.delegate = response;
        if (response.getResponse() instanceof HttpServletResponse) {
            this.response = (HttpServletResponse) response.getResponse();
        } else {
            this.response = null;
        }
    }

    @Override
    public int getStatus() throws Exception {
        return response.getStatus();
    }

    @Override
    public String getStatusMessage() throws Exception {
        return delegate.getDetailMessage();
    }

    @Override
    public void setHeader(String name, String value) {
        response.setHeader(name, value);
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
