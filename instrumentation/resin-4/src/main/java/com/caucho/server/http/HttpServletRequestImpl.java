/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.caucho.server.http;

import java.util.logging.Level;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncListener;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.resin4.AsyncListenerFactory;

@Weave
public class HttpServletRequestImpl {

    public AsyncContext startAsync(ServletRequest request, ServletResponse response) {

        AsyncContext asyncContext = Weaver.callOriginal();

        AsyncListener asyncListener = AsyncListenerFactory.getAsyncListener();
        asyncContext.addListener(asyncListener);
        NewRelic.getAgent().getLogger().log(Level.FINER, "Add async listener: {0}", asyncListener.getClass());

        return asyncContext;
    }

}
