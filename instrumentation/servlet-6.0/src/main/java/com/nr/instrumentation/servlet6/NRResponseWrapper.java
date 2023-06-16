/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.servlet6;

import jakarta.servlet.http.HttpServletResponse;

import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.Response;

public class NRResponseWrapper implements Response {

    private final HttpServletResponse response;

    public NRResponseWrapper(HttpServletResponse response) {
        this.response = response;
    }

    @Override
    public int getStatus() throws Exception {
        return response.getStatus();
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
