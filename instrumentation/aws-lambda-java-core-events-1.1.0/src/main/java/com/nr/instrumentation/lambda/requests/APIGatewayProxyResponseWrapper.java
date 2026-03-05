/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.lambda.requests;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.newrelic.api.agent.ExtendedResponse;
import com.newrelic.api.agent.HeaderType;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class APIGatewayProxyResponseWrapper extends ExtendedResponse {
    private final APIGatewayProxyResponseEvent response;

    public APIGatewayProxyResponseWrapper(APIGatewayProxyResponseEvent msg) {
        this.response = msg;
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.HTTP;
    }

    @Override
    public void setHeader(String name, String value) {
        Map<String, String> headers = (response.getHeaders() == null) ? new HashMap<>() : new HashMap<>(response.getHeaders());
        headers.put(name, value);

        response.setHeaders(headers);
    }

    @Override
    public int getStatus() throws Exception {
        return response.getStatusCode();
    }

    @Override
    public String getStatusMessage() throws Exception {
        return null;
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
                return response.getBody() == null ? 0 : response.getBody().getBytes(StandardCharsets.UTF_8).length;
            }
            String contentLengthHeader = response.getHeaders().get("Content-Length");
            return Long.parseLong(contentLengthHeader);
        } catch (NumberFormatException e) {
            return response.getBody().getBytes(StandardCharsets.UTF_8).length;
        }
    }
}
