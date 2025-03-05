package io.undertow.server;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.TracedMethod;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import io.undertow.server.handlers.PathTemplateHandler;

import java.util.HashMap;
import java.util.logging.Level;

//@Weave(type = MatchType.Interface, originalName = "io.undertow.server.HttpHandler")
public abstract class HttpHandler_Instrumentation {
    @Trace(dispatcher=true)
    public void handleRequest(HttpServerExchange exchange) {
        NewRelic.getAgent().getTracedMethod().setMetricName("Undertow", "HttpHandler", getClass().getSimpleName(), "handleRequest");
        Weaver.callOriginal();
    }
}
