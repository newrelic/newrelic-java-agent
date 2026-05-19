/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.instrumentation.labs.ktor.jetty;

import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.Headers;
import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.ArrayList;

public class KtorJettyHeaders implements Headers {
    
    private final HttpServletRequest request;
    
    public KtorJettyHeaders(HttpServletRequest req) {
        this.request = req;
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.HTTP;
    }

    @Override
    public String getHeader(String name) {
        return request.getHeader(name);
    }

    @Override
    public Collection<String> getHeaders(String name) {
        Enumeration<String> headers = request.getHeaders(name);
        if (headers != null) {
            ArrayList<String> headerList = new ArrayList<>();
            while (headers.hasMoreElements()) {
                headerList.add(headers.nextElement());
            }
            return headerList;
        }
        return Collections.emptyList();
    }

    @Override
    public void setHeader(String name, String value) {
        // Not applicable for request headers
    }

    @Override
    public void addHeader(String name, String value) {
        // Not applicable for request headers
    }

    @Override
    public Collection<String> getHeaderNames() {
        Enumeration<String> headerNames = request.getHeaderNames();
        if (headerNames != null) {
            ArrayList<String> names = new ArrayList<>();
            while (headerNames.hasMoreElements()) {
                names.add(headerNames.nextElement());
            }
            return names;
        }
        return Collections.emptyList();
    }

    @Override
    public boolean containsHeader(String name) {
        return request.getHeader(name) != null;
    }
}