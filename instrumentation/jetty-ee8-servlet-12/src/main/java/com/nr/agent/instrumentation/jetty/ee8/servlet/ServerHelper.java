package com.nr.agent.instrumentation.jetty.ee8.servlet;

import org.eclipse.jetty.ee8.nested.Request;

import javax.servlet.ServletRequest;

public class ServerHelper {

    public static final String EXCEPTION_ATTRIBUTE_NAME = "javax.servlet.error.exception";

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
