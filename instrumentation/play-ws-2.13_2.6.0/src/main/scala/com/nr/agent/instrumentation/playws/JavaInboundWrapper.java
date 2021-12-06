/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.playws;

import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.ExtendedInboundHeaders;
import play.libs.ws.StandaloneWSResponse;

import java.util.List;

public class JavaInboundWrapper extends ExtendedInboundHeaders {

    private final StandaloneWSResponse response;

    public JavaInboundWrapper(StandaloneWSResponse response) {
        this.response = response;
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.HTTP;
    }

    @Override
    public String getHeader(String name) {
        return response.getSingleHeader(name).orElse(null);
    }

    @Override
    public List<String> getHeaders(String name) {
        return response.getHeaderValues(name);
    }
}
