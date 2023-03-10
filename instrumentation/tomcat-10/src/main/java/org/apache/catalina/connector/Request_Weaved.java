/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.catalina.connector;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.tomcat10.AsyncListenerFactory;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.catalina.core.AsyncContextImpl_Weaved;

import java.util.logging.Level;

@Weave(originalName = "org.apache.catalina.connector.Request")
public abstract class Request_Weaved implements HttpServletRequest {

    @Override
    public AsyncContext startAsync(ServletRequest request, ServletResponse response) {

        AsyncContext asyncContext = Weaver.callOriginal();

        asyncContext.addListener(AsyncListenerFactory.getAsyncListener());
        NewRelic.getAgent().getLogger().log(Level.FINER, "Added async listener");

        return asyncContext;
    }

    public AsyncContextImpl_Weaved getAsyncContextInternal() {
        return Weaver.callOriginal();
    }

    public abstract org.apache.catalina.connector.Response getResponse();

}
