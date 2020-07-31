/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.resin3;

import com.caucho.server.connection.AbstractHttpResponse;
import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.Response;

public class ResponseWrapper implements Response {

    private final AbstractHttpResponse delegate;

    public ResponseWrapper(AbstractHttpResponse cauchoResponse) {
        this.delegate = cauchoResponse;
    }

    @Override
    public int getStatus() throws Exception {
        return delegate.getStatusCode();
    }

    @Override
    public String getStatusMessage() throws Exception {

        // we could reflect to get the _statusMessage field value
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
