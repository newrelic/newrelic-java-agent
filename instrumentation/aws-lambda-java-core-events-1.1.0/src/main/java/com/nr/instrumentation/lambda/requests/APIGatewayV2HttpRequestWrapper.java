/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.lambda.requests;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.newrelic.api.agent.ExtendedRequest;
import com.newrelic.api.agent.HeaderType;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;

public class APIGatewayV2HttpRequestWrapper extends ExtendedRequest {
    private APIGatewayV2HTTPEvent event;

    public APIGatewayV2HttpRequestWrapper(APIGatewayV2HTTPEvent event) {
        super();
        this.event = event;
    }

    @Override
    public String getRequestURI() {
        if (event.getRawPath() != null) {
            return event.getRawPath();
        }
        if (event.getRequestContext() != null) {
            APIGatewayV2HTTPEvent.RequestContext requestContext = event.getRequestContext();
            if (requestContext.getHttp() != null) {
                APIGatewayV2HTTPEvent.RequestContext.Http http = requestContext.getHttp();
                if (http != null) {
                    return http.getPath();
                }
            }
        }
        return null;
    }

    @Override
    public String getHeader(String name) {
        Map<String, String> headers = event.getHeaders();
        if (headers == null) {
            return null;
        }
        return event.getHeaders().get(name);
    }

    @Override
    public String getRemoteUser() {
        return null;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Enumeration getParameterNames() {
        Map<String, String> headers = event.getQueryStringParameters();
        if (headers == null) {
            return Collections.emptyEnumeration();
        }
        return Collections.enumeration(headers.keySet());
    }

    @Override
    public String[] getParameterValues(String name) {
        Map<String, String> headers = event.getQueryStringParameters();
        if (headers == null) {
            return new String[0];
        }
        return new String[]{headers.get(name)};
    }

    @Override
    public Object getAttribute(String name) {
        return null;
    }

    @Override
    public String getCookieValue(String name) {
        if (event.getCookies() == null || event.getCookies().isEmpty()) {
            return null;
        }
        for (String cookie: event.getCookies()) {
            int equalsIndx = cookie.indexOf("=");
            if (equalsIndx != -1 && equalsIndx + 1 < cookie.length()) {
                String key = cookie.substring(0, equalsIndx);
                if (name.equals(key)) {
                    return cookie.substring(equalsIndx + 1);
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
        APIGatewayV2HTTPEvent.RequestContext requestContext = event.getRequestContext();
        if (requestContext != null) {
            if (requestContext.getHttp() != null) {
                return requestContext.getHttp().getMethod();
            }
        }
        return null;
    }
}