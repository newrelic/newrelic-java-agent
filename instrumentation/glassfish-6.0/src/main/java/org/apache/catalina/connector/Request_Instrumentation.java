/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.catalina.connector;

import java.util.logging.Level;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.glassfish6.AsyncListenerFactory;
import com.nr.agent.instrumentation.glassfish6.RequestFacadeHelper;

@Weave(originalName = "org.apache.catalina.connector.Request")
public abstract class Request_Instrumentation implements HttpServletRequest {

    @SuppressWarnings("unused")
    private AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse,
            boolean isStartAsyncWithZeroArg) {

        AsyncContext asyncContext = Weaver.callOriginal();

        asyncContext.addListener(AsyncListenerFactory.getAsyncListener(), servletRequest, servletResponse);
        NewRelic.getAgent().getLogger().log(Level.FINER, "Added async listener");

        Request_Instrumentation request = RequestFacadeHelper.getRequest(servletRequest);
        if (request != null) {
            AgentBridge.asyncApi.suspendAsync(request);
        }

        return asyncContext;
    }

    public abstract org.apache.catalina.Response getResponse();

}
