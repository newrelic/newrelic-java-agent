/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.lambda.requests;

import com.amazonaws.services.lambda.runtime.events.ApplicationLoadBalancerRequestEvent;
import com.newrelic.api.agent.ExtendedRequest;
import com.newrelic.api.agent.HeaderType;

import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

public class ApplicationLoadBalancerRequestWrapper extends ExtendedRequest {
    private final ApplicationLoadBalancerRequestEvent event;

    public ApplicationLoadBalancerRequestWrapper(ApplicationLoadBalancerRequestEvent event) {
        super();
        this.event = event;
    }

    @Override
    public String getRequestURI() {
        return event.getPath();
    }

    @Override
    public String getHeader(String name) {
        if (event.getHeaders() != null) {
            return event.getHeaders().get(name);
        }
        return null;
    }

    @Override
    public String getRemoteUser() {
        return null;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Enumeration getParameterNames() {
        if (event.getQueryStringParameters() == null) {
            return Collections.emptyEnumeration();
        }
        return Collections.enumeration(event.getQueryStringParameters().keySet());
    }

    @Override
    public String[] getParameterValues(String name) {
        if (event.getQueryStringParameters() == null) {
            return new String[0];
        }
        return new String[]{event.getQueryStringParameters().get(name)};
    }

    @Override
    public Object getAttribute(String name) {
        return null;
    }

    @Override
    public String getCookieValue(String name) {
        Map<String, String> headers = event.getHeaders();
        if (headers == null) {
            return null;
        }
        String cookieHeader = headers.get("Cookie");
        if (cookieHeader != null) {
            for (String cookie: cookieHeader.split(";")) {
                int equalsIndx = cookie.indexOf("=");
                if (equalsIndx != -1 && equalsIndx + 1 < cookie.length()) {
                    String key = cookie.substring(0, equalsIndx);
                    if (name.equals(key.trim())) {
                        return cookie.substring(equalsIndx + 1).trim();
                    }
                }
            }
        }
        return null;
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.HTTP;
    }

    @Override
    public String getMethod() {
        return event.getHttpMethod();
    }

    @Override
    public List<String> getHeaders(String name) {
        if (event.getHeaders() == null) {
            return Collections.emptyList();
        }

        return Collections.singletonList(event.getHeaders().get(name));
    }
}
