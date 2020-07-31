/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.resin3;

import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpServletRequest;

import com.caucho.server.connection.AbstractHttpRequest;
import com.caucho.server.connection.AbstractHttpResponse;
import com.caucho.server.connection.CauchoResponse;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.Request;
import com.newrelic.api.agent.Response;
import com.newrelic.api.agent.weaver.CatchAndLog;

public final class NRServletRequestListener implements ServletRequestListener {

    @CatchAndLog
    @Override
    public void requestDestroyed(ServletRequestEvent sre) {

        AgentBridge.getAgent().getTransaction().requestDestroyed();
    }

    @CatchAndLog
    @Override
    public void requestInitialized(ServletRequestEvent sre) {

        HttpServletRequest request = getHttpServletRequest(sre);
        if (request != null) {
            AgentBridge.getAgent().getTransaction(true).requestInitialized(getRequest(request), getResponse(request));
        }
    }

    private HttpServletRequest getHttpServletRequest(ServletRequestEvent sre) {
        if (sre.getServletRequest() instanceof HttpServletRequest) {
            return (HttpServletRequest) sre.getServletRequest();
        }
        return null;
    }

    private Response getResponse(HttpServletRequest request) {
        AbstractHttpRequest abstractHttpRequest = getAbstractHttpRequest(request);
        if (abstractHttpRequest == null) {
            return null;
        }
        AbstractHttpResponse response = getAbstractHttpResponse(abstractHttpRequest);
        return new ResponseWrapper(response);
    }

    private AbstractHttpRequest getAbstractHttpRequest(HttpServletRequest request) {
        if (request instanceof AbstractHttpRequest) {
            return (AbstractHttpRequest) request;
        }
        return null;
    }

    private AbstractHttpResponse getAbstractHttpResponse(AbstractHttpRequest request) {
        CauchoResponse cauchoResponse = request.getResponse();
        if (cauchoResponse instanceof AbstractHttpResponse) {
            return (AbstractHttpResponse) cauchoResponse;
        }
        return null;
    }

    private Request getRequest(HttpServletRequest request) {
        return new RequestWrapper(request);
    }

}