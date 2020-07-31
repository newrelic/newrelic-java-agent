/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import java.io.IOException;
import java.util.Collection;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.apache.struts.mock.MockHttpServletRequest;
import org.apache.struts.mock.MockHttpSession;
import org.apache.struts.mock.MockServletContext;

import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.Request;

public final class FakeRequest extends MockHttpServletRequest implements Request {
    private final String path;

    public FakeRequest(String contextPath, final String applicationName, String servletPath, String pathInfo,
            String queryString, String path) {
        super(contextPath, servletPath, pathInfo, queryString);
        ServletContext servletContext = new MockServletContext() {

            @Override
            public String getServletContextName() {
                return applicationName;
            }

        };
        ((MockHttpSession) this.getSession()).setServletContext(servletContext);
        this.path = path;
    }

    @Override
    public String getHeader(String name) {
        return null;
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.HTTP;
    }

    @Override
    public StringBuffer getRequestURL() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("http://localhost:8080");
        buffer.append(path);
        return buffer;
    }

    @Override
    public boolean authenticate(HttpServletResponse arg0) throws IOException, ServletException {
        return false;
    }

    @Override
    public Part getPart(String arg0) throws IOException, ServletException {
        return null;
    }

    @Override
    public Collection<Part> getParts() throws IOException, ServletException {
        return null;
    }

    @Override
    public void login(String arg0, String arg1) throws ServletException {
    }

    @Override
    public void logout() throws ServletException {
    }

    @Override
    public DispatcherType getDispatcherType() {
        return null;
    }

    @Override
    public String getLocalAddr() {
        return null;
    }

    @Override
    public String getLocalName() {
        return null;
    }

    @Override
    public int getLocalPort() {
        return 0;
    }

    @Override
    public int getRemotePort() {
        return 0;
    }

    @Override
    public ServletContext getServletContext() {
        return null;
    }

    @Override
    public boolean isAsyncStarted() {
        return false;
    }

    @Override
    public boolean isAsyncSupported() {
        return false;
    }

    @Override
    public AsyncContext startAsync() throws IllegalStateException {
        return null;
    }

    @Override
    public AsyncContext startAsync(ServletRequest arg0, ServletResponse arg1) throws IllegalStateException {
        return null;
    }

    @Override
    public String getCookieValue(String name) {
        return null;
    }

}
