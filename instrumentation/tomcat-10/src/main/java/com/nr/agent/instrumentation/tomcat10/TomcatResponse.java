/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.tomcat10;

import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.Response;

public class TomcatResponse implements Response {

    private final org.apache.catalina.connector.Response delegate;

    public TomcatResponse(org.apache.catalina.connector.Response response) {
        this.delegate = response;
    }

    @Override
    public int getStatus() throws Exception {
        return delegate.getStatus();
    }

    @Override
    public String getStatusMessage() throws Exception {
        return delegate.getMessage();
    }

    @Override
    public void setHeader(String name, String value) {
        delegate.addHeader(name, value);
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
