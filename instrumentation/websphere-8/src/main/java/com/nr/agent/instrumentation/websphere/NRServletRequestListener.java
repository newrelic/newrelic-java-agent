/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.websphere;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpServletRequest;

import com.ibm.websphere.servlet.request.IRequest;
import com.ibm.websphere.servlet.response.IResponse;
import com.ibm.ws.webcontainer.channel.WCCResponseImpl;
import com.ibm.wsspi.http.channel.HttpResponseMessage;
import com.ibm.wsspi.webcontainer.servlet.IExtendedRequest;
import com.ibm.wsspi.webcontainer.servlet.IExtendedResponse;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.Request;
import com.newrelic.api.agent.Response;
import com.newrelic.api.agent.weaver.CatchAndLog;

public final class NRServletRequestListener implements ServletRequestListener {

    @Override
    @CatchAndLog
    public void requestDestroyed(ServletRequestEvent sre) {

        HttpServletRequest request = getHttpServletRequest(sre);
        if (request != null && request.isAsyncStarted()) {
            AsyncContext asyncContext = request.getAsyncContext();
            if (asyncContext != null) {
                AgentBridge.asyncApi.suspendAsync(asyncContext);
            }
        }

        AgentBridge.getAgent().getTransaction().requestDestroyed();
    }

    @Override
    @CatchAndLog
    public void requestInitialized(ServletRequestEvent sre) {
        HttpServletRequest httpServletRequest = getHttpServletRequest(sre);
        if (httpServletRequest != null) {
            if (httpServletRequest.getDispatcherType() == DispatcherType.ASYNC) {
                AsyncContext asyncContext = httpServletRequest.getAsyncContext();
                if (asyncContext != null) {
                    AgentBridge.asyncApi.resumeAsync(asyncContext);
                    return;
                }
            }
            Response response = getResponse(httpServletRequest);
            AgentBridge.getAgent().getTransaction(true).requestInitialized(getRequest(httpServletRequest), response);
        }
    }

    private HttpServletRequest getHttpServletRequest(ServletRequestEvent sre) {
        if (sre.getServletRequest() instanceof HttpServletRequest) {
            return (HttpServletRequest) sre.getServletRequest();
        }
        return null;
    }

    private Request getRequest(HttpServletRequest httpServletRequest) {
        return new RequestWrapper(httpServletRequest);
    }

    private Response getResponse(HttpServletRequest httpServletRequest) {
        if (httpServletRequest instanceof IRequest) {
            return getResponse((IRequest) httpServletRequest);
        } else if (httpServletRequest instanceof IExtendedRequest) {
            return getResponse((IExtendedRequest) httpServletRequest);
        } else {
            return null;
        }
    }

    private Response getResponse(IRequest request) {
        IResponse wsResponse = request.getWCCResponse();
        if (wsResponse instanceof WCCResponseImpl) {
            HttpResponseMessage httpResponse = ((WCCResponseImpl) wsResponse).getHttpResponse();
            return new ResponseWrapper(httpResponse);
        }
        return null;
    }

    private Response getResponse(IExtendedRequest extendedRequest) {
        IExtendedResponse response = extendedRequest.getResponse();
        return new ExtendedResponseWrapper(response);
    }

}