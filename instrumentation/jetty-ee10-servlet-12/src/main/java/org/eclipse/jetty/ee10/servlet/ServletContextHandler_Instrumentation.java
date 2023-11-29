package org.eclipse.jetty.ee10.servlet;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.jetty.ee10.servlet.JettyRequestListener;
import com.nr.agent.instrumentation.jetty.ee10.servlet.ServerHelper;
import jakarta.servlet.http.HttpServletRequest;
import org.eclipse.jetty.server.Request;

import java.util.EventListener;

@Weave(originalName = "org.eclipse.jetty.ee10.servlet.ServletContextHandler", type = MatchType.ExactClass)
public class ServletContextHandler_Instrumentation {

    protected void doStart() {
        addEventListener(new JettyRequestListener());
        Weaver.callOriginal();
    }

    public boolean addEventListener(EventListener listener) {
        return Weaver.callOriginal();
    }

    protected void requestDestroyed(Request baseRequest, HttpServletRequest request) {
        Throwable throwable = ServerHelper.getRequestError(baseRequest);
        try {
            Weaver.callOriginal();
        } finally {
            if (throwable != null) {
                NewRelic.noticeError(throwable);
            }
        }
    }
}
