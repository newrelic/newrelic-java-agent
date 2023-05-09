package com.nr.agent.instrumentation.httpclient50;

import com.newrelic.agent.bridge.AgentBridge;
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
            InstrumentationUtils.processResponse(uri, response);
        } catch (Throwable t) {
            AgentBridge.getAgent().getLogger().log(Level.FINER, t, "Unable to process response");
        }
        return originalResponseHandler.handleResponse(response);
    }

}
