/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.eclipse.jetty.ee10.servlet;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.jetty12.ee10.servlet.AsyncListenerFactory;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.util.logging.Level;

@Weave(type = MatchType.ExactClass, originalName = "org.eclipse.jetty.ee10.servlet.ServletApiRequest")
public abstract class ServletApiRequest_Instrumentation implements HttpServletRequest {
    @Override
    public AsyncContext startAsync(ServletRequest request, ServletResponse response) {
        AsyncContext asyncContext = Weaver.callOriginal();

        asyncContext.addListener(AsyncListenerFactory.getAsyncListener());
        AgentBridge.getAgent().getLogger().log(Level.FINER, "Added async listener");

        return asyncContext;
    }

    @Override
    public AsyncContext startAsync() {

        AsyncContext asyncContext = Weaver.callOriginal();

        asyncContext.addListener(AsyncListenerFactory.getAsyncListener());
        AgentBridge.getAgent().getLogger().log(Level.FINER, "Added async listener");

        return asyncContext;
    }

}
