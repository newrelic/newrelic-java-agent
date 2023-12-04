/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.jetty.ee10.servlet;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletRequestEvent;
import jakarta.servlet.ServletRequestListener;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.ee10.servlet.AsyncContextEvent;
import org.eclipse.jetty.ee10.servlet.ServletApiRequest;

import java.util.logging.Level;

public class JettyRequestListener implements ServletRequestListener  {

    @Override
    public void requestInitialized(ServletRequestEvent sre) {
        boolean isStarted = AgentBridge.getAgent().getTransaction().isStarted();
        ServletApiRequest request = castRequest(sre.getServletRequest());

        boolean startTransaction = !isStarted && request != null;
        if (startTransaction) {
            if (request.getDispatcherType() == DispatcherType.ASYNC) {

                /*
                 * We have to go through the ServletRequestInfo and ServletChannelState here to get the AsyncContext because calling
                 * request.getAsyncContext() when the dispatcherType is ASYNC throws an IllegalStateException (Boo, Jetty!)
                 */
                AsyncContextEvent asyncContextEvent = request.getServletRequestInfo().getState().getAsyncContextEvent();
                if (asyncContextEvent == null) {
                    AgentBridge.getAgent().getLogger().log(Level.FINE, "AsyncContextEvent is null for request: {0}.", request);
                    return;
                }
                AgentBridge.asyncApi.resumeAsync(asyncContextEvent.getAsyncContext());
            } else {
                HttpServletResponse response = request.getServletRequestInfo().getServletChannel().getServletContextRequest().getHttpServletResponse();
                AgentBridge.getAgent().getTransaction(true).requestInitialized(
                        new JettyRequest(request),
                        new JettyResponse(response));
            }
        }
    }

    @Override
    public void requestDestroyed(ServletRequestEvent sre) {
        ServletRequest request = sre.getServletRequest();
        if (request.isAsyncStarted()) {
            AgentBridge.asyncApi.suspendAsync(request.getAsyncContext());
        }
        final Throwable throwable = ServerHelper.getRequestError(request);
        if (throwable != null) {
            NewRelic.noticeError(throwable);
        }
        AgentBridge.getAgent().getTransaction().requestDestroyed();
    }

    private ServletApiRequest castRequest(ServletRequest request) {
        if (request instanceof ServletApiRequest) {
            return (ServletApiRequest) request;
        }
        return null;
    }
}
