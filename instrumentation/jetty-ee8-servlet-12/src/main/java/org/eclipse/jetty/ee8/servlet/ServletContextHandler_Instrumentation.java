package org.eclipse.jetty.ee8.servlet;

import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.jetty.ee8.servlet.JettyRequestListener;

import java.util.EventListener;

@Weave(originalName = "org.eclipse.jetty.ee8.servlet.ServletContextHandler")
public class ServletContextHandler_Instrumentation {

    protected void doStart() {
        addEventListener(new JettyRequestListener());
        Weaver.callOriginal();
    }

    public boolean addEventListener(EventListener listener) {
        return Weaver.callOriginal();
    }
}
