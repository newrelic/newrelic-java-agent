/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.jetty9;

import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.Response;

public class JettyResponse implements Response {

    private final org.eclipse.jetty.server.Response delegate;

    public JettyResponse(org.eclipse.jetty.server.Response response) {
        this.delegate = response;
    }

    @Override
    public int getStatus() throws Exception {
        return delegate.getStatus();
    }

    @Override
    public String getStatusMessage() throws Exception {
        return delegate.getReason();
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
