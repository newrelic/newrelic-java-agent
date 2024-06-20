/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.jsp4;

import jakarta.servlet.Servlet;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.jsp.JspWriter;
import jakarta.servlet.jsp.PageContext;
import java.io.IOException;
import java.util.Enumeration;

public class PageContextWrapper extends PageContext {
    private final PageContext wrappedContext;
    private final JspWriter wrappedWriter;

    public PageContextWrapper(PageContext wrappedContext, JspWriter wrappedWriter) {
        this.wrappedContext = wrappedContext;
        this.wrappedWriter = wrappedWriter;
    }

    @Override
    public void setAttribute(String name, Object value) {
        wrappedContext.setAttribute(name, value);
    }

    @Override
    public void setAttribute(String name, Object value, int scope) {
        wrappedContext.setAttribute(name, value, scope);
    }

    @Override
    public Object getAttribute(String name) {
        return wrappedContext.getAttribute(name);
    }

    @Override
    public Object getAttribute(String name, int scope) {
        return wrappedContext.getAttribute(name, scope);
    }

    @Override
    public Object findAttribute(String name) {
        return wrappedContext.findAttribute(name);
    }

    @Override
    public void removeAttribute(String name) {
        wrappedContext.removeAttribute(name);
    }

    @Override
    public void removeAttribute(String name, int scope) {
        wrappedContext.removeAttribute(name, scope);
    }

    @Override
    public int getAttributesScope(String name) {
        return wrappedContext.getAttributesScope(name);
    }

    @Override
    public Enumeration<String> getAttributeNamesInScope(int scope) {
        return wrappedContext.getAttributeNamesInScope(scope);
    }

    @Override
    public JspWriter getOut() {
        return wrappedWriter;
    }

    @Override
    public jakarta.el.ELContext getELContext() {
        return wrappedContext.getELContext();
    }

    @Override
    public void initialize(Servlet servlet, ServletRequest request, ServletResponse response, String errorPageURL, boolean needsSession, int bufferSize,
            boolean autoFlush) throws IOException, IllegalStateException, IllegalArgumentException {

    }

    @Override
    public void release() {
        wrappedContext.release();
    }

    @Override
    public HttpSession getSession() {
        return wrappedContext.getSession();
    }

    @Override
    public Object getPage() {
        return wrappedContext.getPage();
    }

    @Override
    public ServletRequest getRequest() {
        return wrappedContext.getRequest();
    }

    @Override
    public ServletResponse getResponse() {
        return wrappedContext.getResponse();
    }

    @Override
    public Exception getException() {
        return wrappedContext.getException();
    }

    @Override
    public ServletConfig getServletConfig() {
        return wrappedContext.getServletConfig();
    }

    @Override
    public ServletContext getServletContext() {
        return wrappedContext.getServletContext();
    }

    @Override
    public void forward(String relativeUrlPath) throws ServletException, IOException {
        wrappedContext.forward(relativeUrlPath);
    }

    @Override
    public void include(String relativeUrlPath) throws ServletException, IOException {
        wrappedContext.include(relativeUrlPath);
    }

    @Override
    public void include(String relativeUrlPath, boolean flush) throws ServletException, IOException {
        wrappedContext.include(relativeUrlPath, flush);
    }

    @Override
    public void handlePageException(Exception e) throws ServletException, IOException {
        wrappedContext.handlePageException(e);
    }

    @Override
    public void handlePageException(Throwable t) throws ServletException, IOException {
        wrappedContext.handlePageException(t);
    }
}
