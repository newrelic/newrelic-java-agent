/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.util;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class MockServletConfig implements ServletConfig {

    private String servletName;
    private Map<String, String> initParameters = new HashMap<>();
    private ServletContext servletContext = new MockServletContext();

    @Override
    public String getServletName() {
        return servletName;
    }

    public MockServletConfig setServletName(String servletName) {
        this.servletName = servletName;
        return this;
    }

    @Override
    public String getInitParameter(String key) {
        return initParameters.get(key);
    }

    public void setInitParameter(String key, String value) {
        initParameters.put(key, value);
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        return null;
    }

    @Override
    public ServletContext getServletContext() {
        return servletContext;
    }

    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

}
