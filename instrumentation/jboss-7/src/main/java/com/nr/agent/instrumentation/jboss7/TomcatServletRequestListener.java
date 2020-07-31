/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.jboss7;

import javax.servlet.AsyncContext;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpServletRequest;

import org.apache.catalina.connector.Request;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.weaver.CatchAndLog;

public final class TomcatServletRequestListener implements ServletRequestListener {

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

        if (httpServletRequest.isAsyncStarted()) {
            AsyncContext asyncContext = httpServletRequest.getAsyncContext();
            if (asyncContext != null) {
                AgentBridge.asyncApi.resumeAsync(asyncContext);
            }
        } else {
            AgentBridge.getAgent().getTransaction(true).requestInitialized(getTomcatRequest(httpServletRequest),
                    getTomcatResponse(httpServletRequest));
        }

    }

    private HttpServletRequest getHttpServletRequest(ServletRequestEvent sre) {
        if (sre.getServletRequest() instanceof HttpServletRequest) {
            return (HttpServletRequest) sre.getServletRequest();
        }
        return null;
    }

    private TomcatRequest getTomcatRequest(HttpServletRequest httpServletRequest) {
        return new TomcatRequest(httpServletRequest);
    }

    private TomcatResponse getTomcatResponse(HttpServletRequest httpServletRequest) {
        Request request = RequestFacadeHelper.getRequest(httpServletRequest);
        if (request != null) {
            return new TomcatResponse(request.getResponse());
        }
        return null;
    }

}