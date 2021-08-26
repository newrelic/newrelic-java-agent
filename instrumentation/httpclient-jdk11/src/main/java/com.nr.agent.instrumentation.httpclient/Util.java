package com.nr.agent.instrumentation.httpclient;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.*;
import com.newrelic.api.agent.weaver.Weaver;

import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletionException;
import java.util.function.BiConsumer;
import java.util.logging.Level;

public class Util {

    private static final String LIBRARY = "JavaHttpClient";
    private static final URI UNKNOWN_HOST = URI.create("UnknownHost");
    private static final String PROCEDURE = "send";

    public static void addOutboundHeaders(HttpRequest.Builder thisBuilder) {
        NewRelic.getAgent().getTracedMethod().addOutboundRequestHeaders(new OutboundWrapper(thisBuilder));
    }

    public static Segment startSegment(URI uri) {
        if (uri != null) {
            String scheme = uri.getScheme().toLowerCase();
            Transaction txn = NewRelic.getAgent().getTransaction();
            if (("http".equals(scheme) || "https".equals(scheme)) && txn != null) {
                return txn.startSegment("JavaHttpClient");
            }
        }
        return null;
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
                    } else if (throwableIsConnectException(throwable)) {
                            segment.reportAsExternal(GenericParameters
                                    .library(LIBRARY)
                                    .uri(UNKNOWN_HOST)
                                    .procedure("failed")
                                    .build());
                    } else if (throwable != null) {
                        NewRelic.noticeError(throwable);
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

    public static <T> void processResponse(HttpResponse<T> response, Segment segment){
        if (response.uri() != null) {
            segment.reportAsExternal(HttpParameters
                    .library(LIBRARY)
                    .uri(response.uri())
                    .procedure(PROCEDURE)
                    .inboundHeaders(new InboundWrapper(response))
                    .build());
            segment.end();
        }
    }

    public static void handleConnectException(Exception e, HttpRequest request, Segment segment){
        if (request.uri() != null) {
            if (throwableIsConnectException(e)) {
                segment.reportAsExternal(GenericParameters
                        .library(LIBRARY)
                        .uri(UNKNOWN_HOST)
                        .procedure("failed")
                        .build());
                segment.end();
            } else {
                NewRelic.noticeError(e);
            }
        }
    }

    private static boolean throwableIsConnectException(Throwable throwable) {
        if (throwable instanceof ConnectException) {
            return true;
        } else return throwable instanceof CompletionException && throwable.getCause() instanceof ConnectException;
    }
}
