/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.spring_webclient;

import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.OutboundHeaders;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Wraps Spring 5 Web Client's outbound request headers for CAT.
 */
public class OutboundRequestWrapper implements OutboundHeaders {

    private final WebClient.RequestHeadersSpec requestHeaderSpec;

    public OutboundRequestWrapper(WebClient.RequestHeadersSpec requestBuilder) {
        this.requestHeaderSpec = requestBuilder;
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.HTTP;
    }

    @Override
    public void setHeader(String name, String value) {
        requestHeaderSpec.header(name, value);
    }
}
