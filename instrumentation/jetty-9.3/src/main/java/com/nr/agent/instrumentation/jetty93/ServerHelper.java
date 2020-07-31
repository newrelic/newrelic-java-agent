/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.jetty93;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import javax.servlet.DispatcherType;
import javax.servlet.http.HttpServletResponse;

import com.newrelic.agent.bridge.AgentBridge;
import org.eclipse.jetty.server.AsyncContextEvent;
import org.eclipse.jetty.server.Request;

public class ServerHelper {

    private static final AtomicBoolean HAS_CONTEXT_HANDLER = new AtomicBoolean(false);

    /**
     * If there is a #ContextHandler, then Jetty is not embedded.
     * 
     * @return true if there is a Jetty #ContextHandler.
     */
    public static boolean hasContextHandler() {
        return HAS_CONTEXT_HANDLER.get();
    }

    public static void contextHandlerFound() {
        if (!HAS_CONTEXT_HANDLER.getAndSet(true)) {
            AgentBridge.getAgent().getLogger().log(Level.FINE, "Detected Jetty ContextHandler");
        }
    }

    public static void preHandle(Request request, HttpServletResponse response) {
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
            AgentBridge.getAgent().getTransaction(true).requestInitialized(new JettyRequest(request),
                    new JettyResponse(response));
        }
    }

    public static void postHandle(Request request) {
        if (request.isAsyncStarted()) {
            AgentBridge.asyncApi.suspendAsync(request.getAsyncContext());
        }
        AgentBridge.getAgent().getTransaction().requestDestroyed();
    }
}
