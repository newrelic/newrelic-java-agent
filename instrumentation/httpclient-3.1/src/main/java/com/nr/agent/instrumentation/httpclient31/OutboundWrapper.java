/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.httpclient31;

import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.OutboundHeaders;
import org.apache.commons.httpclient.HttpMethod;

public class OutboundWrapper implements OutboundHeaders {

    private final HttpMethod delegate;

    public OutboundWrapper(HttpMethod request) {
        this.delegate = request;
    }

    @Override
    public void setHeader(String name, String value) {
        delegate.setRequestHeader(name, value);
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.HTTP;
    }
}
