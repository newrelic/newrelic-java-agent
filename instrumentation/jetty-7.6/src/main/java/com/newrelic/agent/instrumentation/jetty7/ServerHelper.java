/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.jetty7;

import org.eclipse.jetty.server.AsyncContinuation;
import org.eclipse.jetty.server.Request;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Transaction;

public class ServerHelper {

    public static void preHandle(Request request) {
        AsyncContinuation asyncContext = request.getAsyncContinuation();
        if (asyncContext != null && asyncContext.isResumed()) {
            AgentBridge.asyncApi.resumeAsync(asyncContext);
        } else {
            AgentBridge.getAgent().getTransaction(true).requestInitialized(new JettyRequest(request), new JettyResponse(request.getResponse()));
        }
    }

    public static void postHandle(Request request) {
        AsyncContinuation asyncContext = request.getAsyncContinuation();
        if (asyncContext != null && asyncContext.isAsyncStarted()) {
            AgentBridge.asyncApi.suspendAsync(asyncContext);
        } else {
            // We didn't suspend and we're destroying this request so we should attempt to remove the transaction from
            // the AsyncApiImpl in case this is coming from somewhere other than our AsyncContinuation instrumentation
            AgentBridge.asyncApi.completeAsync(asyncContext);
        }
        AgentBridge.getAgent().getTransaction().requestDestroyed();
    }
}
