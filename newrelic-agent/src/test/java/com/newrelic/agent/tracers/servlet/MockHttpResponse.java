/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracers.servlet;

import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.Response;

import java.util.HashMap;
import java.util.Map;

public class MockHttpResponse implements Response {

    private int responseStatus;
    private String responseStatusMessage;
    private Map<String, String> headers;

    public MockHttpResponse() {
        headers = new HashMap<>();
    }

    public void setResponseStatus(int responseStatus) {
        this.responseStatus = responseStatus;
    }

    public void setResponseStatusMessage(String responseStatusMessage) {
        this.responseStatusMessage = responseStatusMessage;
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.HTTP;
    }

    @Override
    public int getStatus() throws Exception {
        return responseStatus;
    }

    @Override
    public String getStatusMessage() throws Exception {
        return responseStatusMessage;
    }

    @Override
    public void setHeader(String name, String value) {
        headers.put(name, value);
    }

    public String getHeader(String name) {
        return headers.get(name);
    }

    @Override
    public String getContentType() {
        return null;
    }
}
