/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package weblogic.servlet.internal;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncListener;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.weblogic12.AsyncListenerFactory;

@Weave
public abstract class ServletRequestImpl {

    public AsyncContext startAsync(ServletRequest request, ServletResponse response) {

        AsyncContext asyncContext = Weaver.callOriginal();

        AsyncListener asyncListener = AsyncListenerFactory.getAsyncListener();
        asyncContext.addListener(asyncListener);

        return asyncContext;
    }

    public abstract weblogic.servlet.internal.ServletResponseImpl getResponse();
}
