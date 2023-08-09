/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.wildfly27;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.weaver.CatchAndLog;
import io.undertow.server.HttpServerExchange;
import io.undertow.servlet.handlers.ServletRequestContext;
import io.undertow.servlet.spec.AsyncContextImpl;
import io.undertow.servlet.spec.HttpServletRequestImpl;
import io.undertow.servlet.spec.HttpServletResponseImpl;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletRequestEvent;
import jakarta.servlet.ServletRequestListener;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

public final class WildflyServletRequestListener implements ServletRequestListener {

    private static final String EXCEPTION_ATTRIBUTE_NAME = "javax.servlet.error.exception";

    @CatchAndLog
    @Override
    public void requestDestroyed(ServletRequestEvent sre) {

        Throwable exception = (Throwable) sre.getServletRequest().getAttribute(EXCEPTION_ATTRIBUTE_NAME);
        if (exception != null) {
            AgentBridge.privateApi.reportException(exception);
        }

        HttpServletRequest httpServletRequest = getHttpServletRequest(sre);
        if (httpServletRequest == null) {
            AgentBridge.getAgent().getTransaction().requestDestroyed();
            return;
        }

        if (httpServletRequest.isAsyncStarted()) {
            AsyncContext asyncContext = httpServletRequest.getAsyncContext();
            if (asyncContext != null) {
                AgentBridge.asyncApi.suspendAsync(asyncContext);
            }
        }

        AgentBridge.getAgent().getTransaction().requestDestroyed();
    }

    @CatchAndLog
    @Override
    public void requestInitialized(ServletRequestEvent sre) {
        HttpServletRequest httpServletRequest = getHttpServletRequest(sre);
        if (httpServletRequest == null) {
            return;
        }

        // the isAsyncStarted method is set to false when the dispatcher is started and so we can not check that flag
        if (httpServletRequest instanceof HttpServletRequestImpl) {
            // must use asyncContextInternal because getAsyncContext checks isAsyncStarted which is false
            AsyncContext context = ((HttpServletRequestImpl) httpServletRequest).getAsyncContextInternal();
            if (context instanceof AsyncContextImpl && (((AsyncContextImpl) context).isDispatched())) {
                AgentBridge.asyncApi.resumeAsync(context);
                return;
            }
        }
        AgentBridge.getAgent().getTransaction(true).requestInitialized(getWildflyRequest(httpServletRequest),
                getWildflyResponse(httpServletRequest));

    }

    private HttpServletRequest getHttpServletRequest(ServletRequestEvent sre) {
        if (sre.getServletRequest() instanceof HttpServletRequest) {
            return (HttpServletRequest) sre.getServletRequest();
        }
        return null;
    }

    private WildflyRequest getWildflyRequest(HttpServletRequest httpServletRequest) {
        return new WildflyRequest(httpServletRequest);
    }

    private WildflyResponse getWildflyResponse(HttpServletRequest httpServletRequest) {
        if (httpServletRequest instanceof HttpServletRequestImpl) {
            HttpServerExchange exchange = ((HttpServletRequestImpl) httpServletRequest).getExchange();
            ServletRequestContext servletRequestContext = exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);
            if (servletRequestContext != null) {
                ServletResponse response = servletRequestContext.getServletResponse();
                if (response instanceof HttpServletResponseImpl) {
                    return new WildflyResponse((HttpServletResponseImpl) response);
                }
            }
        }
        return null;
    }

}