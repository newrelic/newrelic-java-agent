/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.catalina.core;

import javax.servlet.DispatcherType;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.catalina.connector.Request;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.glassfish3.RequestFacadeHelper;

@Weave
public class ApplicationDispatcher {

    /**
     * If this is an async dispatch there are no #ServletRequestListener calls, so handle async here.
     * 
     * {@link ServletRequest#getAsyncContext()} throws an #IllegalStateException, and the asyncContext field in #Request
     * is null, so use the #ServletRequest to find the suspended transaction.
     * 
     * The transaction is suspended in {@link Request#startAsync(ServletRequest, ServletResponse, boolean)}
     */
    public void dispatch(ServletRequest servletRequest, ServletResponse servletResponse, DispatcherType dispatcherType) {

        boolean isAsyncDispatch = dispatcherType == DispatcherType.ASYNC;
        if (isAsyncDispatch) {
            Request request = RequestFacadeHelper.getRequest(servletRequest);
            if (request != null) {
                AgentBridge.asyncApi.resumeAsync(request);
            }
        }

        Weaver.callOriginal();

        if (isAsyncDispatch) {
            if (servletRequest.isAsyncStarted()) {
                Request request = RequestFacadeHelper.getRequest(servletRequest);
                if (request != null) {
                    AgentBridge.asyncApi.suspendAsync(request);
                }
            }
            AgentBridge.getAgent().getTransaction().requestDestroyed();
        }
    }

}
