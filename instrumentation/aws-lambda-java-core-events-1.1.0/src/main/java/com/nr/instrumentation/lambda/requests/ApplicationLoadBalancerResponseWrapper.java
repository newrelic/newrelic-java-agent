/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.lambda.requests;

import com.amazonaws.services.lambda.runtime.events.ApplicationLoadBalancerResponseEvent;
import com.newrelic.api.agent.ExtendedResponse;
import com.newrelic.api.agent.HeaderType;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

public class ApplicationLoadBalancerResponseWrapper extends ExtendedResponse {
    private final ApplicationLoadBalancerResponseEvent response;

    public ApplicationLoadBalancerResponseWrapper(ApplicationLoadBalancerResponseEvent msg) {
        this.response = msg;
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.HTTP;
    }

    @Override
    public void setHeader(String name, String value) {
        response.setHeaders(Collections.singletonMap(name, value));
    }

    @Override
    public int getStatus() throws Exception {
        return response.getStatusCode();
    }

    @Override
    public String getStatusMessage() throws Exception {
        return response.getStatusDescription();
    }

    @Override
    public String getContentType() {
        return response.getHeaders().get("Content-Type");
    }

    @Override
    public long getContentLength() {
        try {
            String contentLengthHeader = response.getHeaders().get("Content-Length");
            return Long.parseLong(contentLengthHeader);
        } catch (NumberFormatException e) {
            return response.getBody().getBytes(StandardCharsets.UTF_8).length;
        }
    }
}
