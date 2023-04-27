package com.nr.agent.instrumentation.httpclient50;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.GenericParameters;
import com.newrelic.api.agent.HttpParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.NewField;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpResponse;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.logging.Level;

public class WrappedFutureCallback<T> implements FutureCallback<T> {
    private static final String LIBRARY = "CommonsHttp";
    private static final String PROCEDURE = "execute";
    private static final URI UNKNOWN_HOST_URI = URI.create("http://UnknownHost/");

    HttpRequest request;
    FutureCallback origCallback;
    Token token;
    public WrappedFutureCallback (HttpRequest request, FutureCallback origCallback, Token token) {
        this.request = request;
        this.origCallback = origCallback;
        this.token = token;
    }
    @Override
    @Trace(async = true)
    public void completed(T response) {
        NewRelic.getAgent().getLogger().log(Level.INFO, "I done intercepted it: completed: "+response.getClass());
        NewRelic.getAgent().getLogger().log(Level.INFO, "Agent: "+NewRelic.getAgent());
        NewRelic.getAgent().getLogger().log(Level.INFO, "Transaction: "+NewRelic.getAgent().getTransaction());
        if (token != null) token.link();
        NewRelic.getAgent().getLogger().log(Level.INFO, "After token link");
        try {
            processResponse(request.getUri(), (SimpleHttpResponse)response);
            NewRelic.getAgent().getLogger().log(Level.INFO, "After processResponse");
        } catch (URISyntaxException e) {
            // TODO throw new IOException(e);
            // TODO can this happen now?  perhaps in one of the overload methods?
        }
        if (origCallback != null) origCallback.completed(response);
        NewRelic.getAgent().getLogger().log(Level.INFO, "After origCallback");
        if (token != null) token.expire();
        token = null;
        NewRelic.getAgent().getLogger().log(Level.INFO, "After expire");
    }

    @Override
    @Trace(async = true)
    public void failed(Exception ex) {
        NewRelic.getAgent().getLogger().log(Level.INFO, "I done intercepted it: failed");
        if (token != null) token.link();
        handleUnknownHost(ex);
        // TODO anything more to do?
        if (origCallback != null) origCallback.failed(ex);
        if (token != null) token.expire();
        token = null;
    }

    @Override
    @Trace(async = true)
    public void cancelled() {
        // TODO handle cancellation
        NewRelic.getAgent().getLogger().log(Level.INFO, "I done intercepted it: cancelled");
        if (token != null) token.link();
        if (origCallback != null) origCallback.cancelled();
        if (token != null) token.expire();
        token = null;
    }

    @Trace(async = true) // TODO on failed and cancelled too?
    private void processResponse(URI requestURI, HttpResponse response) {
        // TODO ***********************************************************
        // TODO ***********************************************************
        // TODO ***********************************************************
        // TODO ***********************************************************
        // TODO this is getting a NoOpTracedMethod sometimes
        // TODO this is what i'm trying to figure out currently
        // TODO definitely doing something wrong, or missing something to make sure the Tx is available
        // TODO ***********************************************************
        // TODO ***********************************************************
        // TODO ***********************************************************
        // TODO ***********************************************************
        if (token != null) token.link();
        NewRelic.getAgent().getLogger().log(Level.INFO, "Current Stack for thread "+
                Thread.currentThread().getName()+"-"+
                Thread.currentThread().getId()+"\n: "+
                stackString(Thread.currentThread().getStackTrace()));
        NewRelic.getAgent().getLogger().log(Level.INFO, "inside processResponse: "+requestURI+" | "+response);
        InboundWrapper inboundCatWrapper = new InboundWrapper(response);
        NewRelic.getAgent().getLogger().log(Level.INFO, "Agent Transaction: "+NewRelic.getAgent().getTransaction());
        NewRelic.getAgent().getLogger().log(Level.INFO, "Bridge Transaction: "+ AgentBridge.getAgent().getTransaction());
        NewRelic.getAgent().getLogger().log(Level.INFO, "Agent method: "+NewRelic.getAgent().getTracedMethod());
        NewRelic.getAgent().getLogger().log(Level.INFO, "Bridge method: "+AgentBridge.getAgent().getTracedMethod());
        if (NewRelic.getAgent().getTracedMethod().getClass().toString().contains("DefaultTracer")) {
            NewRelic.getAgent().getLogger().log(Level.INFO, "------------------------------------------------------------SUCCESS");
        }
        NewRelic.getAgent().getTracedMethod()
                .reportAsExternal(HttpParameters
                        .library(LIBRARY)
                        .uri(requestURI)
                        .procedure(PROCEDURE)
                        .inboundHeaders(inboundCatWrapper)
                        .status(response.getCode(), response.getReasonPhrase())
                        .build());
        if (token != null) token.expire();
        token = null;
    }
    private String stackString(StackTraceElement[] elems) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement elem : elems) {
            sb.append(elem);
            sb.append("\n");
        }
        return sb.toString();
    }
    // TODO move this to a Util class?
    private void handleUnknownHost(Exception e) {
        if (e instanceof UnknownHostException) {
            NewRelic.getAgent().getLogger().log(Level.INFO, "Found UnknownHostException");
            NewRelic.getAgent().getTracedMethod().reportAsExternal(GenericParameters
                    .library(LIBRARY)
                    .uri(UNKNOWN_HOST_URI)
                    .procedure(PROCEDURE)
                    .build());
        }
    }
}
