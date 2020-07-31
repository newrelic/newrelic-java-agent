/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.caucho.server.dispatch;

import java.io.IOException;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave
public class ServletInvocation {

    /**
     * If this is an async dispatch there are no #ServletRequestListener calls, so handle async here.
     */
    public void service(ServletRequest request, ServletResponse response) throws IOException, ServletException {

        boolean isAsyncDispatch = request.getDispatcherType() == DispatcherType.ASYNC;
        if (isAsyncDispatch) {
            AsyncContext asyncContext = request.getAsyncContext();
            AgentBridge.asyncApi.resumeAsync(asyncContext);
        }

        Weaver.callOriginal();

        if (isAsyncDispatch) {
            if (request.isAsyncStarted()) {
                AsyncContext asyncContext = request.getAsyncContext();
                if (asyncContext != null) {
                    AgentBridge.asyncApi.suspendAsync(asyncContext);
                }
            }
            AgentBridge.getAgent().getTransaction().requestDestroyed();
        }
    }

}
