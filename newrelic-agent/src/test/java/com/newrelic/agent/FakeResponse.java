/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import java.util.Collection;
import java.util.Collections;

import org.apache.struts.mock.MockHttpServletResponse;

import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.Response;

public class FakeResponse extends MockHttpServletResponse implements Response {

    private int status;

    @Override
    public void setStatus(int status, String message) {
        // super.setStatus(status, message);
        this.status = status;
    }

    @Override
    public void setStatus(int status) {
        // super.setStatus(status);
        this.status = status;
    }

    @Override
    public String getStatusMessage() {
        return null;
    }

    @Override
    public void setHeader(String name, String value) {
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.HTTP;
    }

    // This is the Servlet 3.0 stuff
    @Override
    public int getStatus() {
        return status;
    }

    @Override
    public String getHeader(String name) {
        return null;
    }

    @Override
    public Collection<String> getHeaders(String name) {
        return Collections.emptyList();
    }

    @Override
    public Collection<String> getHeaderNames() {
        return Collections.emptyList();
    }

    @Override
    public String getContentType() {
        return null;
    }

    @Override
    public void setCharacterEncoding(String charset) {

    }

}
