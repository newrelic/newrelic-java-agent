/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.jetty12.ee8.servlet;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import javax.servlet.DispatcherType;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.ee8.nested.AsyncContextEvent;
import org.eclipse.jetty.ee8.nested.Request;

import java.util.logging.Level;

public class JettyRequestListener implements ServletRequestListener  {

    @Override
    public void requestInitialized(ServletRequestEvent sre) {
        boolean isStarted = AgentBridge.getAgent().getTransaction().isStarted();
        Request request = castRequest(sre.getServletRequest());

        boolean startTransaction = !isStarted && request != null;
        if (startTransaction) {
            if (request.getDispatcherType() == DispatcherType.ASYNC) {

                /*
                 * We have to go through the HttpChannelState here to get the AsyncContext because calling
                 * request.getAsyncContext() when the dispatcherType is ASYNC throws an IllegalStateException (Boo, Jetty!)
                 */
                AsyncContextEvent asyncContextEvent = request.getHttpChannelState().getAsyncContextEvent();
                if (asyncContextEvent == null) {
                    AgentBridge.getAgent().getLogger().log(Level.FINE, "AsyncContextEvent is null for request: {0}.", request);
                    return;
                }
                AgentBridge.asyncApi.resumeAsync(asyncContextEvent.getAsyncContext());
            } else {
                HttpServletResponse response = request.getResponse();
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

    private Request castRequest(ServletRequest request) {
        if (request instanceof Request) {
            return (Request) request;
        }
        return null;
    }
}
