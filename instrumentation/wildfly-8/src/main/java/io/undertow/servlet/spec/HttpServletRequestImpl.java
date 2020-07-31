/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.undertow.servlet.spec;

import java.util.logging.Level;

import javax.servlet.AsyncContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.wildfly8.AsyncListenerFactory;

/*
 * The http servlet request implementation. This class is not thread safe
 */
@Weave
public abstract class HttpServletRequestImpl implements HttpServletRequest {

    @Override
    public AsyncContext startAsync() throws IllegalStateException {
        AsyncContext asyncContext = Weaver.callOriginal();

        asyncContext.addListener(AsyncListenerFactory.getAsyncListener());
        NewRelic.getAgent().getLogger().log(Level.FINER, "Added async listener");

        return asyncContext;
    }

    @Override
    public AsyncContext startAsync(final ServletRequest servletRequest, final ServletResponse servletResponse)
            throws IllegalStateException {
        AsyncContext asyncContext = Weaver.callOriginal();

        asyncContext.addListener(AsyncListenerFactory.getAsyncListener());
        NewRelic.getAgent().getLogger().log(Level.FINER, "Added async listener");

        return asyncContext;
    }

    public abstract io.undertow.server.HttpServerExchange getExchange();

    public abstract AsyncContextImpl getAsyncContextInternal();

}
