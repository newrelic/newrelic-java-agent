/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.glassfish3;

import java.lang.reflect.Field;
import java.util.logging.Level;

import javax.servlet.ServletRequest;

import org.apache.catalina.connector.Request;
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

    private static Request getRequest(RequestFacade requestFacade) {
        if (REQUEST_FIELD != null) {
            try {
                return (Request) REQUEST_FIELD.get(requestFacade);
            } catch (Exception e) {
                NewRelic.getAgent().getLogger().log(Level.FINEST, e,
                        "Unable to fetch the request field value from RequestFacade");
            }
        }
        return null;
    }

    public static Request getRequest(ServletRequest servletRequest) {
        if (servletRequest instanceof Request) {
            return (Request) servletRequest;
        } else if (servletRequest instanceof RequestFacade) {
            return getRequest((RequestFacade) servletRequest);
        }
        return null;
    }
}
