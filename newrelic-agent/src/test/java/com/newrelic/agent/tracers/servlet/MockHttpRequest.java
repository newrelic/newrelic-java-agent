/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracers.servlet;

import com.newrelic.api.agent.ExtendedRequest;
import com.newrelic.api.agent.HeaderType;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;

public class MockHttpRequest extends ExtendedRequest {

    private String requestURI;
    private final Multimap<String, String> headers = LinkedListMultimap.create();
    private final Map<String, String> requestParameters = new HashMap<String, String>();
    private String method;

    @Override
    public String getHeader(String name) {
        return Iterables.getFirst(headers.get(name), null);
    }

    @Override
    public List<String> getHeaders(String name) {
        return new LinkedList<>(headers.get(name));
    }

    public MockHttpRequest setHeader(String name, String header) {
        headers.put(name, header);
        return this;
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.HTTP;
    }

    @Override
    public String getRequestURI() {
        return requestURI;
    }

    public void setRequestURI(String requestURI) throws Exception {
        this.requestURI = requestURI;
    }

    @Override
    public Enumeration getParameterNames() {
        return null;
    }

    @Override
    public String[] getParameterValues(String name) {
        return null;
    }

    @Override
    public Object getAttribute(String name) {
        return null;
    }

    @Override
    public String getRemoteUser() {
        return null;
    }

    @Override
    public String getCookieValue(String name) {
        return null;
    }

    @Override
    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }
}
