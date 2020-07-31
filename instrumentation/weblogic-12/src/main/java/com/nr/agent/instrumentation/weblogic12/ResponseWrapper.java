/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.weblogic12;

import weblogic.servlet.internal.ServletResponseImpl;

import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.Response;

public class ResponseWrapper implements Response {

    private final ServletResponseImpl delegate;

    public ResponseWrapper(ServletResponseImpl servletResponse) {
        this.delegate = servletResponse;
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
