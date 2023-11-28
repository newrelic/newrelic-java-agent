package com.nr.agent.instrumentation.jetty.ee10.servlet;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.ee10.servlet.AsyncContextEvent;
import org.eclipse.jetty.ee10.servlet.ServletChannelState;
import org.eclipse.jetty.ee10.servlet.ServletContextRequest;
import org.eclipse.jetty.server.Request;

import java.util.logging.Level;

public class ServerHelper {

    public static final String EXCEPTION_ATTRIBUTE_NAME = "jakarta.servlet.error.exception";

    public static void preHandleDispatch(ServletContextRequest servletContextRequest) {
        HttpServletRequest request = servletContextRequest.getServletApiRequest();
        HttpServletResponse response = servletContextRequest.getHttpServletResponse();
        AgentBridge.getAgent().getTransaction(true).requestInitialized(new JettyRequest(request),
                new JettyResponse(response));
    }

    public static void preHandleDispatchAsync(ServletContextRequest servletContextRequest, ServletChannelState state) {
        HttpServletRequest request = servletContextRequest.getServletApiRequest();
        AsyncContextEvent asyncContextEvent = state.getAsyncContextEvent();
        if (asyncContextEvent == null) {
            AgentBridge.getAgent().getLogger().log(Level.FINE, "AsyncContextEvent is null for request: {0}.", request);
            return;
        }
        AgentBridge.asyncApi.resumeAsync(asyncContextEvent.getAsyncContext());
    }

    public static void postHandleDispatch(ServletContextRequest servletContextRequest) {
        HttpServletRequest request = servletContextRequest.getServletApiRequest();
        if (request.isAsyncStarted()) {
            AgentBridge.asyncApi.suspendAsync(request.getAsyncContext());
        }
        final Throwable throwable = ServerHelper.getRequestError(request);
        if (throwable != null) {
            NewRelic.noticeError(throwable);
        }
        AgentBridge.getAgent().getTransaction().requestDestroyed();
    }

    public static Throwable getRequestError(ServletContextRequest servletContextReq) {
        if (servletContextReq == null) {
            return null;
        }
        return getRequestError(servletContextReq.getServletApiRequest());
    }

    public static Throwable getRequestError(HttpServletRequest request) {
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
