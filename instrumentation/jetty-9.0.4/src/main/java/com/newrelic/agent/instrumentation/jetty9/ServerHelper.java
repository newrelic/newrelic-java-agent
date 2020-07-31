/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.jetty9;

import javax.servlet.AsyncContext;

import org.eclipse.jetty.server.Request;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Transaction;

public class ServerHelper {

    public static void preHandle(Request request) {
        /*
         * In 9.0.3 Request.isAsyncStarted() calls HttpChannelState.isAsyncStarted() which returns true only if the
         * async state is ASYNCSTARTED or ASYNCWAIT. In 9.0.4 Request.isAsyncStarted() calls HttpChannelState.isAsync()
         * which returns true if the async state is ASYNCSTARTED, REDISPATCHING, ASYNCWAIT, REDISPATCHED, REDISPATCH,
         * COMPLETECALLED. Since HttpChannelState.isAsync() is also available in 9.0.3 use it instead.
         */
        if (request.getHttpChannelState().isAsync()) {
            AsyncContext asyncContext = request.getAsyncContext();
            AgentBridge.asyncApi.resumeAsync(asyncContext);
        } else {
            AgentBridge.getAgent().getTransaction(true).requestInitialized(new JettyRequest(request), new JettyResponse(request.getResponse()));
        }
    }

    public static void postHandle(Request request) {
        /*
         * According to the Servlet 3.0 spec, ServletRequest.isAsyncStarted should return false if the request has been
         * dispatched using one of the AsyncContext.dispatch methods, but org.eclipse.jetty.server.Request returns true.
         */
        if (request.getHttpChannelState().isAsync() && !request.getHttpChannelState().isDispatched()) {
            AsyncContext asyncContext = request.getAsyncContext();
            if (asyncContext != null) {
                AgentBridge.asyncApi.suspendAsync(asyncContext);
            }
        }
        AgentBridge.getAgent().getTransaction().requestDestroyed();
    }
}
