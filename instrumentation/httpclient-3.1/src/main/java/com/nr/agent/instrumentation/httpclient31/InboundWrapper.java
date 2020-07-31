/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.httpclient31;

import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.ExtendedInboundHeaders;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;

import java.util.ArrayList;
import java.util.List;

public class InboundWrapper extends ExtendedInboundHeaders {
    private final HttpMethod delegate;

    public InboundWrapper(HttpMethod response) {
        this.delegate = response;
    }

    @Override
    public String getHeader(String name) {
        Header[] headers = delegate.getResponseHeaders(name);
        if (headers.length > 0) {
            return headers[0].getValue();
        }
        return null;
    }

    @Override
    public List<String> getHeaders(String name) {
        Header[] headers = delegate.getResponseHeaders(name);
        if (headers.length > 0) {
            List<String> result = new ArrayList<>(headers.length);
            for (Header header : headers) {
                result.add(header.getValue());
            }
            return result;
        }
        return null;
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.HTTP;
    }
}
