/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.jetty.ee10.servlet;

import jakarta.servlet.ServletRequest;
import org.eclipse.jetty.server.Request;

public class ServerHelper {

    public static final String EXCEPTION_ATTRIBUTE_NAME = "jakarta.servlet.error.exception";

    public static Throwable getRequestError(ServletRequest request) {
        if (request == null) {
            return null;
        }

        final Object obj = request.getAttribute(ServerHelper.EXCEPTION_ATTRIBUTE_NAME);

        return castObjectToThrowable(obj);
    }

    public static Throwable getRequestError(Request request) {
        if (request == null) {
            return null;
        }

        final Object obj = request.getAttribute(ServerHelper.EXCEPTION_ATTRIBUTE_NAME);
        return castObjectToThrowable(obj);
    }

    private static Throwable castObjectToThrowable(Object obj) {
        if (obj instanceof Throwable) {
            return (Throwable) obj;
        }
        return null;
    }

}
