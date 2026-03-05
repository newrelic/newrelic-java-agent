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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        Map<String, String> headers = (response.getHeaders() == null) ? new HashMap<>() : new HashMap<>(response.getHeaders());
        Map<String, List<String>> multiValueHeaders = (response.getMultiValueHeaders() == null) ? new HashMap<>() : new HashMap<>(response.getMultiValueHeaders());
        if (!headers.containsKey(name)) {
            headers.put(name, value);
        } else {
            String originalValue = headers.get(name);
            headers.put(name, originalValue + ", " + value);

            if (!multiValueHeaders.containsKey(name)) {
                ArrayList<String> values = new ArrayList<>();
                values.add(originalValue);
                values.add(value);
                multiValueHeaders.put(name, values);
            } else {
                multiValueHeaders.get(name).add(value);
            }
        }

        response.setHeaders(headers);
        response.setMultiValueHeaders(multiValueHeaders);
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
        if (response.getHeaders() != null) {
            return response.getHeaders().get("Content-Type");
        }
        return null;
    }

    @Override
    public long getContentLength() {
        try {
            if (response.getHeaders() == null) {
                return response.getBody() == null ? 0 : response.getBody().length();
            }
            String contentLengthHeader = response.getHeaders().get("Content-Length");
            return Long.parseLong(contentLengthHeader);
        } catch (NumberFormatException e) {
            return response.getBody().getBytes(StandardCharsets.UTF_8).length;
        }
    }
}
