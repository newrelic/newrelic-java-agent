/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.spring_webclient;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.api.agent.GenericParameters;
import com.newrelic.api.agent.HttpParameters;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.weaver.Weaver;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.UnknownHostException;
import java.util.function.Consumer;
import java.util.logging.Level;

public class Util {

    private static final String LIBRARY = "Spring-WebClient";
    private static final URI UNKNOWN_HOST = URI.create("UnknownHost");

    public static Segment startSegment() {
        Transaction txn = AgentBridge.getAgent().getTransaction(false);
        return txn == null ? null : txn.startSegment("WebClient.exchange");
    }

    public static ClientRequest addHeaders(ClientRequest request, Segment segment) {
        if (segment != null) {
            OutboundRequestWrapper outboundHeaders = new OutboundRequestWrapper(request);
            segment.addOutboundRequestHeaders(outboundHeaders);
            request = outboundHeaders.build();
        }
        return request;
    }

    public static Mono<ClientResponse> reportAsExternal(ClientRequest request, Mono<ClientResponse> response, Segment segment) {
        if (segment == null) {
            return response;
        }
        URI uri = request.url();
        return response
                .doOnSuccess(reportSucceeded(segment, uri))
                .doOnError(reportFailed(segment))
                .doOnCancel(reportCancelled(segment, uri));
    }

    private static Consumer<ClientResponse> reportSucceeded(Segment segment, final URI uri) {
        return new Consumer<ClientResponse>() {
            @Override
            public void accept(ClientResponse clientResponse) {
                try {
                    segment.reportAsExternal(HttpParameters
                            .library(LIBRARY)
                            .uri(uri)
                            .procedure("exchange")
                            .inboundHeaders(new InboundResponseWrapper(clientResponse))
                            .status(clientReponse.statusCode().value())
                            .build());
                    segment.end();
                } catch (Throwable e) {
                    reportInstrumentationError(e);
                }
            }
        };
    }

    private static Consumer<Throwable> reportFailed(Segment segment) {
        return new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) {
                try {
                    if (throwable instanceof UnknownHostException) {
                        segment.reportAsExternal(GenericParameters
                                .library(LIBRARY)
                                .uri(UNKNOWN_HOST)
                                .procedure("failed")
                                .build());
                    }
                    segment.end();
                } catch (Throwable e) {
                    reportInstrumentationError(e);
                }
            }
        };
    }

    private static Runnable reportCancelled(Segment segment, final URI uri) {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    segment.reportAsExternal(HttpParameters
                            .library(LIBRARY)
                            .uri(uri)
                            .procedure("exchange")
                            .noInboundHeaders()
                            .build());
                    segment.end();
                } catch (Throwable e) {
                    reportInstrumentationError(e);
                }
            }
        };
    }

    private static void reportInstrumentationError(Throwable e) {
        AgentBridge.getAgent()
                .getLogger()
                .log(Level.FINEST, e, "Caught exception in Spring-WebClient instrumentation: {0}");
        AgentBridge.instrumentation.noticeInstrumentationError(e, Weaver.getImplementationTitle());
    }
}
