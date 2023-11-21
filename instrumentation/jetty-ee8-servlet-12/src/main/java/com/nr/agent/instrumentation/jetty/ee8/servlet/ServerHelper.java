package com.nr.agent.instrumentation.jetty.ee8.servlet;

import com.newrelic.agent.bridge.AgentBridge;
import org.eclipse.jetty.ee8.nested.AsyncContextEvent;
import org.eclipse.jetty.ee8.nested.Request;

import javax.servlet.DispatcherType;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.logging.Level;

public class ServerHelper {

    public static void preHandle(Request baseRequest, HttpServletRequest request, HttpServletResponse response) {
        if (request.getDispatcherType() == DispatcherType.ASYNC) {
            AsyncContextEvent asyncContextEvent = baseRequest.getHttpChannelState().getAsyncContextEvent();
            if (asyncContextEvent == null) {
                AgentBridge.getAgent().getLogger().log(Level.FINE, "AsyncContextEvent is null for request: {0}.", request);
                return;
            }
            AgentBridge.asyncApi.resumeAsync(asyncContextEvent.getAsyncContext());
        } else {
            AgentBridge.getAgent().getTransaction(true).requestInitialized(new JettyRequest(request),
                    new JettyResponse(response));
        }
    }

    public static void postHandle(HttpServletRequest request) {
        if (request.isAsyncStarted()) {
            AgentBridge.asyncApi.suspendAsync(request.getAsyncContext());
        }
        AgentBridge.getAgent().getTransaction().requestDestroyed();
    }

}
