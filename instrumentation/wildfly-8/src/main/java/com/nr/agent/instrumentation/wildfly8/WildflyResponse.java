/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.wildfly8;

import io.undertow.servlet.spec.HttpServletResponseImpl;

import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.Response;

public class WildflyResponse implements Response {

    private final HttpServletResponseImpl impl;

    public WildflyResponse(HttpServletResponseImpl response) {
        this.impl = response;
    }

    @Override
    public int getStatus() throws Exception {
        return impl.getStatus();
    }

    @Override
    public String getStatusMessage() throws Exception {
        // the status message is not stored
        return null;
    }

    @Override
    public void setHeader(String name, String value) {
        impl.setHeader(name, value);
    }

    @Override
    public String getContentType() {
        return impl.getContentType();
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.HTTP;
    }
}
