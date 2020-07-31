/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.okhttp4;

import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.OutboundHeaders;
import okhttp3.Request;

public class OutboundWrapper implements OutboundHeaders {

    private Request delegate;

    public OutboundWrapper(Request request) {
        delegate = request;
    }

    public Request getRequestWithNRHeaders() {
        return delegate;
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.HTTP;
    }

    /**
     * Requests are immutable, so we need to build a new one and save it each time a header is added.
     */
    @Override
    public void setHeader(String name, String value) {
        delegate = delegate.newBuilder().addHeader(name, value).build();
    }

}
