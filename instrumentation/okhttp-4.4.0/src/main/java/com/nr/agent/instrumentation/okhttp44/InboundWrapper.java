/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.okhttp44;

import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.ExtendedInboundHeaders;
import okhttp3.Headers;
import okhttp3.Response;

import java.util.List;

public class InboundWrapper extends ExtendedInboundHeaders {

    private final Headers headers;

    public InboundWrapper(Response response) {
        this.headers = response == null ? null : response.headers();
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.HTTP;
    }

    @Override
    public String getHeader(String name) {
        return headers.get(name);
    }

    @Override
    public List<String> getHeaders(String name) {
        return headers.values(name);
    }

}
