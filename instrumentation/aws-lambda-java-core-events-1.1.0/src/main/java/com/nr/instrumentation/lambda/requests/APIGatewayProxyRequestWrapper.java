/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.lambda.requests;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.newrelic.api.agent.ExtendedRequest;
import com.newrelic.api.agent.HeaderType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class APIGatewayProxyRequestWrapper extends ExtendedRequest {
    private final APIGatewayProxyRequestEvent event;

    public APIGatewayProxyRequestWrapper(APIGatewayProxyRequestEvent event) {
        super();
        this.event = event;
    }

    @Override
    public String getRequestURI() {
        if (event.getResource() != null) {
            return event.getResource();
        }
        if (event.getPath() != null) {
            return event.getPath();
        }
        if (event.getRequestContext() != null) {
            APIGatewayProxyRequestEvent.ProxyRequestContext requestContext = event.getRequestContext();
            if (requestContext.getResourcePath() != null) {
                return requestContext.getResourcePath();
            }
            return requestContext.getPath();
        }
        return null;
    }

    @Override
    public String getHeader(String name) {
        List<String> values = getHeaders(name);
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.get(0);
    }

    @Override
    public String getRemoteUser() {
        return null;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Enumeration getParameterNames() {
        Map<String, String> parameters = event.getQueryStringParameters();
        if (parameters == null) {
            parameters = Collections.emptyMap();
        }
        Map<String, List<String>> multiValueParams = event.getMultiValueQueryStringParameters();
        if (multiValueParams == null) {
            multiValueParams = Collections.emptyMap();
        }
        Set<String> keys = new HashSet<>();
        keys.addAll(parameters.keySet());
        keys.addAll(multiValueParams.keySet());
        return Collections.enumeration(keys);
    }

    @Override
    public String[] getParameterValues(String name) {
        Map<String, String> parameters = event.getQueryStringParameters();
        if (parameters == null) {
            parameters = Collections.emptyMap();
        }
        Map<String, List<String>> multiValueParams = event.getMultiValueQueryStringParameters();
        if (multiValueParams == null) {
            multiValueParams = Collections.emptyMap();
        }

        if (multiValueParams.containsKey(name)) {
            return multiValueParams.get(name).toArray(new String[0]);
        }

        if (parameters.containsKey(name)) {
            return new String[]{parameters.get(name)};
        }
        return new String[0];
    }

    @Override
    public Object getAttribute(String name) {
        return null;
    }

    @Override
    public String getCookieValue(String name) {
        if (event.getHeaders() == null) {
            return null;
        }
        String cookieHeader = event.getHeaders().get("Cookie");
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
        String method = event.getHttpMethod();
        if (method == null && event.getRequestContext() != null) {
            return event.getRequestContext().getHttpMethod();
        }
        return method;
    }

    @Override
    public List<String> getHeaders(String name) {
        Map<String, String> headers = (event.getHeaders() == null) ? Collections.emptyMap() : event.getHeaders();
        Map<String, List<String>> multiValueHeaders = (event.getMultiValueHeaders() == null) ? Collections.emptyMap() : event.getMultiValueHeaders();

        if (multiValueHeaders.containsKey(name)) {
            return multiValueHeaders.get(name);
        }

        String value = headers.get(name);
        if (value != null) {
            ArrayList<String> values = new ArrayList<>();
            for (String v: value.split("[,;]")) {
                values.add(v.trim());
            }
            return values;
        }
        return null;
    }
}
