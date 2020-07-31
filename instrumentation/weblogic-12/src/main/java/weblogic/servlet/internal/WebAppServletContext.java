/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package weblogic.servlet.internal;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.weblogic12.RequestWrapper;
import com.nr.agent.instrumentation.weblogic12.ResponseWrapper;

/**
 * This class handles the initial request. An async dispatch is handled by #AsyncRequestDispatcher.
 */
@Weave
public abstract class WebAppServletContext {

    @Trace(dispatcher = true)
    void execute(ServletRequestImpl req, ServletResponseImpl rsp) {

        HttpServletRequest request = getHttpServletRequest(req);

        if (!AgentBridge.getAgent().getTransaction().isWebRequestSet()) {
            if (request != null) {
                AgentBridge.getAgent().getTransaction().setWebRequest(new RequestWrapper(request));
            }
        }

        if (!AgentBridge.getAgent().getTransaction().isWebResponseSet()) {
            if (req != null) {
                AgentBridge.getAgent().getTransaction().setWebResponse(new ResponseWrapper(req.getResponse()));
            }
        }

        Weaver.callOriginal();

        if (request != null) {
            Throwable exception = (Throwable) request.getAttribute("javax.servlet.error.exception");
            if (exception != null) {
                NewRelic.noticeError(exception);
            }
        }

        if (request != null && request.isAsyncStarted()) {
            AsyncContext asyncContext = request.getAsyncContext();
            if (asyncContext != null) {
                AgentBridge.asyncApi.suspendAsync(asyncContext);
            }
        }
        AgentBridge.getAgent().getTransaction().addOutboundResponseHeaders();
        AgentBridge.getAgent().getTransaction().markResponseSent();
    }

    private HttpServletRequest getHttpServletRequest(ServletRequestImpl req) {
        if (req instanceof HttpServletRequest) {
            return (HttpServletRequest) req;
        }
        return null;
    }

}
