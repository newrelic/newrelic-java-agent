package jakarta.xml.ws.spi.http;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(type = MatchType.Interface, originalName = "jakarta.xml.ws.spi.http.HttpHandler")
public abstract class HttpHandler_Instrumentation {

    @Trace(dispatcher = true)
    public void handle(HttpExchange exchange) {

        if (!AgentBridge.getAgent().getTransaction().isWebRequestSet()) {
            ExchangeRequestResponse r = new ExchangeRequestResponse(exchange);
            NewRelic.setRequestAndResponse(r, r);
        }
        Weaver.callOriginal();
    }

}
