package com.nr.agent.instrumentation.httpclient;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.GenericParameters;
import com.newrelic.api.agent.HttpParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.weaver.Weaver;

import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.function.BiConsumer;
import java.util.logging.Level;

public class Util {

    private static final String LIBRARY = "JavaHttpClient";
    private static final URI UNKNOWN_HOST = URI.create("UnknownHost");
    private static final String PROCEDURE = "send";

    public static void addOutboundHeaders(HttpRequest.Builder thisBuilder) {
        NewRelic.getAgent().getTracedMethod().addOutboundRequestHeaders(new OutboundWrapper(thisBuilder));
    }

    public static <T> BiConsumer<? super HttpResponse<T>, ? super Throwable> reportAsExternal(URI uri, Segment segment) {
        return (BiConsumer<HttpResponse<T>, Throwable>) (httpResponse, throwable) -> {
            try {
                if (segment != null && uri != null) {
                    if (httpResponse != null) {
                        segment.reportAsExternal(HttpParameters
                                .library(LIBRARY)
                                .uri(uri)
                                .procedure(PROCEDURE)
                                .inboundHeaders(new InboundWrapper(httpResponse))
                                .build());
                    } else {
                        if (throwable instanceof UnknownHostException) {
                            segment.reportAsExternal(GenericParameters
                                    .library(LIBRARY)
                                    .uri(UNKNOWN_HOST)
                                    .procedure("failed")
                                    .build());
                        }
                    }
                }
                if (segment != null) {
                    segment.end();
                }
            } catch (Throwable e) {
                NewRelic.getAgent().getLogger()
                        .log(Level.FINEST, e, "Caught exception in Java Http Client instrumentation: {0}");
                AgentBridge.instrumentation.noticeInstrumentationError(e, Weaver.getImplementationTitle());
            }
        };
    }
}
