package com.nr.agent.instrumentation.jetty.ee9.servlet;

import com.newrelic.agent.bridge.AgentBridge;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.ee9.nested.AsyncContextEvent;
import org.eclipse.jetty.ee9.nested.Request;

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
