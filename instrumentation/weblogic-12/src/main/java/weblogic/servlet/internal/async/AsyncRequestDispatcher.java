/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package weblogic.servlet.internal.async;

import javax.servlet.AsyncContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

/**
 * This class handles an async dispatch. #WebAppServletContext handles the initial request.
 */
@Weave
class AsyncRequestDispatcher {

    void dispatch(ServletRequest req, ServletResponse resp) {

        AsyncContext asyncContext = req.getAsyncContext();
        if (asyncContext != null) {
            AgentBridge.asyncApi.resumeAsync(asyncContext);
        }

        Weaver.callOriginal();

        if (req.isAsyncStarted()) {
            asyncContext = req.getAsyncContext();
            if (asyncContext != null) {
                AgentBridge.asyncApi.suspendAsync(asyncContext);
            }
        }

        AgentBridge.asyncApi.finishRootTracer();

    }
}
