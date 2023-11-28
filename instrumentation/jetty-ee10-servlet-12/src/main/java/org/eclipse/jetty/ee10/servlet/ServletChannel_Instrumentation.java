package org.eclipse.jetty.ee10.servlet;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.jetty.ee10.servlet.ServerHelper;

@Weave(originalName = "org.eclipse.jetty.ee10.servlet.ServletChannel", type = MatchType.ExactClass)
public class ServletChannel_Instrumentation {
    private final ServletRequestState _state = Weaver.callOriginal();
    private final ServletChannel.Dispatchable _requestDispatchable = Weaver.callOriginal();

    private final ServletChannel.Dispatchable _asyncDispatchable = Weaver.callOriginal();

    private void dispatch(ServletChannel.Dispatchable dispatchable) {
        // Initialize dispatcher to be called
        if (dispatchable == _requestDispatchable) {
            ((RequestDispatchable_Instrumentation)_requestDispatchable).servletContextReq = getServletContextRequest();
        }
        if (dispatchable == _asyncDispatchable) {
            ((AsyncDispatchable_Instrumentation)_requestDispatchable)._state = _state;
            ((AsyncDispatchable_Instrumentation)_requestDispatchable).servletContextReq = getServletContextRequest();
        }

        // dispatchable.dispatch() is called next after some setup
        Weaver.callOriginal();
    }

    public void sendResponseAndComplete() {
        ServletContextRequest servletContextRequest = getServletContextRequest();
        Throwable throwable = ServerHelper.getRequestError(servletContextRequest);
        try {
            Weaver.callOriginal();
        } finally {
            if (throwable != null) {
                NewRelic.noticeError(throwable);
            }
        }
    }

    @Weave(type = MatchType.ExactClass, originalName = "org.eclipse.jetty.ee10.servlet.ServletChannel$RequestDispatchable")
    private static class RequestDispatchable_Instrumentation implements ServletChannel.Dispatchable {

        @NewField
        ServletContextRequest servletContextReq;

        public void dispatch() {
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
    }

    @Weave(type = MatchType.ExactClass, originalName = "org.eclipse.jetty.ee10.servlet.ServletChannel$AsyncDispatchable")
    private static class AsyncDispatchable_Instrumentation implements ServletChannel.Dispatchable {

        @NewField
        ServletContextRequest servletContextReq;
        @NewField
        ServletRequestState _state;

        public void dispatch() {
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
    }

    public ServletContextHandler getServletContextHandler() {
        return Weaver.callOriginal();
    }

    public ServletContextRequest getServletContextRequest()
    {
        return Weaver.callOriginal();
    }
}
