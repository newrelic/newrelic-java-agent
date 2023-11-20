package org.eclipse.jetty.ee10.servlet;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.jetty.ee10.servlet.AsyncListenerFactory;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.internal.HttpChannelState;

import java.util.logging.Level;

@Weave(type = MatchType.BaseClass, originalName = "org.eclipse.jetty.ee10.servlet.ServletApiRequest")
public abstract class ServletApiRequest_Instrumentation implements HttpServletRequest {
    @Override
    public AsyncContext startAsync(ServletRequest request, ServletResponse response) {
        AsyncContext asyncContext = Weaver.callOriginal();

        asyncContext.addListener(AsyncListenerFactory.getAsyncListener());
        AgentBridge.getAgent().getLogger().log(Level.FINER, "Added async listener");

        return asyncContext;
    }

    @Override
    public AsyncContext startAsync() {

        AsyncContext asyncContext = Weaver.callOriginal();

        asyncContext.addListener(AsyncListenerFactory.getAsyncListener());
        AgentBridge.getAgent().getLogger().log(Level.FINER, "Added async listener");

        return asyncContext;
    }

    public abstract HttpChannelState getHttpChannelState();

    public abstract Response getResponse();
}
