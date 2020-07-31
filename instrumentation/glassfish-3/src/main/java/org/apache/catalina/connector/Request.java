/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.catalina.connector;

import java.util.logging.Level;

import javax.servlet.AsyncContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.glassfish3.AsyncListenerFactory;
import com.nr.agent.instrumentation.glassfish3.RequestFacadeHelper;

@Weave
public abstract class Request implements HttpServletRequest {

    @SuppressWarnings("unused")
    private AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse,
            boolean isStartAsyncWithZeroArg) {

        AsyncContext asyncContext = Weaver.callOriginal();

        asyncContext.addListener(AsyncListenerFactory.getAsyncListener(), servletRequest, servletResponse);
        NewRelic.getAgent().getLogger().log(Level.FINER, "Added async listener");

        Request request = RequestFacadeHelper.getRequest(servletRequest);
        if (request != null) {
            AgentBridge.asyncApi.suspendAsync(request);
        }

        return asyncContext;
    }

    public abstract org.apache.catalina.Response getResponse();

}
