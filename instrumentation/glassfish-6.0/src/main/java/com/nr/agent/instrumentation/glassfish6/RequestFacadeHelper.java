/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.glassfish6;

import java.lang.reflect.Field;
import java.util.logging.Level;

import jakarta.servlet.ServletRequest;

import org.apache.catalina.connector.Request_Instrumentation;
import org.apache.catalina.connector.RequestFacade;

import com.newrelic.api.agent.NewRelic;

public class RequestFacadeHelper {

    private static final String REQUEST_FIELD_NAME = "request";

    private static final Field REQUEST_FIELD = getRequestField();

    private RequestFacadeHelper() {

    }

    private static Field getRequestField() {
        try {
            Field field = RequestFacade.class.getDeclaredField(REQUEST_FIELD_NAME);
            field.setAccessible(true);
            return field;
        } catch (Exception e) {
            NewRelic.getAgent().getLogger().log(Level.FINEST, e, "Unable to get the request field from RequestFacade");
        }
        return null;
    }

    private static Request_Instrumentation getRequest(RequestFacade requestFacade) {
        if (REQUEST_FIELD != null) {
            try {
                return (Request_Instrumentation) REQUEST_FIELD.get(requestFacade);
            } catch (Exception e) {
                NewRelic.getAgent().getLogger().log(Level.FINEST, e,
                        "Unable to fetch the request field value from RequestFacade");
            }
        }
        return null;
    }

    public static Request_Instrumentation getRequest(ServletRequest servletRequest) {
        if (servletRequest instanceof Request_Instrumentation) {
            return (Request_Instrumentation) servletRequest;
        } else if (servletRequest instanceof RequestFacade) {
            return getRequest((RequestFacade) servletRequest);
        }
        return null;
    }
}
