/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.jetty91;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.newrelic.agent.bridge.AgentBridge;

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

    public static void preHandle(HttpServletRequest request, HttpServletResponse response) {
        if (request.getDispatcherType() == DispatcherType.ASYNC) {
            AsyncContext asyncContext = request.getAsyncContext();
            if (asyncContext != null) {
                AgentBridge.asyncApi.resumeAsync(asyncContext);
            }
        } else {
            AgentBridge.getAgent().getTransaction(true).requestInitialized(new JettyRequest(request),
                    new JettyResponse(response));
        }
    }

    public static void postHandle(HttpServletRequest request) {
        if (request.isAsyncStarted()) {
            AsyncContext asyncContext = request.getAsyncContext();
            if (asyncContext != null) {
                AgentBridge.asyncApi.suspendAsync(asyncContext);
            }
        }
        AgentBridge.getAgent().getTransaction().requestDestroyed();
    }
}
