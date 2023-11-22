package org.eclipse.jetty.ee10.servlet;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.jetty.ee10.servlet.ServerHelper;

@Weave(originalName = "org.eclipse.jetty.ee10.servlet.ServletChannel", type = MatchType.ExactClass)
public class ServletChannel_Instrumentation {
    private final ServletChannelState _state = Weaver.callOriginal();

    private void dispatch() {
        ServletContextRequest servletContextReq = getServletContextRequest();
        boolean isStarted = AgentBridge.getAgent().getTransaction().isStarted();
        boolean startTransaction = servletContextReq != null && servletContextReq.getServletApiRequest() != null && !isStarted;
        if (startTransaction) {
            ServerHelper.preHandleDispatch(servletContextReq);
        }

        try {
            Weaver.callOriginal();
        } finally {
            if (startTransaction) {
                ServerHelper.postHandleDispatch(servletContextReq);
            }
        }
    }

    public void dispatchAsync() {
        ServletContextRequest servletContextReq = getServletContextRequest();
        boolean isStarted = AgentBridge.getAgent().getTransaction().isStarted();
        boolean startTransaction = servletContextReq != null && servletContextReq.getServletApiRequest() != null
                && !isStarted && _state != null;
        if (startTransaction) {
            ServerHelper.preHandleDispatchAsync(servletContextReq, _state);
        }
        try {
            Weaver.callOriginal();
        } finally {
            if (startTransaction) {
                ServerHelper.postHandleDispatch(servletContextReq);
            }
        }
    }

    public void sendResponseAndComplete() {
        NewRelic.getAgent().getTransaction().addOutboundResponseHeaders();
        Weaver.callOriginal();
    }

    public ServletContextHandler getServletContextHandler() {
        return Weaver.callOriginal();
    }

    public ServletContextRequest getServletContextRequest()
    {
        return Weaver.callOriginal();
    }
}
