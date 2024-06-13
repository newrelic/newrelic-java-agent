/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.jsp3;

import jakarta.servlet.jsp.JspContext;
import jakarta.servlet.jsp.JspWriter;
import jakarta.servlet.jsp.el.ExpressionEvaluator;
import jakarta.servlet.jsp.el.VariableResolver;
import java.util.Enumeration;

public class JspContextWrapper extends JspContext {
    private final JspContext wrappedContext;
    private final JspWriter wrappedWriter;

    public JspContextWrapper(JspContext originalContext, JspWriter wrappedWriter) {
        this.wrappedContext = originalContext;
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
    public ExpressionEvaluator getExpressionEvaluator() {
        return wrappedContext.getExpressionEvaluator();
    }

    @Override
    public VariableResolver getVariableResolver() {
        return wrappedContext.getVariableResolver();
    }

    @Override
    public jakarta.el.ELContext getELContext() {
        return wrappedContext.getELContext();
    }
}
