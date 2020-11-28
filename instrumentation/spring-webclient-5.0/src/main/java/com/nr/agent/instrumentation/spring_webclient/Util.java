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
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.UnknownHostException;
import java.util.function.BiConsumer;
import java.util.logging.Level;

public class Util {

    private static final String LIBRARY = "Spring-WebClient";
    private static final String SEGMENT_ATTRIBUTE = "newrelic-segment";
    private static final URI UNKNOWN_HOST = URI.create("UnknownHost");

    public static void startExternalSegmentIfNeeded(WebClient.RequestHeadersSpec<?> request) {
        Transaction txn = AgentBridge.getAgent().getTransaction(false);
        if (txn != null) {
            Segment segment = txn.startSegment("WebClient.exchange");
            segment.addOutboundRequestHeaders(new OutboundRequestWrapper(request));
            request.attribute(SEGMENT_ATTRIBUTE, segment);
        }
    }

    public static Mono<ClientResponse> reportAsExternal(ClientRequest request, Mono<ClientResponse> response) {
        Segment segment = (Segment) request.attribute(SEGMENT_ATTRIBUTE).orElse(null);
        if (segment == null) {
            return response;
        }
        URI uri = request.url();
        return response.doAfterSuccessOrError(reportAsExternal(segment, uri));
    }

    private static BiConsumer<? super ClientResponse, Throwable> reportAsExternal(Segment segment, URI uri) {
        return new BiConsumer<ClientResponse, Throwable>() {
            @Override
            public void accept(ClientResponse clientResponse, Throwable throwable) {
                try {
                    if (clientResponse != null) {
                        segment.reportAsExternal(HttpParameters
                                .library(LIBRARY)
                                .uri(uri)
                                .procedure("exchange")
                                .inboundHeaders(new InboundResponseWrapper(clientResponse))
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
                    segment.end();
                } catch (Throwable e) {
                    AgentBridge.getAgent()
                            .getLogger()
                            .log(Level.FINEST, e, "Caught exception in Spring-WebClient instrumentation: {0}");
                    AgentBridge.instrumentation.noticeInstrumentationError(e, Weaver.getImplementationTitle());
                }
            }
        };
    }
}
