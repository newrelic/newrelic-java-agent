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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class InboundWrapper extends ExtendedInboundHeaders {
    private final Header[] headers;

    public InboundWrapper(HttpMethod response) {
        this.headers = response == null ? null : response.getResponseHeaders();
    }

    @Override
    public String getHeader(String name) {
        if (headers == null || name == null) return null;

        return Arrays.stream(headers)
                .filter(h -> name.equals(h.getName()))
                .map(h -> h.getValue())
                .findFirst()
                .orElse(null);
    }

    @Override
    public List<String> getHeaders(String name) {
        if (headers == null  || name == null) return null;

        List<String> result = Arrays.stream(headers)
                .filter(h -> name.equals(h.getName()))
                .map(h -> h.getValue())
                .collect(Collectors.toList());

        return result.isEmpty() ? null : result;
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.HTTP;
    }
}
