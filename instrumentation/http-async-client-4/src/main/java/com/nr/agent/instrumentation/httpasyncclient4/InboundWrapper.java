/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.httpasyncclient4;

import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.ExtendedInboundHeaders;
import org.apache.http.Header;
import org.apache.http.HttpResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * Wraps Apache http async client's inbound response headers for CAT.
 */
public class InboundWrapper extends ExtendedInboundHeaders {
    private final HttpResponse response;

    public InboundWrapper(HttpResponse response) {
        this.response = response;
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.HTTP;
    }

    @Override
    public String getHeader(String name) {
        // getHeaders always returns an array of length >= 0
        Header[] headers = response.getHeaders(name);
        if (headers.length > 0) {
            return headers[0].getValue();
        }
        return null;
    }

    @Override
    public List<String> getHeaders(String name) {
        // getHeaders always returns an array of length >= 0
        Header[] headers = response.getHeaders(name);
        if (headers.length > 0) {
            List<String> result = new ArrayList<>(headers.length);
            for (Header header : headers) {
                result.add(header.getValue());
            }
            return result;
        }
        return null;
    }
}
