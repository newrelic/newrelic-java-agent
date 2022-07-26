/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.glassfish.jersey.server;

import java.security.Principal;
import java.util.Enumeration;
import java.util.List;

import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.SecurityContext;

import com.newrelic.api.agent.ExtendedRequest;
import com.newrelic.api.agent.HeaderType;

public class RequestImpl extends ExtendedRequest {

    private final ContainerRequest request;

    public RequestImpl(ContainerRequest request) {
        this.request = request;
    }

    @Override
    public String getRequestURI() {
        return request.getBaseUri().getPath();
    }

    @Override
    public String getHeader(String name) {
        return request.getHeaderString(name);
    }

    @Override
    public String getRemoteUser() {
        SecurityContext securityContext = request.getSecurityContext();
        if (securityContext != null) {
            Principal userPrincipal = securityContext.getUserPrincipal();
            if (userPrincipal != null) {
                return userPrincipal.getName();
            }
        }
        return null;
    }

    @SuppressWarnings("rawtypes")
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
    public String getCookieValue(String name) {
        Cookie c = request.getCookies().get(name);
        return c == null ? null : c.getValue();
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.HTTP;
    }

    @Override
    public String getMethod() {
        return request.getMethod();
    }

    @Override
    public List<String> getHeaders(String name) {
        return request.getHeaders().get(name);
    }
}
