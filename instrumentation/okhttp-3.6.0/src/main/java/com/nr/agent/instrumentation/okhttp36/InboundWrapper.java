/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.okhttp36;

import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.ExtendedInboundHeaders;
import okhttp3.Response;

import java.util.List;

public class InboundWrapper extends ExtendedInboundHeaders {

    private final Response response;

    public InboundWrapper(Response response) {
        this.response = response;
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.HTTP;
    }

    @Override
    public String getHeader(String name) {
        return response.header(name);
    }

    @Override
    public List<String> getHeaders(String name) {
        return response.headers(name);
    }

}
