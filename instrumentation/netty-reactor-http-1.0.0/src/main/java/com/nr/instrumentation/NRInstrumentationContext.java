package com.nr.instrumentation;

import com.newrelic.api.agent.HttpParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Transaction;
import reactor.netty.http.client.HttpClientRequest;
import reactor.netty.http.client.HttpClientResponse;

import java.net.URI;
import java.util.logging.Level;

public final class NRInstrumentationContext {

    private static final String SEGMENT_NAME = "HttpClient";
    private static final String LIBRARY_NAME = "Reactor-Netty";

    private Token token;
    private Segment segment;

    private String method;
    private URI uri;

    public NRInstrumentationContext(Token token) {
        this.token = token;
    }

    public void startSegment(HttpClientRequest request) {
        method = (request != null) ? request.method().name() : "GET";
        uri = request == null ?  null : ReactorNettyUtil.safeParseURI(request.resourceUrl());
        if (uri == null) {
            uri = URI.create("http://unknown");
        }

        if (this.token == null) {
            return;
        }

        try {
            Transaction transaction = NewRelic.getAgent().getTransaction();
            if (transaction != null && !ReactorNettyUtil.isNoOpTransaction(transaction)) {
                ReactorNettyHeaders headers = new ReactorNettyHeaders(request);
                transaction.insertDistributedTraceHeaders(headers);

                this.segment = transaction.startSegment(SEGMENT_NAME);
            }
        } catch (Throwable t) {
        }
    }

    public void endSegment(HttpClientResponse response, Throwable error) {
        try {
            if (this.segment != null) {
                try {
                    if (response != null) {
                        HttpParameters params = buildHttpParameters(response);
                        this.segment.reportAsExternal(params);
                    }
                    this.segment.endAsync();
                } finally {
                    this.segment = null;
                }
            }

            if (error != null) {
                NewRelic.noticeError(error);
                NewRelic.getAgent().getLogger().log(Level.FINEST, error, "HTTP request error reported to New Relic");
            }

        } catch (Throwable t) {
            NewRelic.getAgent().getLogger().log(Level.FINEST, t, "Error ending segment");
        } finally {
            expireToken();
        }
    }

    // TODO why is the status code not showing up in the UI when it's 404?
    // TODO this method does get called, and with the correct data
    private HttpParameters buildHttpParameters(HttpClientResponse response) {
        ReactorNettyHeaders inboundHeaders = new ReactorNettyHeaders(response);

        return HttpParameters
            .library(LIBRARY_NAME)
            .uri(uri)
            .procedure(method)
            .inboundHeaders(inboundHeaders)
            .status(response.status().code(), response.status().reasonPhrase())
            .build();
    }

    private void expireToken() {
        if (this.token != null) {
            this.token.expire();
        }
        this.token = null;
    }

    public Segment getSegment() {
        return this.segment;
    }
}