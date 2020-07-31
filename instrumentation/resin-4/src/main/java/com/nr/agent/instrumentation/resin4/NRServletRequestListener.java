/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.resin4;

import javax.servlet.AsyncContext;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpServletRequest;

import com.caucho.server.http.AbstractCauchoRequest;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.api.agent.Request;
import com.newrelic.api.agent.Response;
import com.newrelic.api.agent.weaver.CatchAndLog;

/**
 * This class handles the initial request. An async dispatch is handled by #ServletInvocation.
 */
public final class NRServletRequestListener implements ServletRequestListener {

    @CatchAndLog
    @Override
    public void requestDestroyed(ServletRequestEvent event) {

        HttpServletRequest request = getHttpServletRequest(event);
        if (request.isAsyncStarted()) {
            AsyncContext asyncContext = request.getAsyncContext();
            if (asyncContext != null) {
                AgentBridge.asyncApi.suspendAsync(asyncContext);
            }
        }
        AgentBridge.getAgent().getTransaction().requestDestroyed();
    }

    @CatchAndLog
    @Override
    public void requestInitialized(ServletRequestEvent event) {

        HttpServletRequest httpServletRequest = getHttpServletRequest(event);
        if (httpServletRequest != null) {
            AgentBridge.getAgent().getTransaction(true).requestInitialized(getRequest(httpServletRequest), getResponse(httpServletRequest));
        }

    }

    private HttpServletRequest getHttpServletRequest(ServletRequestEvent event) {
        if (event.getServletRequest() instanceof HttpServletRequest) {
            return (HttpServletRequest) event.getServletRequest();
        }
        return null;
    }

    private Request getRequest(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        return new RequestWrapper(request);
    }

    private Response getResponse(HttpServletRequest request) {
        AbstractCauchoRequest abstractCauchoRequest = getAbstractCauchoRequest(request);
        if (abstractCauchoRequest == null) {
            return null;
        }
        return new ResponseWrapper(abstractCauchoRequest.getResponse());
    }

    private AbstractCauchoRequest getAbstractCauchoRequest(HttpServletRequest request) {
        if (request instanceof AbstractCauchoRequest) {
            return (AbstractCauchoRequest) request;
        }
        return null;
    }

}