package io.vertx.core.http.impl;

import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.vertx.VertxCoreUtil;

import io.vertx.core.MultiMap;

import java.net.UnknownHostException;

@Weave(originalName = "io.vertx.core.http.impl.HttpClientRequestBase")
public abstract class HttpClientRequestBase_Instrumentation {

    @NewField
    public Segment segment;

    public abstract MultiMap headers();

    @Trace(async = true)
    void handleResponse(HttpClientResponseImpl resp) {
        if (segment != null) {
            reportExternal(resp, segment);
            final Token token = segment.getTransaction().getToken();
            segment.end();
            token.linkAndExpire();
        }

        Weaver.callOriginal();
    }

    @Trace(async = true)
    public void handleException(Throwable t) {
        if (segment != null) {
            if (t instanceof UnknownHostException) {
                VertxCoreUtil.reportUnknownHost(segment);
            }
            final Token token = segment.getTransaction().getToken();
            segment.end();
            token.linkAndExpire();
        }
        Weaver.callOriginal();
    }



    private void reportExternal(HttpClientResponseImpl resp, Segment segment) {
        // Need to access these from here since fields are package private
        // request.getHost() != request.host
        final String host = resp.request().host;
        // request.getPort() != request.port
        final int port = resp.request().port;
        final String scheme = resp.request().ssl ? "https" : "http";
        VertxCoreUtil.processResponse(segment, resp, host, port, scheme);
    }
}