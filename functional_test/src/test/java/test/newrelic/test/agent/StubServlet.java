/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package test.newrelic.test.agent;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.newrelic.agent.AgentHelper;

public abstract class StubServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private String applicationName;
    private String path;

    public StubServlet() throws ServletException, IOException {
        this("", "TestApp", "/test");
    }

    public StubServlet(String appName) throws ServletException, IOException {
        this("", appName, "/test");
    }

    public StubServlet(String contextPath, String applicationName, String path) throws ServletException, IOException {
        this.applicationName = applicationName;
        this.path = path;
        AgentHelper.invokeServlet(this, contextPath, applicationName, path);
    }

    public String getApplicationName() {
        return applicationName;
    }

    public String getPath() {
        return path;
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException,
            IOException {
        try {
            this.run(request, response);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    protected abstract void run(HttpServletRequest request, HttpServletResponse response) throws Exception;
}
