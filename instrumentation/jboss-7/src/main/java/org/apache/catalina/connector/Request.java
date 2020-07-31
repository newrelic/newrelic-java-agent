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

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.jboss7.AsyncListenerFactory;

@Weave
public abstract class Request implements HttpServletRequest {

    @Override
    public AsyncContext startAsync(ServletRequest request, ServletResponse response) {

        AsyncContext asyncContext = Weaver.callOriginal();

        asyncContext.addListener(AsyncListenerFactory.getAsyncListener());
        NewRelic.getAgent().getLogger().log(Level.FINER, "Added async listener");

        return asyncContext;
    }

    public abstract org.apache.catalina.connector.Response getResponse();

}
