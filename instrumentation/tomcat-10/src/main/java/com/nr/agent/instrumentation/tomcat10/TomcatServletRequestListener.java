/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.tomcat10;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.CatchAndLog;
import jakarta.servlet.ServletRequestEvent;
import jakarta.servlet.ServletRequestListener;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.catalina.connector.RequestFacade;
import org.apache.catalina.connector.Request_Weaved;

import java.lang.reflect.Field;
import java.util.logging.Level;

public final class TomcatServletRequestListener implements ServletRequestListener {

    private static final String SERVLET_EXCEPTION_ATTRIBUTE_NAME = "jakarta.servlet.error.exception";
    private static final String REQUEST_FIELD = "request";

    private final Field requestField;

    public TomcatServletRequestListener() {
        requestField = getRequestField();
    }

    private Field getRequestField() {
        try {
            Field field = RequestFacade.class.getDeclaredField(REQUEST_FIELD);
            field.setAccessible(true);
            return field;
        } catch (Exception e) {
            NewRelic.getAgent().getLogger().log(Level.FINEST, e, "Unable to get the request field from RequestFacade");
        }
        return null;
    }

    @CatchAndLog
    @Override
    public void requestDestroyed(ServletRequestEvent sre) {
        Throwable exception =(Throwable) sre.getServletRequest().getAttribute(SERVLET_EXCEPTION_ATTRIBUTE_NAME);
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

    private TomcatRequest getTomcatRequest(HttpServletRequest httpServletRequest) {
        return new TomcatRequest(httpServletRequest);
    }

    private TomcatResponse getTomcatResponse(HttpServletRequest httpServletRequest) {
        Request_Weaved request = getRequest(httpServletRequest);
        if (request != null) {
            return new TomcatResponse(request.getResponse());
        }
        return null;
    }

    private Request_Weaved getRequest(HttpServletRequest httpServletRequest) {
        if (httpServletRequest instanceof RequestFacade && requestField != null) {
            try {
                return (Request_Weaved) requestField.get(httpServletRequest);
            } catch (Exception e) {
                NewRelic.getAgent().getLogger().log(Level.FINEST, e,
                        "Unable to fetch the request field value from RequestFacade");
            }
        } else if (httpServletRequest instanceof Request_Weaved) {
            return (Request_Weaved) httpServletRequest;
        }
        return null;
    }
}