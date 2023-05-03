package com.nr.agent.instrumentation.httpclient50;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.Agent;
import com.newrelic.api.agent.HttpParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.TracedMethod;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;

import java.io.IOException;
import java.net.URI;
import java.util.logging.Level;

public class WrappedResponseHandler<T> implements HttpClientResponseHandler<T> {

    private final URI uri;
    private final HttpClientResponseHandler<T> originalResponseHandler;

    public WrappedResponseHandler(URI uri, HttpClientResponseHandler<T> originalResponseHandler) {
        this.uri = uri;
        this.originalResponseHandler = originalResponseHandler;
    }

    @Override
    public T handleResponse(ClassicHttpResponse response) throws HttpException, IOException {
        try {
            processResponse(uri, response);
        } catch (Throwable t) {
            AgentBridge.getAgent().getLogger().log(Level.FINER, t, "Unable to process response");
        }
        return originalResponseHandler.handleResponse(response);
    }

    private static void processResponse(URI requestURI, ClassicHttpResponse response) {
        InboundWrapper inboundCatWrapper = new InboundWrapper(response);
        Agent agent = NewRelic.getAgent();
        TracedMethod method = agent.getTracedMethod();
        NewRelic.getAgent().getLogger().log(Level.INFO, "JGB agent: " + agent + "; method: " + method); // TODO remove
        NewRelic.getAgent().getTracedMethod().reportAsExternal(HttpParameters
                .library(InstrumentationUtils.LIBRARY)
                .uri(requestURI)
                .procedure(InstrumentationUtils.PROCEDURE)
                .inboundHeaders(inboundCatWrapper)
                .status(response.getCode(), response.getReasonPhrase())
                .build());
    }
}
