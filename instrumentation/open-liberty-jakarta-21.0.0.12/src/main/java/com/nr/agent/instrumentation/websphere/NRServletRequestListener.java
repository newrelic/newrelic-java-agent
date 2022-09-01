/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.websphere;

import com.ibm.websphere.servlet.event.ServletInvocationEvent;
import com.ibm.websphere.servlet.event.ServletInvocationListener;
import com.newrelic.agent.bridge.AgentBridge;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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
