/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.websphere;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.servlet.event.ServletInvocationEvent;
import com.ibm.websphere.servlet.event.ServletInvocationListener;
import com.newrelic.agent.bridge.AgentBridge;

public final class NRServletRequestListener implements ServletInvocationListener {

    @Override
    public void onServletFinishService(ServletInvocationEvent sre) {
        HttpServletRequest request = sre.getRequest();
        if (request != null && request.isAsyncStarted()) {
            AsyncContext asyncContext = request.getAsyncContext();
            if (asyncContext != null) {
                AgentBridge.asyncApi.suspendAsync(asyncContext);
            }
        }

        AgentBridge.getAgent().getTransaction().requestDestroyed();
    }

    @Override
    public void onServletStartService(ServletInvocationEvent sre) {
        HttpServletRequest request = sre.getRequest();
        if (request != null) {
            if (request.getDispatcherType() == DispatcherType.ASYNC) {
                AsyncContext asyncContext = request.getAsyncContext();
                if (asyncContext != null) {
                    AgentBridge.asyncApi.resumeAsync(asyncContext);
                    return;
                }
            }

            HttpServletResponse response = sre.getResponse();
            AgentBridge.getAgent().getTransaction(true).requestInitialized(new RequestWrapper(request),
                    new ResponseWrapper(response));
        }
    }
}
