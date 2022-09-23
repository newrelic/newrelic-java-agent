/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.websphere;

import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.Response;
import jakarta.servlet.http.HttpServletResponse;

public class ResponseWrapper implements Response {

    private final HttpServletResponse httpResponse;

    public ResponseWrapper(HttpServletResponse httpResponse) {
        this.httpResponse = httpResponse;
    }

    @Override
    public int getStatus() throws Exception {
        return httpResponse.getStatus();
    }

    @Override
    public String getStatusMessage() throws Exception {
        return null;
    }

    @Override
    public void setHeader(String name, String value) {
        httpResponse.setHeader(name, value);
    }

    @Override
    public String getContentType() {
        return httpResponse.getContentType();
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.HTTP;
    }
}
