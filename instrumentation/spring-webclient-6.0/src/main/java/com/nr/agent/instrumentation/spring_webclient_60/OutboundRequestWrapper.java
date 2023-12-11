/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.spring_webclient_60;

import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.OutboundHeaders;
import org.springframework.web.reactive.function.client.ClientRequest;

/**
 * Wraps Spring 5 Web Client's outbound request headers for CAT.
 */
public class OutboundRequestWrapper implements OutboundHeaders {

    private final ClientRequest.Builder requestBuilder;

    public OutboundRequestWrapper(ClientRequest request) {
        this.requestBuilder = ClientRequest.from(request);
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.HTTP;
    }

    @Override
    public void setHeader(String name, String value) {
        requestBuilder.header(name, value);
    }

    public ClientRequest build() {
        return requestBuilder.build();
    }
}
