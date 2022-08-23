/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.glassfish6;

import jakarta.servlet.ServletRequestEvent;
import jakarta.servlet.ServletRequestListener;
import jakarta.servlet.http.HttpServletRequest;

import org.apache.catalina.connector.Request_Instrumentation;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.weaver.CatchAndLog;
import org.apache.catalina.core.ApplicationDispatcher_Instrumentation;

/**
 * This class handles the initial request. An async dispatch is handled by
 * {@link ApplicationDispatcher_Instrumentation}.
 */
public final class GlassfishServletRequestListener implements ServletRequestListener {

    private static final String EXCEPTION_ATTRIBUTE_NAME = "jakarta.servlet.error.exception";

    @CatchAndLog
    @Override
    public void requestDestroyed(ServletRequestEvent sre) {
        Throwable exception = (Throwable) sre.getServletRequest().getAttribute(EXCEPTION_ATTRIBUTE_NAME);
        if (exception != null) {
            AgentBridge.privateApi.reportException(exception);
        }

        AgentBridge.getAgent().getTransaction().requestDestroyed();
    }

    @CatchAndLog
    @Override
    public void requestInitialized(ServletRequestEvent sre) {
        HttpServletRequest httpServletRequest = getHttpServletRequest(sre);
        if (httpServletRequest == null) {
            return;
        }

        AgentBridge.getAgent().getTransaction(true).requestInitialized(getTomcatRequest(httpServletRequest),
                getTomcatResponse(httpServletRequest));
    }

    private HttpServletRequest getHttpServletRequest(ServletRequestEvent sre) {
        if (sre.getServletRequest() instanceof HttpServletRequest) {
            return (HttpServletRequest) sre.getServletRequest();
        }
        return null;
    }

    private GlassfishRequest getTomcatRequest(HttpServletRequest httpServletRequest) {
        return new GlassfishRequest(httpServletRequest);
    }

    private GlassfishResponse getTomcatResponse(HttpServletRequest httpServletRequest) {
        Request_Instrumentation request = RequestFacadeHelper.getRequest(httpServletRequest);
        if (request != null) {
            return new GlassfishResponse(request.getResponse());
        }
        return null;
    }

}
