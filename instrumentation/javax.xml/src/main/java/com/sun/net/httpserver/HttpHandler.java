package com.sun.net.httpserver;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(type = MatchType.Interface)
public abstract class HttpHandler {

    @Trace(dispatcher = true)
    public void handle(com.sun.net.httpserver.HttpExchange exchange) {

        // we don't care about requests to fetch the wsdl
        if (!"wsdl".equals(exchange.getRequestURI().getQuery())
                && !AgentBridge.getAgent().getTransaction().isWebRequestSet()) {
            ExchangeRequestResponse r = new ExchangeRequestResponse(exchange);
            NewRelic.setRequestAndResponse(r, r);
        }
        Weaver.callOriginal();
    }
}
